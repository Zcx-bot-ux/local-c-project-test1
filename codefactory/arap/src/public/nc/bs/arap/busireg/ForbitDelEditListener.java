package nc.bs.arap.busireg;

import nc.bs.businessevent.BdUpdateEvent;
import nc.bs.businessevent.BusinessEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.IEventType;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;

/**
 * 临时单据强管控：修改前(1003)/删除前(1005)拦截，正式单据放行。
 * 注册：pub_eventlistener 订阅应付单事件源的 TYPE_UPDATE_BEFORE / TYPE_DELETE_BEFORE，
 * implclassname = nc.bs.arap.busireg.ForbitDelEditListener
 */
public class ForbitDelEditListener implements IBusinessListener {

    /** 表头临时标识字段（按实际启用的自定义字段调整） */
    private static final String TEMP_FIELD = "def19";
    private static final String HEAD_TABLE = "cmp_paybill";
    private static final String PK_COL = "pk_paybill";

    private BaseDAO dao;

    @Override
    public void doAction(IBusinessEvent event) throws BusinessException {
        String type = event.getEventType();
        if (!IEventType.TYPE_UPDATE_BEFORE.equals(type)
                && !IEventType.TYPE_DELETE_BEFORE.equals(type)) {
            return;
        }
        AggregatedValueObject[] bills = getBills(event);
        if (bills == null) {
            return;
        }
        for (AggregatedValueObject bill : bills) {
            if (bill == null || bill.getParentVO() == null) {
                continue;
            }
            String pk = bill.getParentVO().getPrimaryKey();
            if (pk == null || pk.trim().isEmpty()) {
                continue;
            }
            if (isTempBill(pk)) {
                throw new BusinessException("单据[" + bill.getParentVO().getAttributeValue("billno")
                        + "]为临时单据（OA审批中），不允许修改或删除！");
            }
        }
    }

    /** 强管控以数据库标识为准，不信任前端提交的VO，防止客户端篡改绕过 */
    private boolean isTempBill(String pk) throws BusinessException {
        try {
            Object flag = getDao().executeQuery(
                    "select " + TEMP_FIELD + " from " + HEAD_TABLE
                            + " where " + PK_COL + "='" + pk + "' and nvl(dr,0)=0",
                    rs -> rs.next() ? rs.getObject(1) : null);
            return isTempFlag(flag);
        } catch (DAOException e) {
            throw new BusinessException("临时单据标识校验失败：" + e.getMessage());
        }
    }

    /**
     * def19 编码：01=正式单据，02=临时单据
     * null 视为正式单据（存量单/手工单不受影响），仅 02 才拦截
     */
    private boolean isTempFlag(Object v) {
        return v != null && "02".equals(v.toString().trim());
    }

    protected AggregatedValueObject[] getBills(IBusinessEvent event) {
        Object object = null;
        if (event instanceof BusinessEvent) {
            object = ((BusinessEvent) event).getObject();
        } else if (event instanceof BdUpdateEvent) {
            object = ((BdUpdateEvent) event).getNewObject();
        }
        if (object == null) {
            return null;
        }
        if (object.getClass().isArray()) {
            return (AggregatedValueObject[]) object;
        }
        return new AggregatedValueObject[] { (AggregatedValueObject) object };
    }

    private BaseDAO getDao() {
        if (dao == null) {
            dao = new BaseDAO();
        }
        return dao;
    }
}
