收购单领取界面需求文档

1. 背景 当前项目已具备收购单成交后的托管后端基础：

market_delivery 托管表
DeliveryService.listDeliveries(...)
DeliveryService.claimDelivery(...)
但玩家侧还没有可用的“领取入口”。本需求文档用于定义一个单独界面，供玩家查看并领取自己的托管物品。

2. 目标 实现一个独立于主市场页的“托管领取界面”，满足以下最小闭环：

查看自己的托管记录
点击领取单条记录
成功后刷新列表
失败时保留记录并显示失败原因

3. 范围 本次界面需求包含：

客户端独立 Screen + UI
托管列表展示
单条领取操作
基础刷新与状态反馈
与现有市场主界面的跳转
本次不包含：

部分领取
批量领取
托管物品排序筛选
托管记录搜索

4. 与当前项目的适配约束 为保持当前项目风格一致，建议沿用现有 UI 组织方式：

WheatMarketMainScreen
WheatMarketHomeUI
WheatMarketOrderConfirmationScreen
WheatMarketOrderConfirmationUI
因此新界面建议采用同样结构：

WheatMarketDeliveryScreen
WheatMarketDeliveryUI
建议资源命名：

src/main/resources/assets/wheatmarket/ui/delivery.xml

5. 用户故事

玩家进入市场主界面后，可以打开“托管领取界面”。
玩家可以看到所有属于自己的托管记录。
玩家点击某条记录的“领取”按钮后，系统发起领取请求。
若领取成功，该记录从列表中移除或刷新数量。
若领取失败，列表保持不变，并显示失败原因。
玩家关闭领取界面后，可以回到市场主界面。

6. 入口设计 建议入口放在主市场页的顶栏或操作栏，作为单独按钮：

按钮tooltip文案建议：取货
英文tooltip建议：Delivery
按钮图标建议：一个箱子或包裹的图标，突出 领取
点击后行为：

打开 WheatMarketDeliveryScreen
不关闭容器菜单，仅切换客户端界面，和当前 MainScreen -> OrderConfirmationScreen 模式一致
返回行为：

关闭领取界面后返回 WheatMarketMainScreen
返回后应尽量保留主市场页原有状态：
当前筛选
搜索词
当前页码
排序方向

7. 界面目标 界面第一版强调“清晰可领”，不追求复杂仓库视觉。

界面应让玩家一眼看清：
这是什么物品(渲染一个可以浮现tooltip的道具图标，和当前挂单页面的那个类似)
数量
来源(收购/下架/过期)
时间
领取失败时为什么失败

8. 页面结构 建议页面分为以下区域：

顶部标题区
标题：货物暂存区

列表区
展示玩家自己的托管记录（分页而非ScrollView）
每条记录一行列表
底部操作区
返回
状态提示区
显示加载中
显示空列表提示
显示领取成功/失败消息

9. 列表字段要求 每条托管记录至少展示以下字段：

物品图标（鼠标hover时需根据物品实际情况渲染Tooltip）
物品名称
数量
来源(收购/下架/过期)
(如来源为收购，hover时添加tooltip渲染来源玩家 sourcePlayerID 对应名称)
领取 按钮

10. 交互规则

打开界面时自动请求一次托管列表。
点击 刷新 时重新请求托管列表。
点击 领取 时：
按钮进入处理中状态
防止重复点击
发出领取请求
领取成功后：
刷新当前列表
显示成功提示
领取失败后：
保留原记录
显示失败原因
恢复按钮可点击状态

11. 后端对接需求 建议沿用开发指南中的网络拆分，新增如下包：

RequestDeliveryListC2SPacket
DeliveryListS2CPacket
ClaimDeliveryC2SPacket
客户端需要的数据结构建议至少包含：

deliveryID
marketItemID
sourcePlayerID
itemNBT
itemID
amount
remainingAmount
createdTime
claimedTime
如果服务端可直接下发来源玩家名，客户端展示会更稳定。否则客户端只能：

在线时尝试解析玩家名
离线时显示 UUID 缩写

12. 状态与反馈 界面必须覆盖以下状态：

加载中
文案：正在加载托管记录...
空列表
文案：暂无可领取的托管物品
领取成功
文案来源于语言键
成功后刷新列表
领取失败
常见失败原因包括：
托管记录不存在
背包空间不足
物品数据无效
网络或服务异常
统一失败文案

不应把界面卡死在处理中状态

13. 排版与视觉建议
    大区域建议使用board贴图来区分（类似主市场页的大分区）
    列表中每列的背景使用paper来区分。
    列表每行需要有间距
    积极使用ldlib2的自动排列来进行排版
    如果有组件需要组成一堆，尽量对组件进行分组后让ldlib2自动排列
    按钮视觉区分明确

14. 可用性要求

桌面分辨率下能稳定显示多条记录。
按钮尺寸足够点击。
文本超长时不应撑坏布局。（文本若超长应滚动显示）
时间、来源玩家名、物品名过长时需要滚动显示。
同步刷新时不应闪退或丢失当前界面。

15. 语言键需求

如有需要添加文本的地方，对语言键进行自动命名
不要硬编码

16. 客户端文件建议 建议新增：

src/main/java/top/rookiestwo/wheatmarket/client/gui/WheatMarketDeliveryScreen.java
src/main/java/top/rookiestwo/wheatmarket/client/gui/WheatMarketDeliveryUI.java
建议新增网络：

src/main/java/top/rookiestwo/wheatmarket/network/c2s/RequestDeliveryListC2SPacket.java
src/main/java/top/rookiestwo/wheatmarket/network/c2s/ClaimDeliveryC2SPacket.java
src/main/java/top/rookiestwo/wheatmarket/network/s2c/DeliveryListS2CPacket.java
建议新增 UI 资源：

src/main/resources/assets/wheatmarket/ui/delivery.xml

17. 非功能要求

不信任客户端本地物品数据作为权威。
所有显示文本必须走语言键。
界面刷新后不应重复累计旧数据。
不允许玩家看到其他人的托管记录。
领取失败时不能错误移除本地记录。

18. 验收标准

玩家可以从市场主界面进入独立托管领取界面。
界面能正确显示当前玩家的托管记录。
每条记录至少展示物品、数量、来源、领取按钮。
点击领取后会向服务端发送领取请求。
领取成功后列表自动刷新。
背包满等失败情况下，记录保留且原因明确。
关闭界面后可回到主市场界面。
不出现他人托管记录串号显示。
语言键完整，不出现硬编码玩家可见文本。

19. 后续扩展预留 本界面后续可扩展：

全部领取