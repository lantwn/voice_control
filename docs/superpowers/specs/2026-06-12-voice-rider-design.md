# Voice Rider — 语音控制外卖骑手 APP 设计文档

> **日期**: 2026-06-12
> **状态**: 设计完成，待审核
> **目标**: 为美团外卖骑手开发一款基于语音控制的辅助操作 Android APP

---

## 1. 项目概述

### 1.1 目标用户
美团外卖配送骑手。骑手在骑行过程中双手不便操作手机，语音控制能显著提升操作效率和安全性。

### 1.2 核心能力
- 通过语音命令控制美团骑手 APP 完成抢单、取餐、送达等核心操作
- 内置导航能力
- AI 智能提醒
- 蓝牙耳机唤醒词 + 按键双触发

### 1.3 技术路线
- **平台对接**: Android AccessibilityService UI 自动化操作美团骑手 APP（无需美团 API 授权）
- **语音引擎**: 讯飞 SDK 为主（含离线唤醒词），Android SpeechRecognizer 为降级
- **导航**: 高德地图 SDK 内置导航（骑行优先）
- **架构**: 纯本地 APP，无后端服务

---

## 2. 技术栈

| 类别 | 选择 | 说明 |
|------|------|------|
| 语言 | Kotlin | Android 官方推荐 |
| UI | XML View | 对 AccessibilityService 等系统级 API 兼容性最好 |
| 最低 SDK | API 29 (Android 10) | 覆盖约 75% 活跃设备 |
| 语音 | 讯飞语音 SDK + Android SpeechRecognizer | 主备模式 |
| 地图 | 高德地图 SDK | 个人免费 |
| 构建 | Gradle + Kotlin DSL | 多模块管理 |

---

## 3. 模块架构

```
VoiceRider/
├── :core/           基础层 — 数据模型、工具类、配置
├── :voice/          语音层 — 讯飞 SDK 封装、唤醒词、TTS
├── :accessibility/  自动化层 — AccessibilityService、美团 UI 操作
├── :navigation/     导航层 — 高德 SDK、路线规划
└── :app/            编排层 — UI、Service 协调、悬浮窗
```

### 3.1 模块依赖规则
- `:app` 依赖所有子模块
- 所有子模块依赖 `:core`
- 子模块之间互不直接依赖，通过 `:app` 中的 Service 协调通信

### 3.2 模块职责

**`:core`** — 数据模型与基础设施
- 包路径: `com.voicerider.core`
- 核心类: `Order`, `VoiceCommand`, `AirReminder`, `RouteInfo`, `PrefsManager`, `Logger`, `AddressParser`

**`:voice`** — 语音引擎
- 包路径: `com.voicerider.voice`
- 核心类: `WakeUpEngine`（讯飞离线唤醒词，始终可用）, `CommandRecognizer`（讯飞在线优先 → 系统降级）, `TtsSpeaker`（TTS 语音反馈）
- 降级策略: 仅命令识别环节降级到 Android SpeechRecognizer，唤醒词不受影响

**`:accessibility`** — 美团 APP 自动化
- 包路径: `com.voicerider.accessibility`
- 核心类: `RiderAccessibilityService`（系统级无障碍服务）, `MeituanAutomator`（UI 树遍历与操作）, `OrderActionHandler`（命令→操作映射+状态校验）
- 元素定位三层降级: Resource ID → Text 文本 → Content Description

**`:navigation`** — 地图导航
- 包路径: `com.voicerider.navigation`
- 核心类: `AmapNavigator`（导航生命周期管理）, `RoutePlanner`（地理编码+算路）
- 默认骑行模式，支持驾车切换

**`:app`** — 编排与 UI
- 包路径: `com.voicerider.app`
- 页面: 首页（订单列表+AI提醒+语音栏）、订单详情页、设置页
- Service: `VoiceRoutingService`（前台，语音生命周期）、`FloatingWindowService`（前台，悬浮窗）
- 样式: 支持深色/浅色双主题

---

## 4. 主界面设计

### 4.1 首页布局（三区制·简约风格）

**整体气质**: 米白暖底（`#f8f6f2`），大留白、轻柔阴影、纤细分割线。信息密度低但不丢失核心内容。

**布局结构**:

| 区域 | 位置 | 占比 | 内容 |
|------|------|------|------|
| 顶栏 | 顶部 | 全宽 | 大字轻字重标题 + 服务状态圆点 + 今日收入（一行副文字） |
| 订单列表 | 左上 | 55% 宽度 | 大圆角(12dp)白卡片，金额/距离大字 + 送达地址 + 状态标签 |
| AI 智能提醒 | 右上 | 45% 宽度 | 圆点分色级 + 纤细分割线 + 宽松行距 + 今日统计（已完成/进行中/收入） |
| 语音输入栏 | 底部 | 全宽 | 深色胶囊（`#2c2c2c`），白色麦克风圆按钮 + 提示文字 + 发送 |

