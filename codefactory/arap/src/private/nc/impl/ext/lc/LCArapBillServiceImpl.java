package nc.impl.ext.lc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.itf.ext.lc.ILCArapBillService;
import nc.itf.uap.pf.IplatFormEntry;
import nc.vo.arap.payable.AggPayableBillVO;
import nc.vo.arap.payable.PayableBillItemVO;
import nc.vo.arap.payable.PayableBillVO;
import nc.vo.arap.pay.AggPayBillVO;          // 付款单聚合VO，包名以环境实际为准
import nc.vo.arap.pay.PayBillItemVO;
import nc.vo.arap.pay.PayBillVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pflow.PfUserObject;

public class LCArapBillServiceImpl implements ILCArapBillService {

    private BaseDAO dao;
    private IplatFormEntry pfService;

    private BaseDAO getDao() {
        if (dao == null) dao = new BaseDAO();
        return dao;
    }

    private IplatFormEntry getPfService() {
        if (pfService == null)
            pfService = NCLocator.getInstance().lookup(IplatFormEntry.class);
        return pfService;
    }

    /** 应付单新增（F1） */
    @Override
    public JSONObject savePayableBill(JSONObject json) throws BusinessException {
        // 1. 组装聚合VO
        AggPayableBillVO agg = buildPayableBill(json);

        // 2. 走标准 SAVE 动作脚本（校验、编码规则、表体表头联动全部生效）
        Object ret = getPfService().processAction("SAVE", "F1", null, agg,
                new PfUserObject[] { new PfUserObject() }, null);
        if (!(ret instanceof AggPayableBillVO[]))
            throw new BusinessException("应付单保存返回结果异常：" + ret);
        AggPayableBillVO[] saved = (AggPayableBillVO[]) ret;

        // 3. 可选：直接提交（驱动工作流）。不需要提交可删掉
        getPfService().processAction("START", "F1", null, saved[0],
                new PfUserObject[] { new PfUserObject() }, null);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("pk", saved[0].getPrimaryKey());
        result.put("billno", saved[0].getHeadVO().getBillno());
        return result;
    }

    /** 付款单新增（F3） */
    @Override
    public JSONObject savePayBill(JSONObject json) throws BusinessException {
        AggPayBillVO agg = buildPayBill(json);

        Object ret = getPfService().processAction("SAVE", "F3", null, agg,
                new PfUserObject[] { new PfUserObject() }, null);
        if (!(ret instanceof AggPayBillVO[]))
            throw new BusinessException("付款单保存返回结果异常：" + ret);
        AggPayBillVO[] saved = (AggPayBillVO[]) ret;

        getPfService().processAction("START", "F3", null, saved[0],
                new PfUserObject[] { new PfUserObject() }, null);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("pk", saved[0].getPrimaryKey());
        result.put("billno", saved[0].getHeadVO().getBillno());
        return result;
    }

    // ==================== 应付单组装（对齐项目 GenerateSalaryVO 模式） ====================

