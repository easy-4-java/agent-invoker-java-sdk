package io.github.easy4j.agent.invoker.hermes;

import io.github.easy4j.agent.invoker.AgentInvokeCmd;
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.RunCreateRequest;
import io.github.easy4j.hermes.api.model.RunStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HermesAgentInvokerTest {

    @Test
    void submitUsesConfiguredInstructionsWhenTaskDoesNotOverrideThem() {
        RecordingHermesClient client = new RecordingHermesClient("configured instructions");
        HermesAgentInvoker invoker = new HermesAgentInvoker(client, null);

        invoker.submit(AgentInvokeCmd.builder()
                .taskId("task-1")
                .enhancedPrompt("write a summary")
                .build());

        assertEquals("configured instructions", client.lastRequest.getInstructions());
    }

    @Test
    void submitPrefersTaskInstructionsOverConfiguredDefault() {
        RecordingHermesClient client = new RecordingHermesClient("configured instructions");
        HermesAgentInvoker invoker = new HermesAgentInvoker(client, null);

        invoker.submit(AgentInvokeCmd.builder()
                .taskId("task-2")
                .enhancedPrompt("write a summary")
                .variables(Collections.<String, Object>singletonMap("hermes.instructions", "task instructions"))
                .build());

        assertEquals("task instructions", client.lastRequest.getInstructions());
    }

    private static final class RecordingHermesClient extends HermesClient {
        private RunCreateRequest lastRequest;

        private RecordingHermesClient(String defaultInstructions) {
            super(configWith(defaultInstructions));
        }

        private static HermesClientConfig configWith(String defaultInstructions) {
            HermesClientConfig config = new HermesClientConfig().markUnsafeBaseUrlOverriddenForTest();
            config.getHttp().setDefaultInstructions(defaultInstructions);
            return config;
        }

        @Override
        public RunStatus createRun(RunCreateRequest request) {
            lastRequest = request;
            return null;
        }
    }
}
