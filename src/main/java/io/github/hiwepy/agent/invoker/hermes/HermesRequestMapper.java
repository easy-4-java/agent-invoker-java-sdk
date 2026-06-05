package io.github.hiwepy.agent.invoker.hermes;

import io.github.hiwepy.agent.invoker.AgentInvokeCmd;
import io.github.hiwepy.hermes.model.RunCreateRequest;

import java.util.Map;

<<<<<<< HEAD
/**
 * 将业务层 {@link AgentInvokeCmd} 翻译为 Hermes {@link RunCreateRequest}。
 *
 * <p>字段映射：</p>
 * <ul>
 *     <li>{@code input} ← {@link AgentInvokeCmd#getEnhancedPrompt()}</li>
 *     <li>{@code instructions} ← {@code variables["hermes.instructions"]} 或 defaultInstructions</li>
 *     <li>{@code model} ← {@code variables["hermes.model"]}</li>
 *     <li>{@code sessionId} ← {@code variables["hermes.sessionId"]}</li>
 *     <li>{@code conversation} ← {@code variables["hermes.conversation"]}</li>
 * </ul>
 */
public final class HermesRequestMapper {

    private static final String VAR_PREFIX = "hermes.";

    private HermesRequestMapper() {
    }

    /**
     * 构建 Hermes RunCreateRequest。
     *
     * @param cmd               业务调用命令
     * @param defaultInstructions 默认 instructions（可被变量覆盖）
     * @return Hermes RunCreateRequest
     */
=======
public class HermesRequestMapper {

>>>>>>> c15e88a (feat: add hermes-agent adapter)
    public static RunCreateRequest toRunCreateRequest(AgentInvokeCmd cmd, String defaultInstructions) {
        RunCreateRequest request = new RunCreateRequest();
        request.setInput(cmd.getEnhancedPrompt());

<<<<<<< HEAD
        String instructions = stringVariable(cmd, "instructions", defaultInstructions);
        if (instructions != null) {
            request.setInstructions(instructions);
        }

        String model = stringVariable(cmd, "model", null);
        if (model != null) {
            request.setModel(model);
        }

        String sessionId = stringVariable(cmd, "sessionId", null);
        if (sessionId != null) {
            request.setSessionId(sessionId);
        }

        String conversation = stringVariable(cmd, "conversation", null);
        if (conversation != null) {
            request.setConversation(conversation);
=======
        Map<String, Object> variables = cmd.getVariables();
        if (variables != null) {
            Object instructions = variables.get("hermes.instructions");
            if (instructions != null) {
                request.setInstructions(instructions.toString());
            }
            Object model = variables.get("hermes.model");
            if (model != null) {
                request.setModel(model.toString());
            }
            Object sessionId = variables.get("hermes.sessionId");
            if (sessionId != null) {
                request.setSessionId(sessionId.toString());
            }
            Object conversation = variables.get("hermes.conversation");
            if (conversation != null) {
                request.setConversation(conversation.toString());
            }
        }

        if (request.getInstructions() == null && defaultInstructions != null) {
            request.setInstructions(defaultInstructions);
>>>>>>> c15e88a (feat: add hermes-agent adapter)
        }

        return request;
    }
<<<<<<< HEAD

    private static String stringVariable(AgentInvokeCmd cmd, String key, String defaultValue) {
        String value = readVariable(cmd, key);
        return hasText(value) ? value : defaultValue;
    }

    private static String readVariable(AgentInvokeCmd cmd, String key) {
        if (cmd == null || cmd.getVariables() == null || key == null) {
            return null;
        }
        Map<String, Object> variables = cmd.getVariables();
        Object value = variables.get(VAR_PREFIX + key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
=======
>>>>>>> c15e88a (feat: add hermes-agent adapter)
}