    private AggPayableBillVO buildPayableBill(JSONObject json) throws BusinessException {
        String orgCode = require(json.getString("orgCode"), "组织编码不能为空！");
        String[] org = queryOrg(orgCode);                       // [pk_org, pk_group， pk_currtype]
        UFDate billdate =
                json.getString("billdate") == null ? new UFDate() : new UFDate(json.getString("billdate"));
        String supplier = queryCustomerPk(json.getString("supplierCode"), "供应商编码不能为空！");

        PayableBillVO head = new PayableBillVO();
        head.setPk_group(org[1]);
        head.setPk_org(org[0]);
        head.setPk_org_v(org[0]);                               // 未启用组织多版本时，版本pk=组织pk
        head.setPk_fiorg(org[0]);                               // 财务组织
        head.setPk_fiorg_v(org[0]);
        head.setSett_org(org[0]);                               // 结算财务组织
        head.setSett_org_v(org[0]);
        head.setSupplier(supplier);
        head.setObjtype(2);                                     // 对象类型：2=供应商（按环境档案核对）
        head.setBilldate(billdate);
        head.setBusidate(billdate);
        head.setBillyear(billdate.getYear() + "");
        head.setBillperiod(billdate.getStrMonth());
        head.setBillstatus(-1);                                 // 自由态
        head.setPk_currtype(queryPkByCode("bd_currtype", "pk_currtype",
                json.getString("pk_currtype") == null ? "CNY" : json.getString("pk_currtype")));
        head.setRate(UFDouble.ONE_DBL);                         // 本币汇率，外币时需查 gl 汇率
        head.setGrouprate(UFDouble.ONE_DBL);
        head.setGlobalrate(UFDouble.ONE_DBL);
        head.setPk_tradetype(json.getString("tradetype"));      // 交易类型编码，如 F1-Cxx-01
        head.setPk_tradetypeid(queryPkByCode("bd_billtype", "pk_billtypeid",
                json.getString("tradetype") == null ? "F1" : json.getString("tradetype")));
        head.setPk_billtype(queryPkByCode("bd_billtype", "pk_billtypeid", "F1"));
        head.setCreationtime(new UFDateTime());
        // billno 传空 → 按编码规则自动取号；外部单号可用 out_topbillno 回写防重
        if (json.getString("billno") != null) head.setBillno(json.getString("billno"));
        if (json.getString("outBillNo") != null) head.setOut_topbillno(json.getString("outBillNo"));
        head.setScomment(json.getString("memo"));

        // 表体
        JSONArray details = json.getJSONArray("details");
        if (details == null || details.isEmpty())
            throw new BusinessException("表体明细不能为空！");
        PayableBillItemVO[] items = new PayableBillItemVO[details.size()];
        for (int i = 0; i < details.size(); i++) {
            JSONObject d = details.getJSONObject(i);
            PayableBillItemVO item = new PayableBillItemVO();
            item.setPk_group(org[1]);
            item.setPk_org(org[0]);
            item.setBilldate(billdate);
            UFDouble money = new UFDouble(d.getString("money"));
            item.setMoney_cr(money);                            // 原币金额
            item.setLocal_money_cr(money);                      // 本币金额
            item.setLocal_tax_cr(UFDouble.ZERO_DBL);            // 税额
            item.setNotax_cr(money);
            item.setLocal_notax_cr(money);
            item.setMoney_bal(money);                           // 余额
            item.setLocal_money_bal(money);
            item.setOccupationmny(money);
            item.setSupplier(supplier);
            if (d.getString("deptCode") != null)
                item.setPk_deptid(queryPkByCode("org_adminorg", "pk_adminorg", d.getString("deptCode")));
            item.setContractno(d.getString("contractno"));
            items[i] = item;
        }

        AggPayableBillVO agg = new AggPayableBillVO();
        agg.setParent(head);
        agg.setChildrenVO(items);
        // 表体 billno / 主键由 SAVE 动作回填，无需手动设置
        return agg;
    }

    // ==================== 付款单组装 ====================

