package nccloud.api.uapbd.payablebill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.framework.common.NCLocator;
import nc.itf.org.IBasicOrgUnitQryService;
import nc.vo.arap.basebill.BaseAggVO;
import nc.vo.arap.basebill.BaseBillVO;
import nc.vo.arap.openapi.ArapBillOpenApiVO;
import nc.vo.arap.openapi.ArapItemOpenApiVO;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nccloud.itf.uapbd.payablebill.IPayableBillInsertService;
import nccloud.pubitf.arap.openapi.IPayableBillOpenService;

public class PayableBillInsertServiceImpl implements IPayableBillInsertService {

    /**
     * 应付单单据类型编码，与产品标准资源 PayablebillRestResource.insertBill 的传参一致；
     * 编码转 pk、汇率方向计算、审计字段、单据状态均由标准保存链（PayableBillOpenServiceImpl.saveBill）处理。
     */
    private static final String BILLTYPE_F1 = "F1";

    @Override
    public Map<String, Object> insertPayableBill(ArapBillOpenApiVO billVO) throws BusinessException {
        if (billVO == null) {
            throw new BusinessException("请求报文不能为空！");
        }
        validateHead(billVO);
        validateItems(billVO);
        processBeforeSave(billVO);
        //翻译字段
        BaseAggVO aggVO = transferBill(billVO);
        //填充系统字段
        fillData(aggVO);
        //保存
        IPayableBillOpenService openService = NCLocator.getInstance().lookup(IPayableBillOpenService.class);
        Map<String, Object> saveResult = openService.saveBill(aggVO);
        // saveBill 返回 Map 的 key：pk_org/billno/billmaker/pk_bill（javap 验证自 BillOpenServiceImpl.saveBill）
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("bill_no", saveResult.get("billno"));
        map.put("pk_bill", saveResult.get("pk_bill"));
        return map;
    }

    /**
     * 保存前自定义逻辑扩展点：补默认值、业务校验、来源单据联动等在此处理。
     */
    protected void processBeforeSave(ArapBillOpenApiVO billVO) {
    }

    /**
     * 翻译字段：表头/表体的编码→pk（组织、币种、客商、部门、人员、税码、交易类型）
     * 均由标准 openapi 链 toAggVO("F1") 内部完成，与标准资源 PayablebillRestResource.insertBill 同链。
     */
    protected BaseAggVO transferBill(ArapBillOpenApiVO billVO) throws BusinessException {
        return billVO.toAggVO(BILLTYPE_F1);
    }

    /**
     * 填充系统字段：制单人/创建时间/单据号/ts/dr 由标准保存链生成，不在此填；
     * 这里只补链外的字段——pk_group 按财务组织带出。
     */
    protected void fillData(BaseAggVO aggVO) throws BusinessException {
        BaseBillVO head = aggVO.getHeadVO();
        if (head.getPk_group() == null || head.getPk_group().trim().length() == 0) {
            OrgVO org = NCLocator.getInstance().lookup(IBasicOrgUnitQryService.class).getOrg(head.getPk_org());
            head.setPk_group(org == null ? null : org.getPk_group());
        }
    }

    private void validateHead(ArapBillOpenApiVO vo) throws BusinessException {
        requireText(vo.getPk_org(), "pk_org(财务组织编码)");
        requireText(vo.getPk_tradetype(), "pk_tradetype(交易类型编码)");
        requireText(vo.getBilldate(), "billdate(制单日期)");
        requireText(vo.getObjtype(), "objtype(往来对象)");
        requireText(vo.getPk_currtype(), "pk_currtype(币种编码)");
        requireText(vo.getMoney(), "money(原币金额)");
        requireText(vo.getRate(), "rate(组织本币汇率)");
        requireText(vo.getLocal_money(), "local_money(组织本币金额)");
        requireText(vo.getGrouprate(), "grouprate(集团本币汇率)");
        requireText(vo.getGrouplocal(), "grouplocal(集团本币金额)");
        requireText(vo.getGlobalrate(), "globalrate(全局本币汇率)");
        requireText(vo.getGloballocal(), "globallocal(全局本币金额)");
        requirePartyByObjType(vo.getObjtype(), vo.getSupplier(), vo.getPk_dept(), vo.getPk_psndoc(), "");
    }

    private void validateItems(ArapBillOpenApiVO vo) throws BusinessException {
        List<ArapItemOpenApiVO> items = vo.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        int row = 0;
        for (ArapItemOpenApiVO item : items) {
            row++;
            if (item == null) {
                throw new BusinessException("items第" + row + "行数据为空！");
            }
            String prefix = "items第" + row + "行 ";
            requireObjType(item.getObjtype(), prefix + "objtype(往来对象)");
            requireText(item.getPk_currtype(), prefix + "pk_currtype(币种编码)");
            requireText(item.getMoney_cr(), prefix + "money_cr(借方原币金额)");
            requireText(item.getRate(), prefix + "rate(组织本币汇率)");
            requireText(item.getLocal_money_cr(), prefix + "local_money_cr(组织本币金额)");
            requireText(item.getGrouprate(), prefix + "grouprate(集团本币汇率)");
            requireText(item.getGroupcrebit(), prefix + "groupcrebit(集团本币金额)");
            requireText(item.getGlobalrate(), prefix + "globalrate(全局本币汇率)");
            requireText(item.getGlobalcrebit(), prefix + "globalcrebit(全局本币金额)");
            requireText(item.getQuantity_cr(), prefix + "quantity_cr(贷方数量)");
            requireText(item.getPrice(), prefix + "price(单价)");
            requireText(item.getTaxcodeid(), prefix + "taxcodeid(税码编码)");
            String objtype = item.getObjtype() == null ? null : String.valueOf(item.getObjtype().intValue());
            requirePartyByObjType(objtype, item.getSupplier(), item.getPk_dept(), item.getPk_psndoc(), prefix);
        }
    }



    private void requireText(String value, String field) throws BusinessException {
        if (value == null || value.trim().length() == 0) {
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }

    private void requireObjType(Integer value, String field) throws BusinessException {
        if (value == null) {
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }

    /**
     * 往来对象联动校验（NCC 规则）：1-供应商 2-部门 3-业务员，对应档案编码必填。
     */
    private void requirePartyByObjType(String objtype, String supplier, String pkDept, String pkPsndoc,
            String prefix) throws BusinessException {
        if (objtype == null) {
            return;
        }
        if ("1".equals(objtype)) {
            requireText(supplier, prefix + "supplier(供应商编码)");
        } else if ("2".equals(objtype)) {
            requireText(pkDept, prefix + "pk_dept(部门编码)");
        } else if ("3".equals(objtype)) {
            requireText(pkPsndoc, prefix + "pk_psndoc(业务员编码)");
        }
    }
}
