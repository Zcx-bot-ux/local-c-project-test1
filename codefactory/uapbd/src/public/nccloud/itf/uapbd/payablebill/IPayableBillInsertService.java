package nccloud.itf.uapbd.payablebill;

import java.util.Map;

import nc.vo.arap.openapi.ArapBillOpenApiVO;
import nc.vo.pub.BusinessException;

public interface IPayableBillInsertService {

    /**
     * 新增应付单（仅保存，不提交）。
     *
     * @param billVO 表头字段 + items 表体行，报文字段与标准开放接口一致，传编码而非 pk
     * @return 标准保存结果 Map（含 code/message/data，data 内为新单 pk 与单据号）
     */
    Map<String, Object> insertPayableBill(ArapBillOpenApiVO billVO) throws BusinessException;
}
