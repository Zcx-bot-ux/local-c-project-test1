package nccloud.api.uapbd.QueryStaffLinkBank;

import java.io.Serializable;

/**
 * 类说明：视图查询分页参数。
 * 报文示例：{"pageNum":1,"pageSize":50}
 */
public class ViewQueryPagination implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，从 1 开始，不传默认为 1 */
    private Integer pageNum;

    /** 每页行数，不传默认为 50，最大 1000 */
    private Integer pageSize;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
