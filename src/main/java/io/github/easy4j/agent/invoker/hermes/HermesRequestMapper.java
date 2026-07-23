package io.github.easy4j.agent.invoker.hermes;

import io.github.easy4j.agent.invoker.AgentInvokeCmd;
import io.github.easy4j.hermes.model.RunCreateRequest;

import java.util.Map;

public class HermesRequestMapper {

    public static RunCreateRequest toRunCreateRequest(AgentInvokeCmd cmd, String defaultInstructions) {
        RunCreateRequest request = new RunCreateRequest();
        request.setInput(cmd.getEnhancedPrompt());

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
        }

        return request;
    }
}
