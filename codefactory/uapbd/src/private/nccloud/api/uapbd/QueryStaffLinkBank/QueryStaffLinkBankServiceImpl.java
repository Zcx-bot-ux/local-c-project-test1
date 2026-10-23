package nccloud.api.uapbd.QueryStaffLinkBank;

import com.yonyou.diwork.exception.BusinessException;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.impl.am.db.processor.CountProcessor;
import nc.jdbc.framework.SQLParameter;
import nc.md.data.criterion.db.MapListProcessor;
import nc.vo.bd.asslinenum.AssLineNumVO;
import nccloud.itf.uapbd.QueryStaffLinkBank.IQueryStaffLinkBankService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//人性联行信息查询同步
public class QueryStaffLinkBankServiceImpl implements IQueryStaffLinkBankService {

    /**
     * 默认每页行数
     */
    private static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * 最大每页行数
     */
    private static final int MAX_PAGE_SIZE = 1000;

    /**
     * CLOB 字段最大返回长度
     */
    private static final long MAX_CLOB_LENGTH = 4000L;

    /**
     * 分页取数的内部行号别名，仅出现在子查询中，不会返回给调用方
     */
    private static final String ROWNO_ALIAS = "nc_rn_no";



    @Override
    public ViewQueryResult QueryStaffLinkBank(ViewQueryPagination req) throws BusinessException, DAOException {
        int pageNum = resolvePageNum(req);
        int pageSize = resolvePageSize(req);
        int totalCount = queryTotal();
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        int startRow = (pageNum - 1) * pageSize;
        if (startRow < totalCount) {
            rows = queryRows(pageNum, pageSize);
        }
        ViewQueryResult result = new ViewQueryResult();
        result.setRows(rows);
        result.setTotalPage(Integer.valueOf(totalCount == 0 ? 0 : (totalCount + pageSize - 1) / pageSize));
        result.setPageSize(Integer.valueOf(pageSize));
        result.setTotalCount(Integer.valueOf(totalCount));
        result.setPageNum(Integer.valueOf(pageNum));
        return result;
    }


    private int resolvePageNum(ViewQueryPagination pagination) throws BusinessException {
        if (pagination == null || pagination.getPageNum() == null) {
            return 1;
        }
        int pageNum = pagination.getPageNum().intValue();
        if (pageNum < 1) {
            throw new BusinessException("分页页码(pageNum)必须大于 0！");
        }
        return pageNum;
    }

    private int resolvePageSize(ViewQueryPagination pagination) throws BusinessException {
        if (pagination == null || pagination.getPageSize() == null) {
            return DEFAULT_PAGE_SIZE;
        }
        int pageSize = pagination.getPageSize().intValue();
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("分页大小(pageSize)必须在 1 到 " + MAX_PAGE_SIZE + " 之间！");
        }
        return pageSize;
    }

    private int queryTotal() throws  DAOException {
        AssLineNumVO assLineNumVO = new AssLineNumVO();
        String sql = "select count(1) from bd_asslinenum ";
        Object value = new BaseDAO().executeQuery(sql, new CountProcessor());
        if (value == null) {
            return 0;
        }
        return ((Integer) value).intValue();
    }

    /**
     * 分页取数：SQL Server 先用 row_number() over(order by ...) 生成行号，
     * 再在外层按行号范围过滤，外层只取视图字段，不带内部行号列。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryRows(int pageNum, int pageSize) throws BusinessException, DAOException {
        int startRow = (pageNum - 1) * pageSize;
        int endRow = pageNum * pageSize;
        String sql = "select * from (\n" +
                "    select t.*, row_number() over(order by t.ts) nc_rn_no\n" +
                "    from bd_asslinenum t\n" +
                ") where nc_rn_no > ? and nc_rn_no <= ?";
        SQLParameter param = new SQLParameter();
        param.addParam(Integer.valueOf(startRow));
        param.addParam(Integer.valueOf(endRow));
        Object value = new BaseDAO().executeQuery(sql, param, new MapListProcessor(null, null));
        if (value == null) {
            return new ArrayList<Map<String, Object>>();
        }
        return (List<Map<String, Object>>) value;
    }


    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

