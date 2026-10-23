package nccloud.api.arap.kk.lc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import nc.bs.framework.common.NCLocator;
import nc.itf.ext.lc.ILCArapBillService;
import nc.itf.single.pub.ExceptionUtil;
import nc.vo.pub.BusinessException;
import nc.ws.opm.pub.utils.result.APIErrInfoUtil;
import nccloud.api.rest.utils.NCCRestUtils;
import nccloud.api.rest.utils.ResultMessageUtil;
import nccloud.ws.rest.resource.AbstractNCCRestResource;
import org.json.JSONString;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("arap/lcbill")
public class LCArapBillRestResource extends AbstractNCCRestResource {

    nc.bs.logging.Log log = nc.bs.logging.Log.getInstance("lcbill");

    @Override
    public String getModule() {
        return "arap";
    }

    /** 应付单新增（F1） */
    @POST
    @Path("/payablebill/save")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public JSONString payablebillSave(JSONString json) throws BusinessException {
        try {
            JSONObject jObject = JSON.parseObject(json.toJSONString());
            JSONObject result = NCLocator.getInstance()
                    .lookup(ILCArapBillService.class).savePayableBill(jObject);
            return ResultMessageUtil.toJSON(result, "应付单保存成功！");
        } catch (Exception e) {
            log.error("应付单新增接口异常：" + ExceptionUtil.getStackTrace(e));
            return errmsg(ExceptionUtil.getStackTrace(e), e.toString(), e);
        }
    }

    /** 付款单新增（F3） */
    @POST
    @Path("/paybill/save")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public JSONString paybillSave(JSONString json) throws BusinessException {
        try {
            JSONObject jObject = JSON.parseObject(json.toJSONString());
            JSONObject result = NCLocator.getInstance()
                    .lookup(ILCArapBillService.class).savePayBill(jObject);
            return ResultMessageUtil.toJSON(result, "付款单保存成功！");
        } catch (Exception e) {
            log.error("付款单新增接口异常：" + ExceptionUtil.getStackTrace(e));
            return errmsg(ExceptionUtil.getStackTrace(e), e.toString(), e);
        }
    }

    private JSONString errmsg(String errorStack, String msg, Exception e) {
        JSONObject rm = new JSONObject();
        rm.put("errorStack", errorStack);
        rm.put("message", msg);
        rm.put("code", APIErrInfoUtil.getCode(e));
        rm.put("success", false);
        return NCCRestUtils.toJSONString(rm);
    }
}