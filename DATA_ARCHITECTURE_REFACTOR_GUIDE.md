# WheatMarket 数据架构重构指南

本文用于指导后续 AI Agent 重构 WheatMarket 的数据层、市场交易逻辑和持久化逻辑。

## 1. 核心结论

- 不要把市场/经济数据等到游戏保存存档时才持久化。
- 余额、库存、上架、下架、购买记录属于经济数据，必须在业务操作发生时通过异步数据库任务事务提交。
- 数据库写入不能阻塞 Minecraft 主线程。
- 世界保存事件只能作为补充 checkpoint，不应作为主要持久化机制。
- 数据库应是市场和经济数据的权威来源。
- 内存缓存只能是读取加速层，不能绕过服务层随意修改。
- 所有市场和余额变更必须通过服务层完成。
- 只重构当前已经存在的调用路径，不为尚未实现的 UI 或玩法需求预创建数据操作方法。

## 2. 当前状态与剩余风险

当前已经落地：

- Command 和已存在的市场 Packet 已改为调用 service，不再直接调用旧表类。
- 旧 `database.tables` 静态表类已删除。
- SQL 已集中到 repository，repository 不吞 `SQLException`。
- `TransactionManager` 已作为事务边界，service 在事务成功后才更新缓存和返回成功。
- 数据库写操作已通过单线程 DB executor 异步串行执行，避免阻塞 Minecraft 主线程。
- `MarketItemCache` 不再暴露可变 Map 给业务层直接修改。

仍需处理：

- 货币继续使用 `double`；这是当前项目接受的轻量取舍，但必须拒绝 `NaN`、`Infinity` 和非法金额。
- NBT 仍使用 `CompoundTag.toString()` / `TagParser.parseTag()` 持久化，缺少稳定序列化边界。
- GUI 已用 LDLib2 接入市场列表请求、出售单购买确认、完整上架表单、商品详情/管理界面和通用物品选择字段编辑子界面；库存编辑当前通过专门的
  编辑锁 + begin/finalize 子流程接入网络和 service。
- 市场列表真实渲染、筛选、搜索和分页已完成；商品卡片信息仍较少，repository 层条件查询/分页 SQL、过期清理、收购模式和部分体验收尾仍未完成。
- 玩家物品栏与数据库仍不是同一事务系统，事务成功后的物品栏补偿逻辑仍需随 UI 流程完善。

## 3. 当前目标架构

当前采用的分层：

```text
client/gui
  -> network payload
    -> server packet handler
      -> application service
        -> db executor
          -> transaction manager
            -> repositories
              -> database
          -> cache updater
```

当前已使用的核心包：

```text
top.rookiestwo.wheatmarket.database.repository
top.rookiestwo.wheatmarket.database.transaction
top.rookiestwo.wheatmarket.service
top.rookiestwo.wheatmarket.service.result
```

旧 `database.tables` 已迁移为 repository 并删除。

不要提前创建未来功能的 repository/service 方法。只有当现有 Command、Packet、生命周期事件或缓存加载路径已经需要某个操作时，才实现对应方法。

## 4. 设计原则

### 4.1 服务层是唯一业务入口

Packet 和 Command 不允许直接调用 repository/table。

它们只能调用服务层，例如：

```java
MarketService.buyItem(player, marketItemId, amount);
MarketService.listItem(player, request);
MarketService.manageItem(player, request);
EconomyService.transfer(senderId, targetId, amount);
```

以上示例只代表当前已有 packet/command 的迁移目标。不要为了未来界面提前增加未被现有代码调用的方法。

服务层负责：

- 参数验证
- 权限验证
- 调度异步数据库任务
- 事务边界
- 数据库写入
- 缓存更新
- 返回明确的成功/失败结果

### 4.2 Repository 只负责 SQL

Repository 不做游戏业务判断。

Repository 规则：

- 不吞异常。
- 不返回伪成功。
- 写操作失败必须抛出异常或返回明确失败。
- 不访问 `WheatMarket.DATABASE` 全局状态。
- 不直接发送网络消息。
- 不直接修改玩家物品栏。

### 4.3 数据库立即持久化

以下操作必须立即写数据库：

