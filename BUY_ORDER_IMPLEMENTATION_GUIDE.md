# 收购单实现开发指南

本文用于指导 WheatMarket 后续的收购单开发，目标是让收购单从“可显示、可上架基础记录”推进到“可安全成交、可托管、可领取、可维护”。

## 1. 目标

当前项目里，出售单的主流程已经基本可用，但收购单仍处于半完成状态：

- 主界面可以显示和筛选收购单
- 上架界面可以创建 `ifSell=false` 的基础记录
- 购买确认界面遇到收购单仍会提示暂不支持成交
- 成交后的物品托管、领取、退款、过期规则仍未落地

本指南的目标是把收购单拆成可逐步完成的阶段，并尽量降低返工风险。

## 2. 核心原则

1. 收购单成交后，不要直接把物品发给收购单发布者。
2. 收购单和出售单必须明确区分为两套业务流程，不能复用同一套“买家拿物品”的语义。
3. 服务层仍然是唯一业务入口；客户端只负责发请求和展示结果。
4. 客户端传来的任意 `itemNBTCompound` 不能作为权威数据，必须以服务端看到的真实物品为准。
5. 先做最小可用闭环：能成交、能托管、能领取，再做体验优化。

## 3. 当前代码状态摘要

当前已经具备的基础：

- `ifSell` 已能区分出售单和收购单
- 主界面已能筛选和展示收购单
- 上架界面已能提交基础收购单记录
- 通用物品选择子界面已可复用
- 市场列表、编辑锁、库存编辑等基础设施已具备

当前明确缺失的能力：

- 收购单供货成交 packet / service
- 成交后托管仓库
- 托管物品领取 service / GUI
- 收购单资金预留或冻结机制
- 收购单取消、过期、退款规则

## 4. 推荐开发顺序

建议严格按以下顺序推进：

1. 托管仓库后端
2. 领取后端
3. 收购单成交后端
4. 领取界面
5. 收购单供货界面
6. 规则补完与体验收尾

原因：收购单真正的前置条件不是按钮，而是“成交后的物品有安全落点”。如果没有托管和领取能力，就不应该把收购单成交正式开放给玩家。

## 5. 第一阶段：托管仓库后端

### 5.1 目标

让收购单成交后的物品有可靠的数据库存储位置。

### 5.2 新增数据表

建议新表名使用 `delivery` 或 `market_delivery`。为减少和现有表混淆，推荐使用 `market_delivery`。

建议字段：

