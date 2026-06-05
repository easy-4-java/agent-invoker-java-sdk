package io.github.hiwepy.agent.invoker.opencode;

import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.agent.invoker.AgentInvoker;
import io.github.hiwepy.agent.invoker.CallbackOutcome;
import io.github.hiwepy.agent.invoker.RawCallbackPayload;
import io.github.hiwepy.agent.invoker.SubmitResult;
import io.github.hiwepy.opencode.OpenCodeClient;
import io.github.hiwepy.opencode.model.PromptRequest;
import io.github.hiwepy.opencode.model.PromptResult;
import io.github.hiwepy.opencode.model.Session;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * OpenCode 适配器：实现 {@link AgentInvoker}，将业务语义翻译为 OpenCode Server REST API 调用。
 *
 * <p>依赖 {@code opencode-java-sdk}（optional），仅在类路径存在 OpenCodeClient 时可用。</p>
 *
 * <p>与 OpenClawAgentInvoker 的关键差异：</p>
 * <ul>
 *     <li>OpenClaw 使用 webhook + 异步回调模式</li>
 *     <li>OpenCode 使用 session.prompt 同步等待模式（或 promptAsync + 轮询）</li>
 *     <li>回调数据从 AI 响应文本中解析，而非 HTTP webhook</li>
 * </ul>
 *
 * @author wandl
 * @since 1.0.0
 */
@Slf4j
public class OpenCodeAgentInvoker implements AgentInvoker {

    public static final String PROVIDER_CODE = "opencode";

    private final OpenCodeClient openCodeClient;
    private final OpenCodeCallbackParser callbackParser;
    private final String defaultAgent;
    private final String defaultModel;
    private final ConcurrentMap<String, String> taskIdToSessionId = new ConcurrentHashMap<>();

    /**
     * @param openCodeClient OpenCode SDK 客户端
     * @param defaultAgent   默认 agent（如 "build"）
     * @param defaultModel   默认模型（如 "anthropic/claude-sonnet-4-5"）
     */
    public OpenCodeAgentInvoker(OpenCodeClient openCodeClient,
                                  String defaultAgent,
                                  String defaultModel) {
        this.openCodeClient = java.util.Objects.requireNonNull(openCodeClient, "openCodeClient");
        this.defaultAgent = defaultAgent;
        this.defaultModel = defaultModel;
        this.callbackParser = new OpenCodeCallbackParser();
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SubmitResult submit(AgentInvokeCmd cmd) {
        String taskId = cmd != null ? cmd.getTaskId() : null;
        if (cmd == null || cmd.getEnhancedPrompt() == null || cmd.getEnhancedPrompt().isEmpty()) {
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
                    .message("enhancedPrompt is required")
                    .build();
        }

        try {
            // 1. 创建 session
            String title = "task-" + (taskId != null ? taskId : System.currentTimeMillis());
            Session session = openCodeClient.createSession(title);
            String sessionId = session.getId();
            log.info("OpenCode session created: sessionId={}, taskId={}", sessionId, taskId);

            // 2. 构建 prompt 请求
            PromptRequest request = OpenCodePromptRequestMapper.toPromptRequest(cmd, defaultAgent, defaultModel);

            // 3. 同步发送 prompt 并等待 AI 响应
            PromptResult result = openCodeClient.prompt(sessionId, request);

            // 4. 记录映射
            if (taskId != null) {
                taskIdToSessionId.put(taskId, sessionId);
            }

            log.info("OpenCode invoke success, taskId={}, sessionId={}", taskId, sessionId);
            return SubmitResult.builder()
                    .taskId(taskId)
                    .providerTaskId(sessionId)
                    .status(SubmitResult.InvokeStatus.ACCEPTED)
                    .message("OK")
                    .build();
        } catch (Exception e) {
            log.error("OpenCode invoke error for task: {}", taskId, e);
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
                    .message("OpenCode invoke error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            log.warn("OpenCode cancel ignored: taskId is empty");
            return;
        }
        String sessionId = taskIdToSessionId.remove(taskId);
        if (sessionId == null) {
            log.warn("OpenCode cancel: no sessionId mapped for taskId={}", taskId);
            return;
        }
        log.info("OpenCode cancel: aborting session, taskId={}, sessionId={}", taskId, sessionId);
        try {
            openCodeClient.abort(sessionId);
            log.info("OpenCode cancel success, taskId={}, sessionId={}", taskId, sessionId);
        } catch (Exception e) {
            log.error("OpenCode cancel failed, taskId={}, sessionId={}: {}", taskId, sessionId, e.getMessage(), e);
        }
    }

    @Override
    public CallbackOutcome handleCallback(RawCallbackPayload payload) {
        return callbackParser.parse(payload);
    }

    /**
     * 从 AI 响应文本中解析回调结果（供外部调用方使用）。
     */
    public CallbackOutcome parseFromAiResponse(String text) {
        return callbackParser.parseFromText(text);
    }

    public String getDefaultAgent() {
        return defaultAgent;
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