- 余额增减
- 转账
- 商品上架
- 商品购买
- 商品补货
- 商品取出
- 商品改价
- 商品下架
- 购买记录写入

世界保存事件可做：

- flush 非关键脏缓存
- 清理过期商品
- 写统计数据
- 校验缓存和数据库一致性

世界保存事件不应作为交易数据的唯一持久化时机。

## 5. 线程模型

### 5.1 Minecraft 主线程

必须在主线程执行：

- 读取和修改玩家物品栏
- 给玩家发消息
- 修改 Minecraft 世界状态
- 打开/关闭 GUI

### 5.2 数据库线程

必须使用单线程 DB executor 串行处理市场和经济写操作。

原因：

- 当前项目规模小。
- H2 嵌入式数据库足够支持。
- 串行写入可以显著降低并发购买和连接共享风险。
- 比多写线程更容易实现正确事务。

建议：

```java
ExecutorService dbExecutor = Executors.newSingleThreadExecutor(...);
```

注意：

- DB executor 不能直接改玩家物品栏。
- 需要回到主线程执行玩家物品栏变更。

服务层方法建议返回 `CompletableFuture<ServiceResult<T>>`。

Packet/Command 调用流程：

1. 主线程读取玩家、权限、物品栏快照。
2. 调用服务层提交 DB executor 任务。
3. DB executor 内执行事务和 repository 调用。
4. DB 任务完成后回到主线程发送消息、更新 UI、修改物品栏。

## 6. 连接与事务

### 6.1 不要全局共享裸 Connection

`WheatMarketDatabase` 不应暴露 `getConnection()` 给业务层随意使用。

建议改为：

```java
TransactionManager.inTransaction(connection -> {
    // repository calls
});
```

`TransactionManager` 只能在 DB executor 中使用。

### 6.2 事务工具

建议新增：

```java
public final class TransactionManager {
    public <T> T execute(TransactionCallback<T> callback);
}
```

行为：

- 获取连接。
- `setAutoCommit(false)`。
- 执行业务回调。
- 成功则 `commit()`。
- 失败则 `rollback()`。
- 最后恢复/关闭连接。

### 6.3 事务粒度

每个业务操作一个事务：

- 一次购买 = 一个事务。
- 一次转账 = 一个事务。
- 一次上架 = 一个事务。
- 一次商品管理操作 = 一个事务。

不要让多个独立 SQL 自动提交组成一个业务操作。

## 7. 货币模型

当前项目继续使用 `double` 存储余额和价格，数据库继续使用 `DOUBLE`。

原因：

- 本 MOD 是轻量玩法附加，不做真实金融结算。
- 当前经济逻辑较简单，不需要强制引入整数最小单位模型。
- 迁移到 `long` / `BIGINT` 会带来旧数据迁移和 UI 输入显示改造成本。

使用 `double` 时必须遵守：

- 金额输入必须是有限数：拒绝 `NaN` 和 `Infinity`。
- 增加、扣除、转账、价格必须 `> 0`。
- 设置余额允许 `0`，但不允许负数或非有限数。
- 显示给玩家时统一格式化到固定小数位，避免暴露浮点误差字符串。
- 业务判断只能在 service 层完成，packet/command 的校验只是提前失败。

未来如果需要更强精度，再把货币模型作为可选迁移：Java 改为 `long`，数据库改为 `BIGINT`，UI 负责小数显示和输入转换。

## 8. 数据模型建议

### 8.1 player_info

```sql
CREATE TABLE player_info (
    uuid VARCHAR(36) PRIMARY KEY,
    balance DOUBLE
);
```

### 8.2 market_item

```sql
CREATE TABLE market_item (
    MarketItemID VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(255) NOT NULL,
    itemNBTCompound CLOB,
    sellerID VARCHAR(36) NOT NULL,
    price DOUBLE NOT NULL,
    amount INT NOT NULL,
    ifInfinite BOOLEAN DEFAULT FALSE,
    listingTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    ifAdmin BOOLEAN DEFAULT FALSE,
    ifSell BOOLEAN DEFAULT TRUE,
    cooldownAmount INT DEFAULT 0,
    cooldownTimeInMinutes INT DEFAULT 0,
    timeToExpire BIGINT DEFAULT 0,
    lastTradeTime DATETIME,
    FOREIGN KEY (sellerID) REFERENCES player_info(uuid)
);
```

