package nccloud.api.uapbd.payablebill;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import nc.bs.framework.common.NCLocator;
import nc.ws.opm.pub.utils.result.APIErrCodeEnum;
import nc.vo.arap.openapi.ArapBillOpenApiVO;
import nc.vo.pub.BusinessException;
import nccloud.api.rest.log.OPMLogger;
import nccloud.api.rest.utils.NCCRestUtils;
import nccloud.api.rest.utils.ResultMessageUtil;
import nccloud.itf.uapbd.payablebill.IPayableBillInsertService;
import nccloud.ws.rest.resource.AbstractNCCRestResource;
import org.json.JSONString;
import org.restlet.resource.Post;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * 应付单新增：POST /nccloud/api/uapbd/payablebill/insert
 * 报文结构与标准开放接口 /nccloud/api/arap/payablebill/insert 一致。
 */
@Path("payablebill")
public class PayableBillInsertResource extends AbstractNCCRestResource {

    private static final String MODULE_CODE = "uapbd";

    private static final String ERROR_MESSAGE = "应付单新增失败，请联系管理员或查看服务端日志！";

    @Override
    public String getModule() {
        return MODULE_CODE;
    }

    @Post
    @Produces({"application/json"})
    @Consumes({"application/json"})
    @Path("insert")
    public JSONString insert(JSONString json) {

        OPMLogger.info(">>>> /nccloud/api/uapbd/payablebill/insert >>>> request body: " + json);
        try {
            ArapBillOpenApiVO billVO = parseBody(json);
            Map<String, Object> result = NCLocator.getInstance().lookup(IPayableBillInsertService.class)
                    .insertPayableBill(billVO);
            OPMLogger.info(">>>> /nccloud/api/uapbd/payablebill/insert >>>> result: " + result);
            return ResultMessageUtil.toJSON(result);
        } catch (BusinessException e) {
            OPMLogger.error(e.getMessage(), e);
            return buildErrorMsg(e.getMessage());
        } catch (Exception e) {
            OPMLogger.error(ERROR_MESSAGE + "：" + e.getMessage(), e);
            return ResultMessageUtil.exceptionToJSON(ERROR_MESSAGE, APIErrCodeEnum.RUNTIMEEXCCODE.getCode());
        }
    }

    /**
     * 校验/业务错误统一封装为 errormsg 返回给调用方。
     */
    private JSONString buildErrorMsg(String errorMsg) {
        Map<String, Object> err = new HashMap<String, Object>();
        err.put("success", Boolean.FALSE);
        err.put("code", APIErrCodeEnum.BUSINESSEXCCODE.getCode());
        err.put("errormsg", errorMsg);
        return NCCRestUtils.toJSONStringNoTranslate(err);
    }

    /**
     * body 为空串时 fastjson 解析结果为 null，直接给出业务提示，避免空指针或框架读流异常。
     */
    private ArapBillOpenApiVO parseBody(JSONString json) throws BusinessException {
        if (json == null) {
            throw new BusinessException("请求报文不能为空！");
        }
        JSONObject param = JSON.parseObject(json.toJSONString());
        if (param == null) {
            throw new BusinessException("请求报文不能为空！");
        }
        return param.toJavaObject(ArapBillOpenApiVO.class);
    }
}