**订单卡片细节**:
- 白底卡片，圆角 12dp，轻柔阴影 (elevation 1dp)
- 标题行：商家名 + 分店名 + 状态标签（待取餐=暖黄底 / 配送中=蓝底）
- 信息行：金额(17sp) + 距离(17sp)，两列并排
- 分割线后：送达地址一行
- 点击卡片 → SharedElement 过渡到订单详情页

**AI 提醒区细节**:
- 白底卡片，圆角 12dp
- 每条提醒：6dp 彩色圆点（🔴紧急 🟡重要 🔵信息） + 订单号 + 内容摘要 + 时间
- 条目间用极细分割线（`#f8f8f8`）
- 下方附今日统计：已完成(绿) / 进行中(黄) / 收入，三个数字

**语音输入栏细节**:
- 深色胶囊形（`#2c2c2c`），圆角 22dp
- 左侧白色圆形麦克风按钮 (34dp)，右侧发送图标
- 中间 placeholder 提示文字

### 4.2 订单详情页
点击订单卡片进入，展示：完整订单信息（商家/地址/顾客/金额/时间）、四个操作按钮（导航取餐/联系顾客/确认取餐/发消息）、该订单专属 AI 提示、底部语音栏

### 4.3 悬浮窗
- 圆形 56dp，带 8dp elevation + 扩散光晕
- 四种状态: 空闲（灰）→ 监听中（紫+脉冲动画）→ 成功（绿+弹跳）→ 失败（红+摇晃）
- 可拖动到任意位置，轻触触发语音，长按回主界面
- 仅在美团 APP 前台时显示

---

## 5. 语音交互设计

### 5.1 激活方式
- **唤醒词**: "美团精灵"（讯飞离线 SDK，始终可用）
- **蓝牙按键**: 耳机通话键触发
- **悬浮窗轻触**: 手动触发
- **语音栏麦克风**: 应用内点击

### 5.2 处理流水线
```
音频输入 → 唤醒词检测(讯飞离线) → 等待命令(5秒窗口) → 命令识别(讯飞在线→系统降级) → 命令匹配(Pattern Match) → 状态校验 → 执行动作 → TTS反馈
```

**降级行为**: 仅命令识别环节降级到 Android SpeechRecognizer，唤醒词始终由讯飞离线 SDK 提供，不受降级影响。

### 5.3 v1 命令集

| 场景 | 命令示例 | 动作 | 前置状态 |
|------|----------|------|----------|
| 抢单 | "接单""抢" | 点击美团抢单按钮 | 抢单大厅 |
| 拒单 | "不接""拒单" | 点击拒单 | 抢单大厅 |
| 取餐 | "已取餐""取货完成" | 点击确认取餐 | 已接单 |
| 送达 | "已送达""送到了" | 点击确认送达 | 配送中 |
| 导航取餐 | "导航到取餐点" | 提取商家地址→启动导航 | 已接单 |
| 导航送餐 | "导航到顾客" | 提取顾客地址→启动导航 | 已取餐 |
| 联系顾客 | "打电话给顾客" | 拨号 | 任意 |
| 发消息 | "发消息给顾客" | 打开短信 | 任意 |
| 查询 | "查看订单详情""还剩几单" | TTS 播报 | 任意 |

### 5.4 错误处理
- 唤醒后 5 秒无输入 → TTS "请再说一次"
- 命令匹配失败 → TTS "未识别的指令"
- 当前状态不允许该操作 → TTS 说明原因
- Accessibility 操作失败（元素未找到 3 次）→ TTS 提示手动操作
- 导航失败 → TTS "无法规划路线"，回退订单详情页

---

## 6. AI 智能提醒

### 6.1 提醒来源
- 订单状态变更（已出餐、即将超时）
- 顾客消息/催单/备注
- 路线/地址变更
- 收入与统计汇总

### 6.2 提醒级别
- 🔴 **紧急**: 催单、即将超时 → 入场动画 + 2 次触觉震动
- 🟡 **重要**: 状态变更、取餐提醒
- 🔵 **信息**: 路线建议、系统通知
- 🟢 **汇总**: 收入统计

### 6.3 入场动效
从右侧滑入 (translateX 30dp → 0, 200ms)，紧急级别额外添加 haptic feedback

---

## 7. 视觉设计规范

### 7.1 色彩
- **品牌主色**: 紫蓝渐变 `#667eea → #764ba2`
- **语义色**: 成功 `#38a169` / 警告 `#d69e2e` / 紧急 `#e53e3e` / 信息 `#3182ce`
- **背景**: 支持深色 `#0f0f23` 和浅色 `#f8f9fa` 双主题

