package nccloud.arap.payablebill;

import com.alibaba.fastjson.JSONArray;
import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.diwork.exception.BusinessException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.pubitf.bd.accessor.GeneralAccessorFactory;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.vo.arap.payable.AggPayableBillVO;
import nc.vo.arap.payable.PayableBillItemVO;
import nc.vo.arap.payable.PayableBillVO;
import nc.vo.bd.accessor.IBDData;
import nc.vo.obm.payroll.DfgzBVO;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.ISuperVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nccloud.ws.rest.resource.AbstractNCCRestResource;

import com.alibaba.fastjson.JSONObject;
import org.json.JSONString;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

@Path("arap/payablebill")
public class InsertPayableResources extends AbstractNCCRestResource {

    //DfgzHVO主表dfgz_h
    private static final String MD_PK_ORG = "2cfe13c5-9757-4ae8-9327-f5c2d34bcb46";
    //    private static final String MD_PK_GROUP = "17282e0e-892e-4503-8e5d-6c40ccf2723e";
    private static final String MD_PK_BANKTYPE = "1d3d003d-0e8c-4283-9580-0ef81c93ba58";
    private static final String MD_CRDTTYP = "8d761584-44e7-4cd5-af34-5be1d8e9be4e";
    //DfgzBVO子表dfgz_b
    private static final String MD_PK_CURRTYPE = "b498bc9a-e5fd-4613-8da8-bdae2a05704a";

    @Override
    public String getModule() {
        return "arap";
    }
    @POST
    @Path("/insert")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public JSONString addBill(JSONObject json) {
        try {
            if (json == null || json.isEmpty()) {
                throw new BusinessException("传入参数为空，请检查！");
            }

            AggPayableBillVO billVO = convertToPayable(json);
            //非空校验
            validateData(billVO);
            //翻译字段
            transferBill(billVO);
            //填充系统字段
            this.fillData(billVO);
            //保存
            IEbankdfgzMaintain maintain = NCLocator.getInstance().lookup(IEbankdfgzMaintain.class);
//            DfgzBillVO[] dfgzBillVO = maintain.insert(new DfgzBillVO[]{billVO});
            DfgzBillVO dfgzBillVO = maintain.saveDfgzBillWithStep(billVO, false, false,  null,InvocationInfoProxy.getInstance().getUserId());

//            String bill_no = dfgzBillVO[0].getParentVO().getVbillno() ;
            String bill_no = dfgzBillVO.getParentVO().getVbillno();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("bill_no", bill_no);
            return ResultMessageUtil.toJSON(map);

        } catch (BusinessException e) {
            return ResultMessageUtil.exceptionToJSON(e.getMessage(), "0001");
        }
    }