当前 `MarketItemRepository` 启动时会执行轻量迁移：为旧表补充 `ifInfinite` 列，并把旧 `amount = Integer.MAX_VALUE` 的无限商品迁移为
`ifInfinite = TRUE, amount = 1`。

### 8.3 purchase_record

```sql
CREATE TABLE purchase_record (
    recordID VARCHAR(36) PRIMARY KEY,
    marketItemID VARCHAR(36) NOT NULL,
    buyerID VARCHAR(36) NOT NULL,
    lastPurchaseTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    purchasedAmount INT NOT NULL,
    FOREIGN KEY (marketItemID) REFERENCES market_item(MarketItemID) ON DELETE CASCADE,
    FOREIGN KEY (buyerID) REFERENCES player_info(uuid)
);
```

### 8.4 indexes

当前 repository 尚未创建额外索引；如果后续补充，应使用当前实际字段名，例如：

```sql
CREATE INDEX idx_market_item_seller ON market_item(sellerID);
CREATE INDEX idx_market_item_query ON market_item(ifSell, ifAdmin, listingTime);
CREATE INDEX idx_purchase_record_item_buyer ON purchase_record(marketItemID, buyerID, lastPurchaseTime);
CREATE INDEX idx_purchase_record_buyer ON purchase_record(buyerID, lastPurchaseTime);
```

## 9. NBT 持久化

目标：所有 item stack 数据必须可稳定往返。

最低要求：

- NBT 序列化和反序列化集中到一个类。
- 不要在多个 repository 中手写 `toString()` / `TagParser.parseTag()`。
- 反序列化失败必须返回错误，不应静默变成 `null`。

建议新增：

```java
ItemStackCodec
```

职责：

- `String encode(ItemStack stack, RegistryAccess registryAccess)`
- `ItemStack decode(String data, RegistryAccess registryAccess)`

如果继续使用 SNBT，需要明确：

- 空 NBT 如何表示。
- 解析失败如何处理。
- 是否允许旧数据迁移。

## 10. 缓存策略

### 10.1 缓存不是事实来源

数据库是事实来源。

缓存只用于：

- 快速展示商品列表
- 减少重复查询

### 10.2 不暴露可变 Map

不要提供：

```java
Map<UUID, MarketItem> getCache()
```

建议提供：

```java
Optional<MarketItemView> get(UUID id);
List<MarketItemView> query(MarketQuery query);
void replace(MarketItem item);
void remove(UUID id);
```

### 10.3 更新时机

- 事务成功后更新缓存。
- 事务失败不得更新缓存。
- 启动时从数据库加载缓存。
- 如缓存和数据库不一致，以数据库为准重建缓存。

## 11. 业务流程设计

### 11.1 转账

流程：

1. 主线程解析命令和玩家。
2. 服务层校验金额大于 0。
3. DB executor 中开启事务，读取付款方和收款方余额。
4. 校验付款方余额足够。
5. 扣付款方余额。
6. 加收款方余额。
7. 提交事务。
8. 主线程发送成功消息。

失败时：

- 回滚事务。
- 不发送成功消息。
- 返回明确错误。

### 11.2 购买出售商品

推荐流程：

1. 主线程校验玩家和背包空间，并复制待发放物品数据。
2. DB executor 中开启事务，读取商品和买家余额。
3. 校验商品存在、未过期、在售、库存足够、余额足够、冷却通过。
4. 扣买家余额。
5. 如果不是系统商店，加卖家余额。
6. 扣商品库存。
7. 写购买记录。
8. 提交事务。
9. 回到主线程再次确认背包可接收物品并发放物品。
10. 更新缓存和客户端状态。

注意：

- Minecraft 物品栏和数据库不是同一个事务系统。
- DB 事务失败时不得发放物品。
- 如果事务成功后背包状态变化导致无法放入物品，应把物品掉落给玩家或执行一次退款补偿。

### 11.3 上架商品

推荐流程：