- `deliveryID VARCHAR(36) PRIMARY KEY`
- `receiverID VARCHAR(36) NOT NULL`
- `marketItemID VARCHAR(36)`
- `sourcePlayerID VARCHAR(36)`
- `itemID VARCHAR(255) NOT NULL`
- `itemNBTCompound CLOB`
- `amount INT NOT NULL`
- `remainingAmount INT NOT NULL`
- `createdTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `claimedTime TIMESTAMP`

第一版建议使用 `remainingAmount`，因为它天然支持后续的“部分领取”或“失败保留剩余”。

### 5.3 建议新增文件

- `src/main/java/top/rookiestwo/wheatmarket/database/entities/DeliveryItem.java`
- `src/main/java/top/rookiestwo/wheatmarket/database/repository/DeliveryItemRepository.java`

### 5.4 Repository 第一版建议方法

- `createTable(Connection connection)`
- `insert(Connection connection, DeliveryItem item)`
- `findById(Connection connection, UUID deliveryId)`
- `findByReceiver(Connection connection, UUID receiverId)`
- `updateRemainingAmount(Connection connection, UUID deliveryId, int remainingAmount)`
- `delete(Connection connection, UUID deliveryId)`

### 5.5 数据库初始化接入

在 `WheatMarketDatabase.java` 中：

- 注册 `DeliveryItemRepository`
- 在建表阶段调用 `createTable(...)`
- 后续将其注入 `DeliveryService`

### 5.6 本阶段验收标准

1. 项目启动时能自动建表。
2. 可以插入托管记录。
3. 可以按玩家查询托管记录。
4. 可以减少剩余数量或删除记录。
5. 失败时不吞异常。

## 6. 第二阶段：领取后端

### 6.1 目标

让收购单发布者可以安全地从数据库托管中取回物品。

### 6.2 建议新增服务

建议新增独立服务，而不是把托管逻辑全部塞进 `MarketService`：

- `src/main/java/top/rookiestwo/wheatmarket/service/DeliveryService.java`

### 6.3 第一版建议方法

- `listDeliveries(UUID receiverId)`
- `claimDelivery(UUID receiverId, UUID deliveryId)`

`claimDelivery(...)` 返回值建议至少包含：

- 领取物品的 `itemNbt`
- 本次领取的 `amount`
- 领取后是否删除整条记录
- 领取后的 `remainingAmount`

### 6.4 必做校验

1. 托管记录存在。
2. 领取人等于 `receiverID`。
3. `remainingAmount > 0`。
4. 物品 NBT 可反序列化。
5. 数据库事务内正确更新 `remainingAmount` 或删除记录。

### 6.5 背包空间策略

第一版建议采用最稳妥的规则：

- 如果背包放不下，则直接拒绝领取
- 不修改数据库记录
- 返回明确失败消息

先不要做“自动部分领取”，因为那会引入更复杂的容量计算、拆分逻辑和界面文案。

### 6.6 本阶段验收标准

1. 玩家只能领取自己的托管记录。
2. 背包满时记录保持不变。
3. 领取成功时数据库数量正确减少或删除。
4. 服务失败不会错误发放物品。
5. 不出现“记录已减但物品没发”或“物品发了但记录没减”的半成功状态。

## 7. 第三阶段：收购单成交后端

### 7.1 目标

让其他玩家可以向收购单供货，并获得货币。

### 7.2 不要复用 `BuyItemC2SPacket`

建议新增专用 packet，例如：

- `network/c2s/FulfillBuyOrderC2SPacket.java`

原因：

- 出售单语义是“我付钱，拿物”
- 收购单语义是“我交物，拿钱”

两者方向相反，强行复用只会让服务和 UI 越来越混乱。

### 7.3 Packet 最小参数建议

- `marketItemID`
- `amount`

如果通过菜单受控槽位交付物品，则不需要让客户端直接上传任意 NBT；服务端最终应以菜单中的真实物品为准。

### 7.4 `MarketService` 建议新增方法

建议新增：

- `fulfillBuyOrder(UUID supplierId, UUID marketItemId, int amount, ...)`

### 7.5 服务层必须完成的校验

1. 商品存在。
2. 商品是收购单，即 `ifSell == false`。
3. 商品未过期。
4. 商品未被编辑锁锁住。
5. 供货数量大于 0。
6. 供货数量不超过收购单剩余需求量。
7. 供货物品与收购样品完全一致。
8. 收购单发布者资金足够，或者预留资金足够。
9. 冷却/限购规则是否允许本次成交。

### 7.6 物品一致性校验

必须以服务端可见的真实物品为准，使用类似：

- `ItemStack.isSameItemSameComponents(...)`

不要信任客户端直接发送的 `itemNBTCompound`。

### 7.7 事务内应完成的数据库操作

1. 扣减或校验收购方资金。
2. 增加供货方余额。
3. 减少收购单剩余需求量。
4. 写入托管仓库记录。
5. 写入 `purchase_record` 或等价成交记录。
6. 更新 `lastTradeTime`。
7. 如果需求量减为 0，则删除收购单。

### 7.8 真实物品扣除与补偿

建议继续沿用当前项目的总体模式：

- 先由 service 事务判断数据库层面是否成功
- 再在主线程扣真实物品
- 如果扣物失败，则执行补偿回滚

由于收购单的补偿涉及更多状态，至少要能回滚：

- 托管记录
- 收购单剩余需求量
- 供货者余额增加
- 收购方资金扣减或冻结状态
- 本次成交记录

如果实现时发现这套补偿风险过高，建议尽量把供货物品放进受控菜单槽位中，再由最终 packet 统一结算，减少“事务成功但主线程扣物失败”的窗口。

### 7.9 本阶段验收标准

1. 玩家只能向收购单供货，不能误走出售单逻辑。
2. 提供错误物品会被拒绝。
3. 成交成功后供货者获得货币。
4. 成交成功后物品进入托管仓库。
5. 收购单需求量正确减少。
6. 扣物失败时能完整补偿。
7. 不会把物品直接发给离线收购者。

## 8. 第四阶段：最小可用领取入口

### 8.1 目标

为托管后端提供一个玩家可用入口。

### 8.2 入口形式建议

第一版直接做简单 GUI，不必等待“完美仓库系统”。

最小能力：

- 展示托管记录
- 点击领取
- 显示失败原因
- 成功后刷新

### 8.3 建议新增网络包

- `RequestDeliveryListC2SPacket`
- `DeliveryListS2CPacket`
- `ClaimDeliveryC2SPacket`

### 8.4 托管列表最小展示字段

- 物品图标
- 物品名称
- 数量
- 来源玩家或来源订单
- 创建时间

### 8.5 本阶段验收标准

1. 玩家能看到自己的托管记录。
2. 玩家能点击领取。
3. 成功后列表刷新。
4. 失败时记录保留且显示原因。

## 9. 第五阶段：收购单供货界面

### 9.1 目标

让收购单从“显示但不可成交”变成真正可成交。

### 9.2 当前入口位置

当前收购单详情入口在 `WheatMarketOrderConfirmationUI` 中，但对收购单仍只会提示不支持。

后续应改为：

- 打开供货流程
- 选择要交付的物品
- 确认供货数量
- 发送专用收购成交 packet

### 9.3 推荐交互

建议复用现有物品选择子界面：

1. 玩家点开收购单。
2. 进入供货确认页。
3. 点击“选择供货物品”。
4. 打开物品选择子界面。
5. 子界面锁定为收购样品模板。
6. 玩家放入同种物品。
7. 确认后返回供货确认页。
8. 最终发送 `FulfillBuyOrderC2SPacket`。

### 9.4 关键约束

1. 必须传入 `lockedStackTemplate`。
2. 只能放入与收购样品完全一致的物品。
3. 必须保留真实物品数量，不能只传模板。
4. 当前“自己商品 = 编辑商品”的逻辑要保留。

### 9.5 第一版建议展示字段

- 收购者
- 单价
- 剩余需求量
- 本次供货量
- 预计获得金额
- 样品物品
- 冷却或限制提示

### 9.6 本阶段验收标准

1. 玩家打开收购单时不再只看到“不支持”。
2. 只能放入符合样品的物品。
3. 确认后走专用收购成交 packet。
4. 成功后余额、托管、需求量一起正确变化。

## 10. 第六阶段：规则补完

### 10.1 资金策略

这是收购单设计里最关键的规则之一，建议尽快定死。

可选方案：

1. 上架时冻结全额资金。
2. 成交时实时检查余额。

更推荐方案 1，因为：

- 逻辑更稳定
- 不会出现“挂了单但临时没钱”的失败体验
- 更接近真实市场买单逻辑

### 10.2 过期和取消规则

建议：

- 未成交部分退回冻结资金
- 已成交但未领取的物品继续保留在托管仓库

### 10.3 冷却/限购规则

建议第一版就与出售单保持统一字段语义，避免 UI 和服务规则分裂。

### 10.4 系统商店收购单

第一版建议不要完全开放，或者只允许 OP 使用；否则货币来源和权限边界会明显更复杂。

## 11. 第七阶段：体验收尾

### 11.1 语言键

需要新增或补全：

- 收购单供货文案
- 托管列表文案
- 领取成功/失败文案
- 资金不足文案
- 样品不匹配文案
- 需求已完成文案
- 背包空间不足文案

### 11.2 刷新与状态保留

需要补的体验项：

- 成交后刷新市场列表
- 领取后刷新托管列表
- 刷新余额
- 从详情或供货流程返回时保留筛选、分页、搜索状态

### 11.3 日志

建议补充明确日志的场景：

- 收购成交失败
- 托管写入失败
- 领取失败
- 补偿回滚失败

### 11.4 并发与防重

建议补充保护：

- 避免双击提交
- 避免重复领取
- 避免同一收购单并发成交超卖

## 12. 推荐新增文件清单

### 12.1 数据层

- `database/entities/DeliveryItem.java`
- `database/repository/DeliveryItemRepository.java`

### 12.2 服务层

- `service/DeliveryService.java`
- `service/MarketService.java` 扩展收购成交方法

### 12.3 网络层

- `network/c2s/FulfillBuyOrderC2SPacket.java`
- `network/c2s/RequestDeliveryListC2SPacket.java`
- `network/c2s/ClaimDeliveryC2SPacket.java`
- `network/s2c/DeliveryListS2CPacket.java`

### 12.4 客户端 GUI

- 扩展 `client/gui/WheatMarketOrderConfirmationUI.java`
- 扩展 `client/gui/WheatMarketOrderConfirmationScreen.java`
- 新增托管列表界面
- 新增或扩展供货界面

### 12.5 资源与文本

- `assets/wheatmarket/lang/en_us.json`
- `assets/wheatmarket/lang/zh_cn.json`
- 必要的 XML UI 文件

## 13. 每阶段最关键的验收问题

1. 物品是否始终有唯一归属。
2. 余额是否始终与成交结果一致。
3. 托管记录是否始终可追踪。
4. 失败时是否能恢复，不留半成功状态。
5. 客户端是否永远不能伪造样品或供货物品。
6. 离线收购者是否完全不影响成交安全性。

## 14. 最需要避免的坑

1. 直接复用 `BuyItemC2SPacket`。
2. 直接把物品发给离线收购者。
3. 用客户端传来的任意 NBT 作为权威数据。
4. 没有补偿就先改余额和库存。
5. 没有托管仓库就开放收购成交。
6. 把收购单规则混进出售单旧逻辑里。

## 15. 一句话路线

先做“托管表 + 领取 service”，再做“收购成交 service”，最后接“供货 UI 和领取 UI”。