    /**
     * 翻译字段：表头/表体的编码→pk（组织、币种、客商、部门、人员、税码、交易类型）
     * 均由标准 openapi 链 toAggVO("F1") 内部完成，与标准资源 PayablebillRestResource.insertBill 同链。
     */
    private void transferBill(AggPayableBillVO billVO) throws BusinessException {
        PayableBillVO headVO = (PayableBillVO) billVO.getParentVO();
        String pk_group = InvocationInfoProxy.getInstance().getGroupId();
        String orgCode = headVO.getPk_org();
        String pk_org = this.translateAttribute(orgCode, MD_PK_ORG, pk_group);
        if (StringUtils.isEmpty(pk_org)) {
            throw new BusinessException("未查询到编码为 " + orgCode + " 的财务组织！");
        }
        //=====================头部字段翻译 start=====================
        //pk_tradetype 交易类型编码
        String pk_tradetype = doTranslateDoc(headVO.getPk_tradetype(), MD_TRADETYPE, pk_org, "交易类型");
        headVO.setPk_tradetype(pk_tradetype);

        //supplier 供应商编码
        String supplierPk = doTranslateDoc(headVO.getSupplier(), MD_SUPPLIER, pk_org, "供应商");
        headVO.setSupplier(supplierPk);

        //pk_dept 部门编码
        String deptPk = doTranslateDoc(headVO.getPk_dept(), MD_DEPT, pk_org, "部门");
        headVO.setPk_dept(deptPk);

        //pk_psndoc 业务员编码
        String psndocPk = doTranslateDoc(headVO.getPk_psndoc(), MD_PSNDOC, pk_org, "业务员");
        headVO.setPk_psndoc(psndocPk);

        //pk_currtype 币种编码
        String currtypePk = doTranslateDoc(headVO.getPk_currtype(), MD_CURRTYPE, pk_org, "币种");
        headVO.setPk_currtype(currtypePk);

        //pk_busitype 业务流程编码
        String busitypePk = doTranslateDoc(headVO.getPk_busitype(), MD_BUSITYPE, pk_org, "业务流程");
        headVO.setPk_busitype(busitypePk);

        //sett_org 结算财务组织编码
        String settOrgPk = doTranslateDoc(headVO.getSett_org(), MD_ORG, pk_org, "结算财务组织");
        headVO.setSett_org(settOrgPk);

        //pk_pcorg 利润中心编码
        String pcorgPk = doTranslateDoc(headVO.getPk_pcorg(), MD_PCORG, pk_org, "利润中心");
        headVO.setPk_pcorg(pcorgPk);

        //pu_org 业务组织编码
        String puOrgPk = doTranslateDoc(headVO.getPu_org(), MD_ORG, pk_org, "业务组织");
        headVO.setPu_org(puOrgPk);

        //pu_deptid 业务部门编码
        String puDeptPk = doTranslateDoc(headVO.getPu_deptid(), MD_DEPT, pk_org, "业务部门");
        headVO.setPu_deptid(puDeptPk);

        //pu_psndoc 业务人员编码
        String puPsnPk = doTranslateDoc(headVO.getPu_psndoc(), MD_PSNDOC, pk_org, "业务人员");
        headVO.setPu_psndoc(puPsnPk);

        //ap_pk_payterm 付款协议编码
        String paytermPk = doTranslateDoc(headVO.getAp_pk_payterm(), MD_PAYTERM, pk_org, "付款协议");
        headVO.setAp_pk_payterm(paytermPk);

        //material 物料编码
        String materialPk = doTranslateDoc(headVO.getMaterial(), MD_MATERIAL, pk_org, "物料");
        headVO.setMaterial(materialPk);

        //pk_ratetype 组织汇率类型
        String ratetypePk = doTranslateDoc(headVO.getPk_ratetype(), MD_RATETYPE, pk_org, "汇率类型");
        headVO.setPk_ratetype(ratetypePk);

        PayableBillItemVO[] childrens = (PayableBillItemVO[])billVO.getChildren(PayableBillItemVO.class);
        if (childrens != null ) {
            for(int i = 0; i<childrens.length;i++) {
                //supplier 供应商编码
                String itemSupplierPk = doTranslateDoc(childrens[i].getSupplier(), MD_SUPPLIER, pk_org, "供应商");
                childrens[i].setSupplier(itemSupplierPk);

                //pk_dept 部门编码
                String itemDeptPk = doTranslateDoc(childrens[i].getPk_dept(), MD_DEPT, pk_org, "部门");
                childrens[i].setPk_dept(itemDeptPk);

                //pk_psndoc 业务员编码
                String itemPsnPk = doTranslateDoc(childrens[i].getPk_psndoc(), MD_PSNDOC, pk_org, "业务员");
                childrens[i].setPk_psndoc(itemPsnPk);

                //pk_currtype 币种编码
                String itemCurrPk = doTranslateDoc(childrens[i].getPk_currtype(), MD_CURRTYPE, pk_org, "币种");
                childrens[i].setPk_currtype(itemCurrPk);

                //taxcodeid 税码编码
                String taxcodePk = doTranslateDoc(childrens[i].getTaxcodeid(), MD_TAXCODE, pk_org, "税码");
                childrens[i].setTaxcodeid(taxcodePk);

                //sett_org 结算财务组织编码
                String itemSettOrgPk = doTranslateDoc(childrens[i].getSett_org(), MD_ORG, pk_org, "结算财务组织");
                childrens[i].setSett_org(itemSettOrgPk);

                //pk_pcorg 利润中心编码
                String itemPcorgPk = doTranslateDoc(childrens[i].getPk_pcorg(), MD_PCORG, pk_org, "利润中心");
                childrens[i].setPk_pcorg(itemPcorgPk);

                //pu_org 业务组织编码
                String itemPuOrgPk = doTranslateDoc(childrens[i].getPu_org(), MD_ORG, pk_org, "业务组织");
                childrens[i].setPu_org(itemPuOrgPk);

                //pu_deptid 业务部门编码
                String itemPuDeptPk = doTranslateDoc(childrens[i].getPu_deptid(), MD_DEPT, pk_org, "业务部门");
                childrens[i].setPu_deptid(itemPuDeptPk);

                //pu_psndoc 业务人员编码
                String itemPuPsnPk = doTranslateDoc(childrens[i].getPu_psndoc(), MD_PSNDOC, pk_org, "业务人员");
                childrens[i].setPu_psndoc(itemPuPsnPk);

                //pk_subjcode 收支项目编码
                String subjcodePk = doTranslateDoc(childrens[i].getPk_subjcode(), MD_SUBJ, pk_org, "收支项目");
                childrens[i].setPk_subjcode(subjcodePk);

                //cashitem 现金流量项目编码
                String cashitemPk = doTranslateDoc(childrens[i].getCashitem(), MD_CASHITEM, pk_org, "现金流量项目");
                childrens[i].setCashitem(cashitemPk);

                //ap_payaccount 付款银行账户编码
                String payAccountPk = doTranslateDoc(childrens[i].getAp_payaccount(), MD_BANKACCOUNT, pk_org, "付款银行账户");
                childrens[i].setAp_payaccount(payAccountPk);

                //ap_recaccount 收款银行账户编码
                String recAccountPk = doTranslateDoc(childrens[i].getAp_recaccount(), MD_BANKACCOUNT, pk_org, "收款银行账户");
                childrens[i].setAp_recaccount(recAccountPk);

                //costcenter 成本中心编码
                String costcenterPk = doTranslateDoc(childrens[i].getCostcenter(), MD_COSTCENTER, pk_org, "成本中心");
                childrens[i].setCostcenter(costcenterPk);

                //productline 产品线编码
                String productlinePk = doTranslateDoc(childrens[i].getProductline(), MD_PRODUCTLINE, pk_org, "产品线");
                childrens[i].setProductline(productlinePk);

                //pk_balatype 结算方式编码
                String balatypePk = doTranslateDoc(childrens[i].getPk_balatype(), MD_BALATYPE, pk_org, "结算方式");
                childrens[i].setPk_balatype(balatypePk);

                //pk_ratetype 组织汇率类型
                String itemRateTypePk = doTranslateDoc(childrens[i].getPk_ratetype(), MD_RATETYPE, pk_org, "汇率类型");
                childrens[i].setPk_ratetype(itemRateTypePk);
            }

        }
    }
    /**
     * 档案编码翻译统一处理
     * @param code 传入编码
     * @param mdMeta 档案元常量
     * @param pkOrg 组织pk
     * @param desc 档案名称，用于异常提示
     * @return 翻译后的pk，code为空返回null
     * @throws BusinessException 编码有值，但翻译结果为空时抛出
     */
    private String doTranslateDoc(String code, String mdMeta, String pkOrg, String desc) throws BusinessException {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        String pk = this.translateAttribute(code, mdMeta, pkOrg);
        if (StringUtils.isEmpty(pk)) {
            throw new BusinessException("编码【" + code + "】未匹配到有效的" + desc + "档案！");
        }
        return pk;
    }