1. 主线程校验 slot、数量、价格、权限。
2. 复制待上架 ItemStack 数据。
3. DB executor 中开启事务，创建 market_item。
4. 事务成功后，回到主线程再次确认原 slot 仍可扣减。
5. 扣减玩家物品栏。
6. 更新缓存。

注意：

- DB 插入失败时不得扣玩家物品。
- 如果事务成功后玩家物品栏已变化，必须删除刚创建的 market_item 或返回失败并补偿。

### 11.4 商品管理

所有管理动作必须走同一个服务入口：

```java
MarketService.manageItem(player, request)
```

动作包括：

- 补货
- 取出
- 改价
- 下架
- 切换系统商店
- 切换无限库存
- 设置冷却

每个动作都必须：

- 校验所有权或 OP 权限。
- 校验商品存在。
- 在事务中修改数据库。
- 事务成功后更新缓存。
- 事务失败时不修改缓存。

## 12. 物品栏与数据库一致性

Minecraft 玩家物品栏和 H2 数据库不是同一个事务系统。

这意味着：

- 数据库事务不能自动回滚玩家物品栏变更。
- 玩家物品栏保存也不能自动回滚数据库事务。
- 服务器崩溃时，仍可能存在“DB 已提交但物品栏未保存”或“物品栏已改但 DB 未提交”的小窗口。

### 12.1 本项目采用的轻量策略

本 MOD 是玩法附加，不追求复杂的崩溃强恢复。重构目标是消除当前最严重风险：DB 写失败时不应继续发送成功消息、修改缓存或修改玩家物品栏。

要求：

- DB 失败不发放物品。
- DB 失败不扣玩家物品。
- DB 失败不更新缓存。
- 事务成功后才向客户端报告成功。
- 事务成功后修改物品栏前必须再次校验玩家和 slot 状态。
- 物品栏最终操作失败时必须有简单补偿，例如退款、删除新商品或掉落物品。

这个策略不保证服务器崩溃强一致，但符合本项目的复杂度目标。

## 13. 结果对象

服务层不要直接发网络包。

当前实现为普通泛型结果类 `ServiceResult<T>`，包含 `success`、`value`、`messageKey` 和 `messageArgs`。未来如果需要更强类型约束，可迁移为
sealed 结果对象，例如：

```java
public sealed interface ServiceResult<T> {
    record Success<T>(T value) implements ServiceResult<T> {}
    record Failure<T>(String translationKey, Object... args) implements ServiceResult<T> {}
}
```

Packet handler 负责检查 `result.isSuccess()`，并把失败结果中的 `messageKey` / `messageArgs` 转成
`OperationResultS2CPacket`。

这样可以让服务层被命令、网络和测试复用。

## 14. 错误处理

规则：

- SQL 异常不能被表层吞掉。
- 业务失败返回 `ServiceResult.failure(...)`。
- 系统失败记录 error，并返回通用失败消息。
- 成功消息只能在事务成功后发送。
- 不要把“玩家不存在”和“数据库异常”都当作余额 0。

## 15. 迁移状态与后续步骤

已完成：

- 新增 `TransactionManager`、repository 包、service 包和 `ServiceResult`。
- SQL 失败向上传播，不再由旧表类吞掉。
- 删除旧 `database.tables` 静态表类。
- `/balance`、`/pay`、`/account` 已改用 `EconomyService`。
- 玩家登录创建账户记录已改用 `EconomyService.ensurePlayerRecord`。
- 当前已有购买、上架、管理和列表请求 packet 已改用 `MarketService`。
- 事务成功后才更新 `MarketItemCache`。
- `MarketItemCache` 不再暴露可变 Map，也不再提供全量 `saveAllToDatabase` 旧接口。
- `EconomyService` 和 `MarketService` 已在主要金额入口使用 `Double.isFinite(...)`，拒绝 `NaN`、`Infinity` 和非法金额。

后续按顺序推进：

### Phase E: double 货币使用约束（已落地，持续约束）

1. 保持 Java `double` 和数据库 `DOUBLE`。
2. 所有新增金额入口继续在 service 层校验 `Double.isFinite(value)`。
3. 价格、转账、增加和扣除金额必须 `> 0`。
4. 设置余额允许 `0`，但必须拒绝负数和非有限数。
5. 玩家可见金额继续统一格式化到固定小数位。

