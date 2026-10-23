package nccloud.api.cmp.pub.add;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONString;

import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.bs.trade.business.HYPubBO;
import nc.itf.cmp.fieldmap.IBillFieldGet;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.md.data.access.NCObject;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.IMDPersistenceQueryService;
import nc.pubitf.cmp.paybill.ICmpPayBillPubService;
import nc.vo.bd.bankaccount.BankAccSubVO;
import nc.vo.cmp.bill.BillAggVO;
import nc.vo.cmp.bill.BillDetailVO;
import nc.vo.cmp.bill.BillVO;
import nc.vo.cmp.util.StringUtils;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.billtype.BilltypeVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.tmpub.util.StringUtil;
import nc.vo.tmpub.util.TMCurrencyUtil;
import nc.vo.uap.rbac.constant.INCSystemUserConst;
import nccloud.api.rest.utils.ResultMessageUtil;
import nccloud.api.tmpub.common.TMpubCommonRefTranslate;

@Path("cmp/paybillAdd")
public class PayBillAddResource extends AbstractCmpBillAddResource{

    @POST
    @Produces({ "application/json" })
    @Consumes({ "application/json" })
    @Path("addBill")
    @Override
    public JSONString addBill(Map<String, Object> varMap) {
        return super.addBill(varMap);
    }

    @Override
    protected AggregatedValueObject initAggVO() {
        return new BillAggVO();
    }

    @Override
    protected CircularlyAccessibleValueObject initParentVO() {
        return new BillVO();
    }

    @Override
    protected CircularlyAccessibleValueObject[] initChildVO(int size) {
        BillDetailVO[] billDetailVOs = new BillDetailVO[size];
        for(int i=0; i<size; i++) {
            billDetailVOs[i] = new BillDetailVO();
        }
        return billDetailVOs;
    }

    @Override
    protected String[] getHeadNotNullFields() {
        return new String[]{
                "pk_org","bill_date"
        };
    }

    @Override
    protected String[] getBodyNotNullFields() {
        return new String[]{
                "pk_org",
                "pk_currtype",
                "pay_primal"
        };
    }

    @Override
    protected void dataNotNullValidate(AggregatedValueObject aggvo) throws BusinessException {
        super.dataNotNullValidate(aggvo);
        BillDetailVO[] childVOs = (BillDetailVO[]) aggvo.getChildrenVO();
        for(BillDetailVO childVO : childVOs) {
            int num=0;
            if(StringUtil.isNotNull(childVO.getPk_oppaccount())) {
                num++;
            }
            if(StringUtil.isNotNull(childVO.getMon_account())) {
                num++;
            }
            if(StringUtil.isNotNull(childVO.getNote_no())) {
                num++;
            }
            if(num == 0) {
                throw new BusinessException("表体本方银行账户、现金账户、票据号（商业汇票号）不能同时为空");
            } else if(num>1) {
                throw new BusinessException("表体本方银行账户、现金账户、票据号（商业汇票号）不能同时有值");
            }

            if(childVO.getObjecttype() != null) {
                if(childVO.getObjecttype() == 0 && StringUtil.isNull(childVO.getPk_customer())) {
                    throw new BusinessException("交易对象类型为客户时，客户不能为空");
                } else if(childVO.getObjecttype() == 1 && StringUtil.isNull(childVO.getPk_supplier())){
                    throw new BusinessException("交易对象类型为供应商时，供应商不能为空");
                } else if(childVO.getObjecttype() == 2 && StringUtil.isNull(childVO.getPk_dept())){
                    throw new BusinessException("交易对象类型为部门时，部门不能为空");
                } else if(childVO.getObjecttype() == 3 && StringUtil.isNull(childVO.getPk_busiman())){
                    throw new BusinessException("交易对象类型为人员时，业务员不能为空");
                }
            }
        }

    }

