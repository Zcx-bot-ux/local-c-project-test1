package nccloud.itf.uapbd.QueryStaffLinkBank;

import com.yonyou.diwork.exception.BusinessException;
import nc.bs.dao.DAOException;
import nccloud.api.uapbd.QueryStaffLinkBank.ViewQueryPagination;
import nccloud.api.uapbd.QueryStaffLinkBank.ViewQueryResult;

public interface IQueryStaffLinkBankService {
    /**
     * 按视图编码查询视图数据。
     *
     * @param req 视图查询请求报文
     * @return 视图字段清单与分页数据
     * @throws BusinessException 视图编码不存在、字段不存在、分页参数不合法等
     */
    ViewQueryResult QueryStaffLinkBank(ViewQueryPagination req) throws BusinessException, DAOException;
}
