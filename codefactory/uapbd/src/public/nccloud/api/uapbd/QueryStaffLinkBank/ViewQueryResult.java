package nccloud.api.uapbd.QueryStaffLinkBank;


import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 类说明：视图查询返回结果，对应报文中的 data 节点。
 * 报文结构：
 * {
 *   "success": true,
 *   "data": {
 *     "rows": [{"filed1": "xxxxxxx", "filed2": "xxxxxx"}],
 *     "totalPage": 4,
 *     "pageSize": 50,
 *     "totalCount": 152,
 *     "pageNum": 1
 *   },
 *   "code": "1000000000"
 * }
 * rows 为当前页数据，每行是该视图的全部字段（字段名到字段值的映射）。
 */
public class ViewQueryResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据，每行为视图全部字段 */
    private List<Map<String, Object>> rows;

    /** 总页数 */
    private Integer totalPage;

    /** 每页行数 */
    private Integer pageSize;

    /** 总行数 */
    private Integer totalCount;

    /** 当前页码 */
    private Integer pageNum;

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
}