    @Override
    protected AggregatedValueObject fillData(AggregatedValueObject aggvo) throws BusinessException {
        BillVO parentVO = (BillVO) aggvo.getParentVO();
        BillDetailVO[] childVOs = (BillDetailVO[]) aggvo.getChildrenVO();

        //补录单据大类信息(值为FJ)
        parentVO.setBillclass(IBillFieldGet.FJ);
        parentVO.setBill_type("F5");
        parentVO.setSource_flag("2");
        parentVO.setBill_status(-10);


        //补录创建人和制单人(默认为系统用户)
        if(StringUtil.isNull(parentVO.getCreator())) {
            parentVO.setCreator(INCSystemUserConst.NC_USER_PK);
        }
        if(StringUtil.isNull(parentVO.getBillmaker())) {
            parentVO.setBillmaker(parentVO.getCreator());
            parentVO.setModifier(parentVO.getBillmaker());
        }
        parentVO.setCreator(parentVO.getBillmaker());

        parentVO.setModifier(parentVO.getBillmaker());//

        parentVO.setCreationtime(new UFDateTime());

        parentVO.setModifiedtime(new UFDateTime());//
        //补录表体信息(跟随表头)
        int i=0;
        for(BillDetailVO childVO : childVOs) {
            childVO.setBillclass(parentVO.getBillclass());
            childVO.setCreator(parentVO.getCreator());
            childVO.setPk_group(parentVO.getPk_group());
            childVO.setBill_date(parentVO.getBill_date());
            childVO.setBill_type("F5");
            childVO.setDirection(-1);
            childVO.setCreationtime(parentVO.getCreationtime());
            childVO.setCreator(parentVO.getCreator());
            childVO.setTeade_type(parentVO.getTrade_type());
            childVO.setPk_tradetypeid(parentVO.getPk_tradetypeid());
            parentVO.setDef1(childVO.getDef1());
            childVO.setModifier(parentVO.getModifier());
            childVO.setModifiedtime(parentVO.getModifiedtime());
            childVO.setBilldetail_no(i);
            i++;
        }

        //补录全局、集团、组织下的本币金额和汇率等信息
        aggvo = fillMoneyAndRate(aggvo);

        return aggvo;
    }

    /**
     * 补录全局、集团、组织下的本币金额和汇率等信息
     * @param aggvo
     * @return
     * @throws BusinessException
     */
    private AggregatedValueObject fillMoneyAndRate(AggregatedValueObject aggvo) throws BusinessException {
        BillDetailVO[] childVOs = (BillDetailVO[]) aggvo.getChildrenVO();
        //补录表体信息
        for (BillDetailVO childVO : childVOs) {
            String pk_group_b = childVO.getPk_group();
            String pk_org_b = childVO.getPk_org();
            String pk_currtype_b = childVO.getPk_currtype();
            UFDate bill_date_b = childVO.getBill_date();
            // 设置汇率
            if (childVO.getGlobal_rate() == null) {
                childVO.setGlobal_rate(TMCurrencyUtil.getGlobalCurrRate(pk_org_b, pk_currtype_b, bill_date_b));
            }
            if (childVO.getGroup_rate() == null) {
                childVO.setGroup_rate(
                        TMCurrencyUtil.getGroupCurrRate(pk_group_b, pk_org_b, pk_currtype_b, bill_date_b));
            }
            if (childVO.getLocal_rate() == null) {
                childVO.setLocal_rate(TMCurrencyUtil.getOrgCurrRate(pk_org_b, pk_currtype_b, bill_date_b));
            }
            // 设置本币金额
            if (childVO.getGlobal_local_pay() == null) {
                childVO.setGlobal_local_pay(TMCurrencyUtil.getGlobalLocalMoney(pk_org_b, pk_currtype_b,
                        childVO.getPay_primal(), childVO.getGlobal_rate(), childVO.getLocal_rate(), bill_date_b));
            }
            if (childVO.getGroup_local_pay() == null) {
                childVO.setGroup_local_pay(TMCurrencyUtil.getGroupLocalMoney(pk_group_b, pk_org_b, pk_currtype_b,
                        childVO.getPay_primal(), childVO.getGlobal_rate(), childVO.getLocal_rate(), bill_date_b));
            }
            if (childVO.getPay_local() == null) {
                childVO.setPay_local(TMCurrencyUtil.getOrgLocalMoney(pk_org_b, pk_currtype_b,
                        childVO.getPay_primal(), childVO.getLocal_rate(), bill_date_b));
            }
        }

        return aggvo;
    }

    @Override
    protected JSONString addBill(AggregatedValueObject aggvo) throws BusinessException {
        BillAggVO[] aggVOs = {(BillAggVO) aggvo};
        BillAggVO[] newAggVOs = NCLocator.getInstance().lookup(ICmpPayBillPubService.class).save(aggVOs);
        String bill_no = ((BillVO)newAggVOs[0].getParentVO()).getBill_no();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("bill_no", bill_no);
        return ResultMessageUtil.toJSON(map);
    }

    @Override
    public AggregatedValueObject getAggVO(Map<String, Object> varMap) throws BusinessException, ClassCastException {
        if (varMap.isEmpty()) {
            throw new BusinessException("传入参数不正确");
        }
        BillAggVO aggvo = (BillAggVO)initAggVO();
        return  converToBillVO(varMap, aggvo);
    }