    private AggPayBillVO buildPayBill(JSONObject json) throws BusinessException {
        String orgCode = require(json.getString("orgCode"), "组织编码不能为空！");
        String[] org = queryOrg(orgCode);
        UFDate billdate =
                json.getString("billdate") == null ? new UFDate() : new UFDate(json.getString("billdate"));
        String supplier = queryCustomerPk(json.getString("supplierCode"), "供应商编码不能为空！");

        PayBillVO head = new PayBillVO();
        head.setPk_group(org[1]);
        head.setPk_org(org[0]);
        head.setPk_org_v(org[0]);
        head.setPk_fiorg(org[0]);
        head.setPk_fiorg_v(org[0]);
        head.setSupplier(supplier);
        head.setBilldate(billdate);
        head.setBusidate(billdate);
        head.setBillyear(billdate.getYear() + "");
        head.setBillperiod(billdate.getStrMonth());
        head.setBillstatus(-1);
        head.setPk_currtype(queryPkByCode("bd_currtype", "pk_currtype",
                json.getString("currencyCode") == null ? "CNY" : json.getString("currencyCode")));
        head.setRate(UFDouble.ONE_DBL);
        head.setGrouprate(UFDouble.ONE_DBL);
        head.setGlobalrate(UFDouble.ONE_DBL);
        head.setPk_tradetype(json.getString("tradetype"));      // 付款类交易类型编码
        head.setPk_tradetypeid(queryPkByCode("bd_billtype", "pk_billtypeid",
                json.getString("tradetype") == null ? "F3" : json.getString("tradetype")));
        head.setPk_billtype(queryPkByCode("bd_billtype", "pk_billtypeid", "F3"));
        head.setCreationtime(new UFDateTime());
        if (json.getString("billno") != null) head.setBillno(json.getString("billno"));
        // 银行账户：本方账户必输（对照 ItfVOTranOrder 校验 paymentAccount）
        head.setPayaccount(queryPkByCode("bd_bankaccsub", "pk_bankaccsub",
                require(json.getString("payAccountCode"), "付款银行账户编码不能为空！")));

        JSONArray details = json.getJSONArray("details");
        if (details == null || details.isEmpty())
            throw new BusinessException("表体明细不能为空！");
        PayBillItemVO[] items = new PayBillItemVO[details.size()];
        for (int i = 0; i < details.size(); i++) {
            JSONObject d = details.getJSONObject(i);
            PayBillItemVO item = new PayBillItemVO();
            item.setPk_group(org[1]);
            item.setPk_org(org[0]);
            item.setBilldate(billdate);
            UFDouble money = new UFDouble(d.getString("money"));
            item.setMoney(money);
            item.setLocal_money(money);
            item.setMoney_bal(money);
            item.setLocal_money_bal(money);
            item.setSupplier(supplier);
            if (d.getString("recAccountCode") != null)
                item.setRecaccount(queryPkByCode("bd_bankaccsub", "pk_bankaccsub", d.getString("recAccountCode")));
            item.setComment(d.getString("memo"));
            items[i] = item;
        }

        AggPayBillVO agg = new AggPayBillVO();
        agg.setParent(head);
        agg.setChildrenVO(items);
        return agg;
    }

    // ==================== 公共查询 ====================

    private String[] queryOrg(String code) throws BusinessException {
        try {
            Object[] row = (Object[]) getDao().executeQuery(
                    "select pk_org, pk_group, pk_currtype from org_orgs where code='" + code
                            + "' and dr=0 and ISBUSINESSUNIT='Y'",
                    rs -> rs.next() ? new Object[]{rs.getString(1), rs.getString(2),rs.getString(3)} : null);
            if (row == null) throw new BusinessException("根据组织编码未找到业务组织：" + code);
            return new String[]{(String) row[0], (String) row[1], (String) row[2]};
        } catch (DAOException e) {
            throw new BusinessException("查询组织失败：" + e.getMessage());
        }
    }

    private String queryCustomerPk(String code, String errMsg) throws BusinessException {
        if (code == null) throw new BusinessException(errMsg);
        return queryPkByCode("bd_customer", "pk_customer", code);
    }

    private String queryPkByCode(String table, String pkCol, String code) throws BusinessException {
        try {
            Object pk = getDao().executeQuery(
                    "select " + pkCol + " from " + table + " where code='" + code
                            + "' and nvl(dr,0)=0 and rownum=1",
                    rs -> rs.next() ? rs.getString(1) : null);
            if (pk == null) throw new BusinessException("根据编码未找到档案：" + table + "/" + code);
            return pk.toString();
        } catch (DAOException e) {
            throw new BusinessException("查询档案失败：" + e.getMessage());
        }
    }

    private String require(String v, String msg) throws BusinessException {
        if (v == null || v.trim().isEmpty()) throw new BusinessException(msg);
        return v.trim();
    }
}