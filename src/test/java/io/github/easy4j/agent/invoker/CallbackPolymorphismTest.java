package io.github.easy4j.agent.invoker;

import io.github.easy4j.agent.invoker.openclaw.OpenClawCallbackParser;
import io.github.easy4j.agent.invoker.opencode.OpenCodeCallbackParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CallbackPolymorphismTest {

    private static final String TYPE_ID = "java.util.HashMap<java.io.FileOutputStream,java.lang.String>";

    @Test
    void openClawTreatsClassNameAsData() {
        RawCallbackPayload payload = RawCallbackPayload.builder()
                .rawBody("{\"task_id\":\"t1\",\"title\":\"ok\",\"extra\":{\"@class\":\"" + TYPE_ID + "\"}}")
                .build();

        CallbackOutcome outcome = new OpenClawCallbackParser().parse(payload);

        assertEquals(CallbackOutcome.CallbackStatus.SUCCESS, outcome.getStatus());
        assertEquals("t1", outcome.getTaskId());
        assertEquals(TYPE_ID, outcome.getExtra().get("@class"));
        assertFalse(outcome.getExtra().get("@class") instanceof Class);
    }

    @Test
    void openCodeTreatsClassNameAsDataInFencedJson() {
        String text = "```json\n{\"task_id\":\"t2\",\"title\":\"ok\",\"extra\":{\"@class\":\""
                + TYPE_ID + "\"}}\n```";

        CallbackOutcome outcome = new OpenCodeCallbackParser().parseFromText(text);

        assertEquals(CallbackOutcome.CallbackStatus.SUCCESS, outcome.getStatus());
        assertEquals("t2", outcome.getTaskId());
        assertEquals(TYPE_ID, outcome.getExtra().get("@class"));
    }
}