    private AggregatedValueObject converToBillVO(Map<String, Object> varMap, AggregatedValueObject aggVO)
            throws BusinessException{
        // 组装表头数据
        Map<String, Object> headMap = (Map<String, Object>) varMap.get("head");
        // 翻译传入的数据
        TMpubCommonRefTranslate.translateExToNCAdd(headMap);
        BillVO headvo = (BillVO) getVO(headMap,new BillVO());
        // 组织版本
        headvo.setPk_org_v(TMpubCommonRefTranslate.getOrgUnit_V((String) headvo.getAttributeValue("pk_org")));
        //付款交易类型
        getPk_tradetypeid((BillVO)headvo);
        //制单日期
        if(StringUtil.isNull(headvo.getBillmaker_date())) {
            headvo.setBillmaker_date(new UFDate());
        }

        aggVO.setParentVO(headvo);

        // 组装表体数据
        List<Map<String, Object>> bodyList = (List<Map<String, Object>>) varMap.get("body");
        BillDetailVO[] bodysVO = new BillDetailVO[bodyList.size()];
        for (int i = 0; i < bodyList.size(); i++) {
            Map<String, Object> bodyMap = bodyList.get(i);
            // 翻译传入的数据
            TMpubCommonRefTranslate.translateExToNCAdd(bodyMap);
            bodysVO[i] = (BillDetailVO)getVO(bodyMap,new BillDetailVO());
            // 组织
            bodysVO[i].setPk_org((String) headvo.getAttributeValue("pk_org"));
            bodysVO[i].setPk_org_v((String) headvo.getAttributeValue("pk_org_v"));
            // 付款银行账户
            bodysVO[i].setPk_oppaccount(
                    TMpubCommonRefTranslate.translatebankacc((String) headvo.getAttributeValue("pk_org"),
                            bodysVO[i].getPk_oppaccount(), (String) bodysVO[i].getAttributeValue("pk_currtype")));
            //现金账户
            if(StringUtil.isNotNull(bodysVO[i].getMon_account())) {
                bodysVO[i].setMon_account(
                        TMpubCommonRefTranslate.translatecashkacc((String) headvo.getAttributeValue("pk_org"),
                                bodysVO[i].getMon_account(), (String) bodysVO[i].getAttributeValue("pk_currtype")));
            }else {
                bodysVO[i].setMon_account(null);
            }
            //供应商
            bodysVO[i].setPk_supplier(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_supplier", bodysVO[i].getPk_supplier()));
            //客户
            bodysVO[i].setPk_customer(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_customer", bodysVO[i].getPk_customer()));
            //人员
            bodysVO[i].setPk_busiman(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_busiman", bodysVO[i].getPk_busiman()));
            // 翻译收款银行账户
            String pk_trader = null;
            String accnum = bodysVO[i].getPk_account();
            if(StringUtil.isNotNull(bodysVO[i].getPk_supplier())){
                pk_trader = bodysVO[i].getPk_supplier();
            }else if(StringUtil.isNotNull(bodysVO[i].getPk_customer())){
                pk_trader = bodysVO[i].getPk_customer();
            }else if(StringUtil.isNotNull(bodysVO[i].getPk_busiman())){
                pk_trader = bodysVO[i].getPk_busiman();
            }
            Integer objecttype = bodysVO[i].getObjecttype();
            if(objecttype ==null){
                throw new BusinessException("交易对方类型不能为空！");
            }

            if(StringUtils.isEmpty(accnum)){
                throw new BusinessException("对方银行账户不能为空！");
            }
            if(objecttype != 4){
                String pk_account = getOppAccount(accnum,pk_trader,objecttype,bodysVO[i].getPk_currtype());
                if(StringUtils.isEmpty(pk_account)){
                    throw new BusinessException("对方账户没有在系统中维护或不属于该交易对象！");
                }
                bodysVO[i].setPk_account(pk_account);
            }
            // 资金计划项目
            bodysVO[i].setBankroll_projet(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "bankroll_projet", bodysVO[i].getBankroll_projet()));
            //科目
            bodysVO[i].setPk_subjct(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_subjct", bodysVO[i].getPk_subjct()));
            //部门
            bodysVO[i].setPk_dept(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_dept", bodysVO[i].getPk_dept()));
            //现金流量项目
            bodysVO[i].setCash_item(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "cash_item", bodysVO[i].getCash_item()));
            //收支项目
            bodysVO[i].setPk_recproject(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_recproject", bodysVO[i].getPk_recproject()));
            //项目
            bodysVO[i].setPk_jobid(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "pk_jobid", bodysVO[i].getPk_jobid()));
            //物料基本信息
            bodysVO[i].setCinventoryid(TMpubCommonRefTranslate.translateExToNCWithOrg(bodysVO[i].getPk_org(),
                    "cinventoryid", bodysVO[i].getCinventoryid()));

            //litao 新费用类型 def1
            bodysVO[i].setDef1(qryDefdocPK(bodysVO[i].getDef1(),"FYLX"));
            //litao 新付款银行类型 def2
            bodysVO[i].setDef2(qryDefdocPK(bodysVO[i].getDef2(),"YHLB-F"));
            //litao 新收款银行类型 def3
            bodysVO[i].setDef3(qryDefdocPK(bodysVO[i].getDef3(),"YHLB-S"));
        }
        aggVO.setChildrenVO(bodysVO);
        return aggVO;
    }

    /**
     * 依据交易类型编码或名称查询交易类型pk
     * @throws MetaDataException
     */
    private void getPk_tradetypeid(BillVO billVO) throws MetaDataException {
        String trade_type = billVO.getTrade_type();//交易类型编码
        String pk_goup = billVO.getPk_group();//当前集团
        IMDPersistenceQueryService query = NCLocator.getInstance().lookup(
                IMDPersistenceQueryService.class);
        String whereCondStr = " pk_group = '" + pk_goup + "' and (pk_billtypecode = '" + trade_type + "' or billtypename = '" + trade_type + "') ";//查询条件
        NCObject[] billtypes = query.queryBillOfNCObjectByCond(BilltypeVO.class, whereCondStr, false);
        if(billtypes!=null&&billtypes.length>0){
            NCObject obVO = billtypes[0];
            if (obVO != null && obVO.getContainmentObject() != null
                    && obVO.getContainmentObject() instanceof BilltypeVO) {
                BilltypeVO billtypevo = (BilltypeVO) obVO
                        .getContainmentObject();
                if (billtypevo.getPk_billtypecode().equals(trade_type) || billtypevo.getBilltypename().equals(trade_type)) {
                    billVO.setPk_tradetypeid(billtypevo.getPk_billtypeid());//交易类型pk赋值
                    billVO.setTrade_type(billtypevo.getPk_billtypecode());
                }
            }
        }
    }

    /**
     * 查询对方银行账户
     * @param accnum
     * @param pk_trader
     * @param objecttype
     * @param pk_currtype
     * @return
     * @throws BusinessException
     */
    private String getOppAccount(String accnum,String pk_trader, Integer objecttype,String pk_currtype) throws BusinessException {
        String whereSql = " and 1=1 ";
        HYPubBO bo =new HYPubBO();
        BankAccSubVO[] subVOs = null;
        //0=客户，1=供应商，2=部门，3=人员，4=散户
        if(objecttype == 0 || objecttype == 1){
            whereSql = " exists (select 1 from bd_custbank where bd_custbank.pk_bankaccsub = bd_bankaccsub.pk_bankaccsub and pk_cust ='"+pk_trader+"') and bd_bankaccsub.pk_currtype='"+pk_currtype+"' and (bd_bankaccsub.accnum='"+accnum+"' or bd_bankaccsub.accname = '" + accnum + "' or bd_bankaccsub.pk_bankaccsub = '" + accnum + "') ";
            subVOs = (BankAccSubVO[])bo.queryByCondition(BankAccSubVO.class, whereSql);
        }else if (objecttype == 2 || objecttype == 3){
            whereSql = " exists (select 1 from bd_psnbankacc where bd_psnbankacc.pk_bankaccsub = bd_bankaccsub.pk_bankaccsub and pk_psndoc ='"+pk_trader+"') and bd_bankaccsub.pk_currtype = '"+pk_currtype+"' and (bd_bankaccsub.accnum='"+accnum+"' or bd_bankaccsub.accname = '" + accnum + "' or bd_bankaccsub.pk_bankaccsub = '" + accnum + "') ";
            subVOs = (BankAccSubVO[])bo.queryByCondition(BankAccSubVO.class, whereSql);
        }

        if(subVOs!=null && subVOs.length>0){
            return subVOs[0].getPk_bankaccsub();
        }
        return null;
    }

    //根据自定义档案编码查询主键
    private String qryDefdocPK(String defcode,String deflistcode) {
        IUAPQueryBS bs = NCLocator.getInstance().lookup(IUAPQueryBS.class);
        String sql = "select b.pk_defdoc from bd_defdoc b where b.code = '" + defcode + "' and b.dr = 0 and b.pk_defdoclist=(select d.pk_defdoclist from bd_defdoclist d where d.code='"+deflistcode+"' and d.dr=0)";
        try {
            String rs=(String)bs.executeQuery(sql, new ColumnProcessor());
            return rs==null?defcode:rs;
        } catch (BusinessException e) {
            e.printStackTrace();
        }
        return null;
    }
    private CircularlyAccessibleValueObject getVO(Map<String, Object> head,CircularlyAccessibleValueObject vo) {
        Set<String> keys = head.keySet();
        for(String key : keys) {
            vo.setAttributeValue(key, head.get(key));
        }
        return vo;
    }

}
