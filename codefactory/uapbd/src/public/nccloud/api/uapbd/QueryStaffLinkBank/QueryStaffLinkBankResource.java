package nccloud.api.uapbd.QueryStaffLinkBank;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yonyou.diwork.exception.BusinessException;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.ws.opm.pub.utils.result.APIErrCodeEnum;
import nccloud.api.rest.log.OPMLogger;
import nccloud.api.rest.utils.ResultMessageUtil;
import nccloud.itf.uapbd.QueryStaffLinkBank.IQueryStaffLinkBankService;
import nccloud.ws.rest.resource.AbstractNCCRestResource;
import org.json.JSONString;
import org.restlet.resource.Post;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("staffLinkBank")
public class QueryStaffLinkBankResource extends AbstractNCCRestResource {
    /**
     * 模块编码
     */
    private static final String MODULE_CODE = "uapbd";


    private static final String DB_ERROR_MESSAGE = "视图查询执行失败，请联系管理员或查看服务端日志！";

    @Override
    public String getModule() {
        return MODULE_CODE;
    }

    @Post
    @Produces({"application/json"})
    @Consumes({"application/json"})
    @Path("query")
    public JSONString query(JSONString json) {

        OPMLogger.info(">>>> /nccloud/api/uapbd/staffLinkBank/query >>>> request body: " + json);
        try {

            ViewQueryPagination req = new ViewQueryPagination();
            if (json != null) {
                JSONObject param = JSON.parseObject(json.toJSONString());
                req = param.toJavaObject(ViewQueryPagination.class);
            }
            ViewQueryResult result = NCLocator.getInstance().lookup(IQueryStaffLinkBankService.class).QueryStaffLinkBank(req);

            OPMLogger.info(">>>> /nccloud/api/uapbd/staffLinkBank/query >>>>"
                    + ", totalCount: " + result.getTotalCount()
                    + ", rowCount: " + (result.getRows() == null ? 0 : result.getRows().size()));
            return ResultMessageUtil.toJSONUnTranslate(result);
        } catch(DAOException e){
            // 数据库报错只写日志，只把统一提示返回给调用方
            OPMLogger.error("视图查询执行数据库操作失败：" + e.getMessage(), e);
            return ResultMessageUtil.exceptionToJSON(DB_ERROR_MESSAGE, APIErrCodeEnum.RUNTIMEEXCCODE.getCode());
        } catch(BusinessException e){
            OPMLogger.error(e.getMessage(), e);
            return ResultMessageUtil.exceptionToJSON(e);
        } catch(Exception e){
            OPMLogger.error(e.getMessage(), e);
            return ResultMessageUtil.exceptionToJSON(e);
        }

    }
}