验收：

- `NaN` / `Infinity` 不会进入数据库。
- 非正价格和非正交易金额被拒绝。
- 玩家不会看到浮点误差长尾字符串。

### Phase F: NBT codec

1. 新增集中式 `ItemStackCodec` 或等价类。
2. repository 不再直接手写 `CompoundTag.toString()` / `TagParser.parseTag()`。
3. 明确空 NBT、解析失败和旧数据迁移策略。

验收：

- ItemStack 数据可稳定往返。
- 反序列化失败返回明确错误。

### Phase G: GUI 接入与查询补全

1. 保持已完成的市场主页真实渲染、筛选、搜索和分页。
2. 完整上架表单、商品详情/管理界面已接入现有 payload；后续仅补收购模式、状态保留和必要的实时刷新。
3. 只按真实 GUI 需求补充 repository/service 查询方法。
4. 将通用物品选择子界面的选择结果接入具体父流程，而不是在选择界面中硬编码业务。
5. 物品选择子界面作为父级表单字段编辑器使用，由父流程通过 `ItemSelectionRequest` 指定 `purpose`、固定 `mode`、
   `initialSelection`、`baselineAmount`、`lockedStackTemplate`、`allowEmpty` 和确认回调。
6. 出售订单上架使用 `LIST_SELL + TRANSFER`，收购订单上架使用 `LIST_BUY + SAMPLE`，补货/编辑库存使用
   `EDIT_LISTING_STOCK + TRANSFER`。
7. 补货/编辑库存必须锁定原商品模板；目标出售单仍有未售完库存时，应以剩余库存 `ItemStack` 预填选择槽，且允许用户清空选择槽表示编辑后库存为
   0 或准备取回全部剩余库存。

验收：

- GUI 不直接写数据库或缓存。
- 新增查询只覆盖已接入界面的真实需求。
- 物品选择子界面可被不同父菜单复用，并由父流程决定用途、固定模式、预填内容、锁定模板和如何消费 `ItemSelectionResult`。
- 选择界面只负责返回 count=1 的模板 `selectedStack`、`totalAmount`、`baselineAmount`、`deltaAmount`
  和空状态；最终上架、补货、取回或库存编辑仍由父流程转换为业务 packet 并在服务端校验。父流程不得把聚合数量写入需要
  `ItemStack.save(...)` 的 `ItemStack.count`。

## 16. 测试与验证

每个 Phase 完成后至少运行：

```text
WheatMarket [build]
```

优先使用 IDEA MCP run configuration。

基础数据层可在游戏内由 OP 执行：

```text
/wmtest data
```

建议补充测试场景：

- 玩家加入后创建余额记录。
- `/pay` 余额不足。
- `/pay` 正常转账。
- 购买不存在商品。
- 购买库存不足。
- 购买余额不足。
- 购买成功后余额、库存、购买记录一致。
- 上架成功后库存和 DB 一致。
- DB 写入失败时不发送成功消息。
- DB 操作不会阻塞 Minecraft 主线程。

## 17. AI Agent 执行规则

- 开始前阅读本文和 `.opencode/skills/wheatmarket-workflow/SKILL.md`。
- 任何用户说法都需要用代码、配置、工具输出或官方文档核对。
- 不要一次性重写全部数据层。
- 不要为未实现的需求预创建数据操作方法、表、列或服务接口。
- 不要让 packet handler 继续直接写表。
- 不要在 Minecraft 主线程执行数据库写入。
- 不要吞 SQL 异常。
- 不要新增世界保存才落库的设计。
- 如果涉及旧数据库兼容，先询问用户是否需要迁移。
- 每个阶段结束必须说明验证结果和剩余风险。

## 18. 推荐最终状态

最终代码应满足：

- Packet/Command 只调用服务层。
- 服务层是唯一业务入口。
- Repository 是唯一 SQL 入口。
- 每个经济操作都有明确事务边界。
- 数据库写入都在 DB executor 中执行。
- 缓存只在事务成功后更新。
- 货币继续使用 `double` 时，所有入口都拒绝非有限数和非法金额。
- SQL 失败不会被静默忽略。
- 世界保存不是经济数据的主要持久化机制。
