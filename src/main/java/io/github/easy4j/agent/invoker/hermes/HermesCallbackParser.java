package io.github.easy4j.agent.invoker.hermes;

import io.github.easy4j.agent.invoker.CallbackOutcome;
import io.github.easy4j.hermes.model.RunStatus;
import io.github.easy4j.hermes.util.HermesJsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class HermesCallbackParser {

    private final HermesJsonParser jsonParser = new HermesJsonParser();

    public CallbackOutcome parseFromRunStatus(RunStatus status, String taskId) {
        if (status == null || status.getOutput() == null) {
            return CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.FAILED)
                    .errorMessage("Empty response from Hermes")
                    .build();
        }

        String output = status.getOutput();
        Map<String, Object> parsed = jsonParser.parseFromText(output);

        if (parsed != null) {
            CallbackOutcome.Builder builder = CallbackOutcome.builder()
                    .taskId(taskId)
                    .status(CallbackOutcome.CallbackStatus.SUCCESS);

            if (parsed.containsKey("title")) builder.title(parsed.get("title").toString());
            if (parsed.containsKey("subtitle")) builder.subtitle(parsed.get("subtitle").toString());
            if (parsed.containsKey("content")) builder.content(parsed.get("content").toString());
            if (parsed.containsKey("cover_url")) builder.coverUrl(parsed.get("cover_url").toString());
            if (parsed.containsKey("coverUrl")) builder.coverUrl(parsed.get("coverUrl").toString());
            if (parsed.containsKey("output_type")) builder.outputType(parsed.get("output_type").toString());
            if (parsed.containsKey("outputType")) builder.outputType(parsed.get("outputType").toString());
            if (parsed.containsKey("image_prompt")) builder.imagePrompt(parsed.get("image_prompt").toString());
            if (parsed.containsKey("imagePrompt")) builder.imagePrompt(parsed.get("imagePrompt").toString());
            if (parsed.containsKey("error_message")) builder.errorMessage(parsed.get("error_message").toString());

            return builder.build();
        }

        // No JSON found, use raw output as content
        return CallbackOutcome.builder()
                .taskId(taskId)
                .status(CallbackOutcome.CallbackStatus.SUCCESS)
                .content(output)
                .build();
    }
}
