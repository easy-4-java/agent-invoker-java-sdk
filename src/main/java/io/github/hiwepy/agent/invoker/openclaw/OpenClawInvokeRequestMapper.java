package io.github.hiwepy.agent.invoker.openclaw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.openclaw.InvokeAgentRequest;
import io.github.hiwepy.openclaw.OpenClawSessionKeys;

import java.util.Map;
import java.util.Objects;

/**
 * 将业务层 {@link AgentInvokeCmd} 翻译为 OpenClaw {@link InvokeAgentRequest}。
 *
 * <p>callbackUrl 优先取自 {@link AgentInvokeCmd#getCallbackUrl()}，否则回退到 adapter 配置的
 * {@code callbackBaseUrl}，并写入 agent 提示词供 OpenClaw 回写业务系统（Gateway Hooks 协议无独立 callbackUrl 字段）。</p>
 * <p>其余 Hook 可选字段（{@code deliver}、{@code model}、{@code thinking}、{@code wakeMode}、
 * {@code timeoutSeconds}、{@code name}）可通过 {@link AgentInvokeCmd#getVariables()} 中 {@code openclaw.*}
 * 键传入，避免扩展业务命令模型。</p>
 */
public final class OpenClawInvokeRequestMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VAR_PREFIX = "openclaw.";

    private OpenClawInvokeRequestMapper() {
    }

    /**
     * 构建 OpenClaw Gateway {@code POST /hooks/agent} 请求体。
     *
     * @param cmd             业务调用命令
     * @param callbackBaseUrl adapter 级 callback 基础 URL（{@code agents.provider.openclaw.callback-base-url}）
     * @return 可直接交给 {@link io.github.hiwepy.openclaw.OpenClawClient#agent(InvokeAgentRequest)} 的请求
     */
    public static InvokeAgentRequest toInvokeRequest(AgentInvokeCmd cmd, String callbackBaseUrl) {
        Objects.requireNonNull(cmd, "cmd");
        InvokeAgentRequest request = new InvokeAgentRequest();
        request.setAgentId(cmd.getAgentId());
        request.setMessage(buildMessage(cmd, resolveCallbackUrl(cmd, callbackBaseUrl)));
        request.setName(stringVariable(cmd, "name", "Generation"));
        request.setWakeMode(stringVariable(cmd, "wakeMode", "now"));
        request.setTimeoutSeconds(intVariable(cmd, "timeoutSeconds", 300));

        String hookChannel = stringVariable(cmd, "channel", null);
        if (hasText(hookChannel)) {
            request.setChannel(hookChannel);
        } else if (hasText(cmd.getChannel())) {
            request.setChannel(cmd.getChannel());
        }
        String hookTo = stringVariable(cmd, "to", null);
        if (hasText(hookTo)) {
            request.setTo(hookTo);
        } else if (hasText(cmd.getUserId())) {
            request.setTo(cmd.getUserId());
        }
        Boolean deliver = booleanVariable(cmd, "deliver");
        if (deliver != null) {
            request.setDeliver(deliver);
        }
        String model = stringVariable(cmd, "model", null);
        if (hasText(model)) {
            request.setModel(model);
        }
        String thinking = stringVariable(cmd, "thinking", null);
        if (hasText(thinking)) {
            request.setThinking(thinking);
        }
        String sessionKey = buildSessionKey(cmd);
        if (sessionKey != null) {
            request.setSessionKey(sessionKey);
        }
        return request;
    }

    /**
     * 解析最终 callback URL：命令级覆盖 adapter 默认值。
     */
    public static String resolveCallbackUrl(AgentInvokeCmd cmd, String callbackBaseUrl) {
        if (cmd != null && hasText(cmd.getCallbackUrl())) {
            return cmd.getCallbackUrl().trim();
        }
        if (hasText(callbackBaseUrl)) {
            return callbackBaseUrl.trim();
        }
        return "http://localhost:7088";
    }

    /**
     * 组装 agent message：原始 prompt + callback 指令 + 租户/变量等元数据。
     */
    static String buildMessage(AgentInvokeCmd cmd, String callbackUrl) {
        StringBuilder message = new StringBuilder();
        if (hasText(cmd.getEnhancedPrompt())) {
            message.append(cmd.getEnhancedPrompt().trim());
        }

        message.append("\n\n---\n");
        message.append("When finished, POST the result JSON to callback URL: ").append(callbackUrl);
        if (hasText(cmd.getTaskId())) {
            message.append("\nInclude field task_id=\"").append(cmd.getTaskId()).append("\" in the JSON body.");
        }
        if (hasText(cmd.getCallbackToken())) {
            message.append("\nAuthenticate callback with api_key=\"").append(cmd.getCallbackToken()).append("\".");
        }
        if (hasText(cmd.getTenantId())) {
            message.append("\nTenant ID: ").append(cmd.getTenantId());
        }
        if (hasText(cmd.getUserId())) {
            message.append("\nUser ID: ").append(cmd.getUserId());
        }
        if (hasText(cmd.getChannel())) {
            message.append("\nChannel: ").append(cmd.getChannel());
        }
        Map<String, Object> variables = cmd.getVariables();
        if (variables != null && !variables.isEmpty()) {
            message.append("\nVariables (JSON):\n").append(toJson(variables));
        }
        return message.toString();
    }

    /**
     * 按 {@link OpenClawSessionKeys} 约定构造 sessionKey，便于 Gateway {@code hooks:} 命名空间接受。
     *
     * <ul>
     *     <li>有 peer + taskId → {@code hook:<peerId>:<taskId>}（一次性任务）</li>
     *     <li>有 peer + agentId → {@code hook:<agentId>:<peerId>}（固定多轮）</li>
     * </ul>
     */
    static String buildSessionKey(AgentInvokeCmd cmd) {
        if (cmd == null) {
            return null;
        }
        String peerId = resolvePeerId(cmd);
        try {
            if (hasText(peerId) && hasText(cmd.getTaskId())) {
                return OpenClawSessionKeys.forEphemeralPeer(peerId, cmd.getTaskId().trim());
            }
            if (hasText(peerId) && hasText(cmd.getAgentId())) {
                return OpenClawSessionKeys.forStableSession(cmd.getAgentId(), peerId);
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    /**
     * 解析业务 peer：优先 {@code tenantId.userId}，否则 tenant 或 user 单独作为 peer。
     */
    static String resolvePeerId(AgentInvokeCmd cmd) {
        if (cmd == null) {
            return null;
        }
        if (hasText(cmd.getUserId())) {
            if (hasText(cmd.getTenantId())) {
                return cmd.getTenantId().trim() + "." + cmd.getUserId().trim();
            }
            return cmd.getUserId().trim();
        }
        if (hasText(cmd.getTenantId())) {
            return cmd.getTenantId().trim();
        }
        return null;
    }

    private static String stringVariable(AgentInvokeCmd cmd, String key, String defaultValue) {
        String value = readVariable(cmd, key);
        return hasText(value) ? value : defaultValue;
    }

    private static String readVariable(AgentInvokeCmd cmd, String key) {
        if (cmd == null || cmd.getVariables() == null || key == null) {
            return null;
        }
        Object value = cmd.getVariables().get(VAR_PREFIX + key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static Boolean booleanVariable(AgentInvokeCmd cmd, String key) {
        if (cmd == null || cmd.getVariables() == null || key == null) {
            return null;
        }
        Object value = cmd.getVariables().get(VAR_PREFIX + key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private static int intVariable(AgentInvokeCmd cmd, String key, int defaultValue) {
        if (cmd == null || cmd.getVariables() == null || key == null) {
            return defaultValue;
        }
        Object value = cmd.getVariables().get(VAR_PREFIX + key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String toJson(Map<String, Object> variables) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            return variables.toString();
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
