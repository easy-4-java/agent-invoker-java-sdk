package io.github.hiwepy.agent.invoker.opencode;

import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.opencode.model.PromptRequest;

import java.util.Map;
import java.util.Objects;

/**
 * 将业务层 {@link AgentInvokeCmd} 翻译为 OpenCode {@link PromptRequest}。
 *
 * <p>与 OpenClawInvokeRequestMapper 对称，将增强 prompt 及元数据
 * 封装为 opencode session.prompt 请求体。</p>
 */
public final class OpenCodePromptRequestMapper {

    private OpenCodePromptRequestMapper() {
    }

    /**
     * 构建 OpenCode prompt 请求。
     *
     * @param cmd   业务调用命令
     * @param agent 默认 agent（可被变量覆盖）
     * @param model 默认模型（可被变量覆盖），格式 provider/model
     * @return prompt 请求
     */
    public static PromptRequest toPromptRequest(AgentInvokeCmd cmd, String agent, String model) {
        Objects.requireNonNull(cmd, "cmd");

        String promptText = buildPromptText(cmd);
        PromptRequest request = PromptRequest.ofText(promptText);

        // 解析 agent（优先级：变量 > 默认值）
        String resolvedAgent = resolveVariable(cmd, "agent", agent);
        if (hasText(resolvedAgent)) {
            request.setAgent(resolvedAgent);
        }

        // 解析模型（优先级：变量 > 默认值）
        String resolvedModel = resolveVariable(cmd, "model", model);
        if (hasText(resolvedModel) && resolvedModel.contains("/")) {
            String[] parts = resolvedModel.split("/", 2);
            request.setModel(new PromptRequest.ModelRef(parts[0], parts[1]));
        }

        return request;
    }

    /**
     * 组装 prompt 文本：原始 prompt + 元数据。
     * <p>
     * 注意：与 OpenClaw 不同，opencode 没有 webhook/callback 机制，
     * 所以不注入 callback URL。回调逻辑由调用方（如 cloud-agents 的 OpenCodeAgentInvoker）
     * 通过轮询 session 消息或 SSE 事件流实现。
     * </p>
     */
    static String buildPromptText(AgentInvokeCmd cmd) {
        StringBuilder text = new StringBuilder();
        if (hasText(cmd.getEnhancedPrompt())) {
            text.append(cmd.getEnhancedPrompt().trim());
        }

        // 附加元数据（供 AI 参考，不强制要求回调格式）
        if (hasText(cmd.getTaskId()) || hasText(cmd.getTenantId())
                || hasText(cmd.getUserId()) || hasText(cmd.getChannel())) {
            text.append("\n\n---\n");
            if (hasText(cmd.getTaskId())) {
                text.append("Task ID: ").append(cmd.getTaskId().trim()).append("\n");
            }
            if (hasText(cmd.getTenantId())) {
                text.append("Tenant ID: ").append(cmd.getTenantId().trim()).append("\n");
            }
            if (hasText(cmd.getUserId())) {
                text.append("User ID: ").append(cmd.getUserId().trim()).append("\n");
            }
            if (hasText(cmd.getChannel())) {
                text.append("Channel: ").append(cmd.getChannel().trim()).append("\n");
            }
        }

        Map<String, Object> variables = cmd.getVariables();
        if (variables != null && !variables.isEmpty()) {
            text.append("\nVariables: ").append(variables.toString()).append("\n");
        }

        return text.toString();
    }

    /**
     * 解析 opencode.* 前缀的变量。
     */
    static String resolveVariable(AgentInvokeCmd cmd, String key, String defaultValue) {
        if (cmd == null || cmd.getVariables() == null || key == null) {
            return defaultValue;
        }
        Object value = cmd.getVariables().get("opencode." + key);
        if (value != null && hasText(value.toString())) {
            return value.toString().trim();
        }
        return defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
