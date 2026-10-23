# NCC 高级版（NCCloud）接口案例说明

> 当前环境：`home1/ierp/bin/product_version_init.properties` → `product_version=2025.05`
> 判定：本项目是 **NCC 高级版（NCCloud 2025.05）**，不是 NC65。

## 一、与 NC65 的核心区别速查

| 维度 | NC65 | NCC 高级版（本案例） |
|---|---|---|
| 中间件 | WebSphere / WebLogic / Tomcat | Yonyou Cloud Middleware |
| API 范式 | `nc.itf.*` + `nc.impl.*` + `.upt`（EJB） | `@RestController`（Spring MVC） |
| 部署目录 | NC_HOME/modules | NC_HOME/hotwebs/nccloud/WEB-INF |
| 客户端 | Swing | 浏览器（HTML5） |
| 网关前缀 | 无 | `/nccloud/api` |
| 鉴权 | Session | OAuth2 / AppSecret 签名（x-gw-* 头） |

## 二、案例文件清单（位于 `uapbd/uapbd/src/public/`）

```
public/
├── controller/
│   └── CustomerController.java   ← REST 入口，@RestController + ISuccessExecute
├── service/
│   └── CustomerService.java      ← 业务逻辑，BaseDAO + BeanListProcessor
├── dto/
│   └── CustomerDTO.java          ← 数据载体（对应 bd_customer）
└── vo/
    └── ApiResponse.java          ← 统一响应包装（已有）

META-INF/nccloudService/
└── uapbd_customer_service.xml    ← 接口注册（出现在 OpenAPI 列表里）
```

## 三、运行时调用示例

### 1. 浏览器直接调（已登录 NCC）
```
GET http://<host>:<port>/nccloud/api/uapbd/customer/list?keyword=客户A&page=1&pageSize=20
```

### 2. 外部系统调（带网关签名，参考 utils/SignatureDigital.java）
```
GET /nccloud/api/uapbd/customer/list?keyword=xxx
Headers:
  x-gw-appid:      saas2025
  x-gw-timestamp:  1753372800
  x-gw-nonce:      xxxxxxxx-xxxxxx
  x-gw-signature:  md5(sha256(appSecret + nonce + timestamp))
```

### 3. 响应示例
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "rows": [
      {
        "pkCustomer": "1001A110000000001AAA",
        "code": "C001",
        "name": "北京 XX 客户",
        "orgCode": "00",
        "enableState": "2",
        "taxRate": "0.13",
        "modifiedTime": "2026-07-20 10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 20
  }
}
```

## 四、关键编码约定（与现有 MaterialService 一致）

1. **三段工程**：DTO / VO / Controller / Service 都放 `src/public`，可被 client / private 共享。
2. **DAO**：用 `nc.bs.dao.BaseDAO` + `executeQuery`，参数化用 `?` 配合 `executeQuery(sql, processor, params)`。
3. **分页**：当前实现用 `OFFSET ... FETCH NEXT ...`（SQL Server 2012+）；如用 Oracle 改 `ROWNUM <= :end` 或 `ROW_NUMBER() OVER(...)`。
4. **异常**：Service 抛 `Exception`，Controller 捕获后转 `ApiResponse.fail`，避免把堆栈抛给前端。
5. **签名工具**：复用 `utils.SignatureDigital.getSignature()`，所有对外请求统一加 `x-gw-*` 头。

## 五、与本项目现有 MaterialController 的差异

| 点 | MaterialController | CustomerController（本案例） |
|---|---|---|
| 方向 | NC → 推送外部 | 外部 → 查 NC |
| 数据源 | bd_material（物料） | bd_customer（客户） |
| 查询 | MapListProcessor + 手工组装 | BeanListProcessor（自动按列别名映射到 DTO） |
| 日志 | 用未声明的 log/console（**编译会失败**） | 用 `nc.utils.LoggerUtil` |
| 注册 | 无 | 增加 `META-INF/nccloudService/*.xml` |

> ⚠️ 现有 `MaterialService.java` 里 `log.info(...)` 和 `console.info(...)` 没声明，编译会报错。
> 建议参考本案例的 `LoggerUtil.error(...)` 用法替换。
