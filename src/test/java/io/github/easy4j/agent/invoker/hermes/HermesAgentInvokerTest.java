package io.github.easy4j.agent.invoker.hermes;

import io.github.easy4j.agent.invoker.AgentInvokeCmd;
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.RunCreateRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HermesAgentInvokerTest {

    @Test
    void submitUsesConfiguredInstructionsWhenTaskDoesNotOverrideThem() {
        HermesClient client = clientWithDefaultInstructions("configured instructions");
        HermesAgentInvoker invoker = new HermesAgentInvoker(client, null);

        invoker.submit(AgentInvokeCmd.builder()
                .taskId("task-1")
                .enhancedPrompt("write a summary")
                .build());

        assertEquals("configured instructions", submittedRequest(client).getInstructions());
    }

    @Test
    void submitPrefersTaskInstructionsOverConfiguredDefault() {
        HermesClient client = clientWithDefaultInstructions("configured instructions");
        HermesAgentInvoker invoker = new HermesAgentInvoker(client, null);

        invoker.submit(AgentInvokeCmd.builder()
                .taskId("task-2")
                .enhancedPrompt("write a summary")
                .variables(Collections.<String, Object>singletonMap("hermes.instructions", "task instructions"))
                .build());

        assertEquals("task instructions", submittedRequest(client).getInstructions());
    }

    private static HermesClient clientWithDefaultInstructions(String defaultInstructions) {
        HermesClientConfig config = new HermesClientConfig();
        config.getHttp().setDefaultInstructions(defaultInstructions);
        HermesClient client = mock(HermesClient.class);
        when(client.getConfig()).thenReturn(config);
        return client;
    }

    private static RunCreateRequest submittedRequest(HermesClient client) {
        ArgumentCaptor<RunCreateRequest> request = ArgumentCaptor.forClass(RunCreateRequest.class);
        verify(client).createRun(request.capture());
        return request.getValue();
    }
}
