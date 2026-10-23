package nc.itf.ext.lc;

import com.alibaba.fastjson.JSONObject;
import nc.vo.pub.BusinessException;

public interface ILCArapBillService {

    /** 应付单新增（F1）：保存 + 可选提交 */
    JSONObject savePayableBill(JSONObject json) throws BusinessException;

    /** 付款单新增（F3）：保存 + 可选提交 */
    JSONObject savePayBill(JSONObject json) throws BusinessException;
}