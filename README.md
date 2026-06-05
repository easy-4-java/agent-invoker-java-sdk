# agent-invoker-java-sdk

AI Agent Invoker 抽象 SDK — Provider 无关的 AI 智能体调用 Java 库。

## 概述

提供统一抽象，跨多个 Provider（OpenClaw、OpenCode、Hermes 等）调用 AI 智能体。

### 核心接口

- **`AgentInvoker`** — 智能体调用接口：提交任务（`submit`）、取消（`cancel`）、处理回调（`handleCallback`）
- **`AgentInvokerRouter`** — 根据 `providerCode` 和 `defaultProvider` 路由到正确的 Provider 实现
- **`CallbackRouter`** — 将回调路由到正确的 Provider adapter
- **`AgentInvokeCmd`** — 调用命令构建器（taskId、agentId、providerCode、enhancedPrompt、callbackUrl 等）
- **`SubmitResult`** — 提交结果（taskId、providerTaskId、status、message）

### Provider Adapter

| Provider | providerCode | 类 | 依赖 |
|----------|-------------|-----|------|
| OpenClaw | `openclaw` | `OpenClawAgentInvoker` | `openclaw-java-sdk`（可选） |
| OpenCode | `opencode` | `OpenCodeAgentInvoker` | `opencode-java-sdk`（可选） |
| Hermes | `hermes` | `HermesAgentInvoker` | `hermes-java-sdk`（可选） |

每个 Provider adapter 包含三个组件：
- **Invoker** — 实现 `AgentInvoker` 接口，负责提交和取消
- **RequestMapper** — 将 `AgentInvokeCmd` 映射为 Provider 特定请求
- **CallbackParser** — 将 Provider 回调解析为 `CallbackOutcome`

## 架构

```
AgentInvokerRouter
  ├── OpenClawAgentInvoker  (providerCode = "openclaw")
  │     ├── OpenClawInvokeRequestMapper
  │     └── OpenClawCallbackParser
  ├── OpenCodeAgentInvoker  (providerCode = "opencode")
  │     ├── OpenCodePromptRequestMapper
  │     └── OpenCodeCallbackParser
  └── HermesAgentInvoker    (providerCode = "hermes")
        ├── HermesRequestMapper
        └── HermesCallbackParser
```

## 使用

```java
AgentInvokerRouter router = new AgentInvokerRouter();
router.setDefaultProvider("openclaw");
router.register(new OpenClawAgentInvoker(openClawClient, "http://localhost:7088"));
router.register(new OpenCodeAgentInvoker(openCodeClient));
router.register(new HermesAgentInvoker(hermesClient, "http://localhost:7088"));

AgentInvokeCmd cmd = AgentInvokeCmd.builder()
    .taskId("task-001")
    .agentId("main")
    .providerCode("openclaw")
    .enhancedPrompt("Generate content...")
    .callbackUrl("http://localhost:7088/api/generation/save")
    .tenantId("t1")
    .userId("u1")
    .channel("xiaohongshu")
    .variables(Collections.singletonMap("tone", "casual"))
    .build();

AgentInvoker invoker = router.route("openclaw");
SubmitResult result = invoker.submit(cmd);
```

### OpenClaw callback URL

OpenClaw Gateway Hooks（`POST /hooks/agent`）没有专用 `callbackUrl` 字段。adapter 的处理方式：

1. 从 `AgentInvokeCmd.callbackUrl` 解析 URL，否则使用 adapter `callbackBaseUrl`
2. 将 URL 和 `task_id` / metadata 嵌入 agent `message`，供 OpenClaw POST 结果回来

### OpenCode Provider

`OpenCodeAgentInvoker`（`providerCode = "opencode"`）通过 `opencode-java-sdk` 的 `OpenCodeClient` 交互：
- 创建 session 并发送 prompt
- 支持同步/异步调用
- 通过 SSE 事件流获取实时状态

### Hermes Provider

`HermesAgentInvoker`（`providerCode = "hermes"`）通过 `hermes-java-sdk` 的 `HermesClient` 交互：
- 基于 Run 的异步任务执行
- 提交后自动轮询 Run 状态直到完成
- 完成后通过回调 URL POST 结果

### Cancel

- **OpenClaw**：`cancel(taskId)` 跟踪 `taskId → runId`，发送 best-effort `wake` 信号
- **OpenCode**：通过 `OpenCodeClient.abort(sessionId)` 中止会话
- **Hermes**：通过 `HermesClient.stopRun(runId)` 停止 Run

## 配置（Spring Boot）

使用 **`openclaw-spring-boot-starter`** 配置 `OpenClawClient`（`openclaw.*`）。使用 **`agent-invoker-spring-boot-starter`** 配置路由（`agents.provider.*`）：

| Key | 用途 |
|-----|------|
| `agents.provider.default-provider` | Router 回退默认 Provider |
| `agents.provider.openclaw.enabled` | 启用 OpenClaw adapter |
| `agents.provider.openclaw.callback-base-url` | adapter 回调基础 URL（未设置时回退 `openclaw.callback-base-url`） |
| `agents.provider.hermes.enabled` | 启用 Hermes adapter |
| `agents.provider.hermes.callback-base-url` | Hermes 回调基础 URL |
| `agents.provider.hermes.default-instructions` | Hermes 默认 instructions |
| `openclaw.gateway-base-url` | Gateway URL |
| `openclaw.hooks-token` | Webhook 鉴权 |
| `openclaw.callback-base-url` | 未设置 agent-invoker key 时桥接到 adapter |

## 多版本支持

9 个分支对应不同 Spring Boot 线：2.3.x、2.7.x、3.0.x–4.0.x。
每个分支的 pom.xml 由 `scripts/render-branch-pom.py` 生成。