### 7.2 字体
- 中文: PingFang SC（系统默认中文字体）
- 数字/金额: DIN Alternate Bold
- 代码/命令: JetBrains Mono

### 7.3 组件规格
| 组件 | 圆角 | 阴影 | 尺寸/间距 |
|------|------|------|-----------|
| 订单卡片 | 12dp | elevation 2dp | 内边距 12dp，间距 8dp |
| AI 提醒卡片 | 8dp | elevation 1dp | 内边距 8dp，间距 6dp |
| 语音栏 | 24dp (胶囊) | elevation 4dp | 高 48dp |
| 悬浮窗 | 28dp (圆形) | elevation 8dp+光晕 | 56×56dp |
| 操作按钮 | 8dp | elevation 2dp | 内边距 10×16dp |

### 7.4 关键动效
- **悬浮窗脉冲**: 监听中 1.2s scale 循环 (1→1.12→1)，外层扩散光圈同步透明度
- **订单卡片状态切换**: 左边框颜色 300ms 过渡，卡片高度 250ms ease-out
- **AI 提醒入场**: 右侧滑入 200ms，紧急级别+震动
- **语音栏焦点**: 命令面板展开 200ms，麦克风图标 60ms 颜色渐变
- **操作反馈**: 成功弹跳 (1→1.3→1, 400ms overshoot) / 失败摇晃 (3 次 150ms)
- **页面转场**: 订单卡片 SharedElement 过渡到详情页，后退 slide in from left

---

## 8. 数据模型（定义在 `:core`）

```kotlin
// 语音命令
data class VoiceCommand(
    val type: CommandType,
    val rawText: String,
    val confidence: Float,
    val timestamp: Long
)

// 订单
data class Order(
    val id: String,
    val status: OrderStatus,
    val merchantName: String,
    val merchantAddress: String,
    val customerName: String,
    val customerAddress: String,
    val customerPhone: String,
    val amount: Double,
    val acceptedTime: Long,
    val estimatedDelivery: Long
)

// AI 提醒
data class AirReminder(
    val type: ReminderType,
    val level: ReminderLevel,  // URGENT, IMPORTANT, INFO, SUMMARY
    val message: String,
    val orderId: String?,
    val timestamp: Long
)

// 路线信息
data class RouteInfo(
    val from: LatLng,
    val to: LatLng,
    val distance: Float,   // 米
    val duration: Int,     // 秒
    val polyline: String   // 高德 polyline 编码
)
```

---

## 9. 数据流

以"已取餐"命令为例：

1. 骑手说"美团精灵，已取餐" → 蓝牙耳机采集音频
2. `WakeUpEngine` 检测到唤醒词 → 开启 5 秒命令窗口
3. `CommandRecognizer` 识别为"已取餐" → 匹配 `CommandType.PICKUP_DONE`
4. `VoiceRoutingService` 收到命令 → 校验订单状态为"待取餐" → 转发
5. `MeituanAutomator` 三层定位找到"确认取餐"按钮 → 执行点击
6. 订单状态更新为"配送中" → TTS "已确认取餐，开始导航到顾客"
7. `:app` 自动触发送餐导航建议

---

## 10. 测试策略

| 层级 | 范围 | 工具 | 目标 |
|------|------|------|------|
| 单元测试 | 命令匹配、状态机、地址解析、AI 规则 | JUnit 5 + MockK | `:core` + 各模块业务逻辑 |
| 集成测试 | 语音→路由、Accessibility→UI 操作、导航算路 | Android Instrumentation | 模块协作 |
| 手动验收 | 端到端语音控制、美团 APP 真实交互 | 人工 + 日志 | 完整配送流程 |

---

## 11. 风险与应对

| 风险 | 应对 |
|------|------|
| 美团 APP 更新导致元素定位失效 | 三层降级定位 + 元素映射配置文件热更新 |
| AccessibilityService 被系统杀死 | 前台 Service 保活 + 定时自启检测 |
| 不同手机厂商 ROM 对无障碍限制不同 | 引导用户开启权限，维护厂商兼容清单 |
| 讯飞 SDK 配额耗尽 | 自动降级到系统 SpeechRecognizer（唤醒词仍可用） |
| 室外噪音影响识别准确率 | 讯飞离线命令词 + 蓝牙耳机近场收音 |

---

## 12. 开放问题

- 美团骑手 APP 各版本的 resource-id 需要通过反编译/UI Automator Viewer 实测获取
- 讯飞 SDK 具体 AppID 和唤醒词需要注册后配置
- 高德地图 SDK Key 需要申请