    private String translateAttribute(String code, String metaId, String pk_org) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        IGeneralAccessor accessor = GeneralAccessorFactory.getAccessor(metaId);
        IBDData bdData = accessor.getDocByCode(pk_org, code);
        if (bdData != null) {
            return bdData.getPk();
        }
        return null;
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
    private void validateData(AggPayableBillVO billVO) throws BusinessException{
        if (billVO == null) {
            throw new BusinessException("参数不能为空！");
        }
        try {
            PayableBillVO parent = (PayableBillVO)billVO.getParent();
            requireText(parent.getPk_org(), "pk_org(财务组织编码)");
            requireText(parent.getPk_tradetype(), "pk_tradetype(交易类型编码)");
            requireUFDate(parent.getBilldate(), "billdate(制单日期)");
            requireInteger(parent.getObjtype(), "objtype(往来对象)");
            requireText(parent.getPk_currtype(), "pk_currtype(币种编码)");
            requireUFDouble(parent.getMoney(), "money(原币金额)");
            requireUFDouble(parent.getRate(), "rate(组织本币汇率)");
            requireUFDouble(parent.getLocal_money(), "local_money(组织本币金额)");
            requireUFDouble(parent.getGrouprate(), "grouprate(集团本币汇率)");
            requireUFDouble(parent.getGrouplocal(), "grouplocal(集团本币金额)");
            requireUFDouble(parent.getGlobalrate(), "globalrate(全局本币汇率)");
            requireUFDouble(parent.getGloballocal(), "globallocal(全局本币金额)");
            requirePartyByObjType(Integer.toString (parent.getObjtype()), parent.getSupplier(), parent.getPk_deptid(), parent.getPk_psndoc(), "");
            PayableBillItemVO[] childrens = (PayableBillItemVO[])billVO.getChildren(PayableBillVO.class);
            if(childrens != null && childrens.length != 0 ) {
                for (int i = 0; i < childrens.length; i++) {
                    PayableBillItemVO children = childrens[i];
                    requirePartyByObjType(Integer.toString (children.getObjtype()), children.getSupplier(), children.getPk_deptid(), children.getPk_psndoc(), "");
                    requireText(children.getPk_currtype(), "pk_currtype(币种编码)");
                    requireUFDouble(children.getMoney_cr(),"money_cr借方原币金额");
                    requireUFDouble(children.getRate(), "rate(组织本币汇率)");
                    requireUFDouble(children.getLocal_money_cr(), "local_money(组织本币金额)");
                    requireUFDouble(children.getGrouprate(), "grouprate(组织本币金额)");
                    requireUFDouble(children.getGroupcrebit(), "grouplocal(集团本币金额)");
                    requireUFDouble(children.getGlobalrate(), "globalrate(全局本币汇率)");
                    requireUFDouble(children.getGlobalcrebit(), "globallocal(全局本币金额)");
                    requireUFDouble(children.getPrice(), "globallocal(全局本币金额)");
                    requireText(children.getTaxcodeid(), "globallocal(全局本币金额)");
                }
            }
        }catch(BusinessException e){

            throw e;
        }

    }
    private void requireUFDate(UFDate value, String field) throws BusinessException {
        if (value == null ){
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }
    private void requireUFDouble(UFDouble value, String field) throws BusinessException {
        if (value == null ){
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }
    private void requireText(String value, String field) throws BusinessException {
        if (value == null || value.trim().length() == 0) {
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }

    private void requireInteger(Integer value, String field) throws BusinessException {
        if (value == null) {
            throw new BusinessException("必填字段为空：" + field + "！");
        }
    }
    /**
     * 往来对象联动校验（NCC 规则）：1-供应商 2-部门 3-业务员，对应档案编码必填。
     */
    private void requirePartyByObjType(String objtype, String supplier, String pkDept, String pkPsndoc,
                                       String label) throws BusinessException {
        if (objtype == null) {
            return;
        }
        if ("1".equals(objtype)) {
            requireText(supplier, label + "supplier(供应商编码)");
        } else if ("2".equals(objtype)) {
            requireText(pkDept, label + "pk_dept(部门编码)");
        } else if ("3".equals(objtype)) {
            requireText(pkPsndoc, label + "pk_psndoc(业务员编码)");
        }
    }

    private AggPayableBillVO convertToPayable(JSONObject json) {

        AggPayableBillVO AggPayableBillVO = new AggPayableBillVO();

        //主表
        if(json != null){
            PayableBillVO billVO = new PayableBillVO();
            for (String key : json.keySet()) {
                Object value = json.get(key);
                if(!"items".equals(key)){
                    if ("".equals(value)) value = null;
                    billVO.setAttributeValue(key,value);
                }
            }
            AggPayableBillVO.setParentVO(billVO);
            JSONArray items = json.getJSONArray("items");
            if(items != null && !items.isEmpty()){
                PayableBillItemVO[] itemsData = new PayableBillItemVO[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    JSONObject body = items.getJSONObject(i);
                    PayableBillItemVO item = new PayableBillItemVO();
                    for (String key : body.keySet()) {
                        Object value = body.get(key);
                        if("".equals(value)) value = null;
                        item.setAttributeValue(key,value);
                    }
                    itemsData[i] = item;
                }
                AggPayableBillVO.setChildren(PayableBillItemVO.class,itemsData);
            }
            return AggPayableBillVO;

        }


    }


}
