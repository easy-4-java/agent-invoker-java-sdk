package io.github.easy4j.agent.invoker.hermes;

import io.github.easy4j.agent.invoker.AgentInvokeCmd;
import io.github.easy4j.agent.invoker.AgentInvoker;
import io.github.easy4j.agent.invoker.CallbackOutcome;
import io.github.easy4j.agent.invoker.RawCallbackPayload;
import io.github.easy4j.agent.invoker.SubmitResult;
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.model.RunCreateRequest;
import io.github.easy4j.hermes.model.RunStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class HermesAgentInvoker implements AgentInvoker {

    public static final String PROVIDER_CODE = "hermes";

    private final HermesClient hermesClient;
    private final HermesCallbackParser callbackParser;
    private final String callbackBaseUrl;
    private final ConcurrentMap<String, String> taskIdToRunId = new ConcurrentHashMap<>();
    private final ExecutorService pollingExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "hermes-poll");
        t.setDaemon(true);
        return t;
    });

    public HermesAgentInvoker(HermesClient hermesClient, String callbackBaseUrl) {
        this.hermesClient = Objects.requireNonNull(hermesClient, "hermesClient");
        this.callbackBaseUrl = callbackBaseUrl != null ? callbackBaseUrl : "http://localhost:7088";
        this.callbackParser = new HermesCallbackParser();
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SubmitResult submit(AgentInvokeCmd cmd) {
        String taskId = cmd.getTaskId();
        String enhancedPrompt = cmd.getEnhancedPrompt();
        if (enhancedPrompt == null || enhancedPrompt.isEmpty()) {
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
                    .message("enhancedPrompt is required for hermes")
                    .build();
        }

        try {
            RunCreateRequest request = HermesRequestMapper.toRunCreateRequest(cmd,
                    hermesClient.getConfig().getDefaultInstructions());
            RunStatus result = hermesClient.createRun(request);
            if (result == null || result.getRunId() == null) {
                log.warn("Hermes invoke failed, taskId={}", taskId);
                return SubmitResult.builder()
                        .taskId(taskId)
                        .status(SubmitResult.InvokeStatus.REJECTED)
                        .message("Hermes createRun returned null")
                        .build();
            }

            String runId = result.getRunId();
            taskIdToRunId.put(taskId, runId);
            log.info("Hermes invoke success, taskId={}, runId={}", taskId, runId);

            // Async polling for completion
            pollingExecutor.submit(() -> pollUntilComplete(taskId, runId, cmd.getCallbackUrl(), cmd.getCallbackToken()));

            return SubmitResult.builder()
                    .taskId(taskId)
                    .providerTaskId(runId)
                    .status(SubmitResult.InvokeStatus.ACCEPTED)
                    .message("OK")
                    .build();
        } catch (Exception e) {
            log.error("Hermes invoke error for task: {}", taskId, e);
            return SubmitResult.builder()
                    .taskId(taskId)
                    .status(SubmitResult.InvokeStatus.REJECTED)
                    .message("Hermes invoke error: " + e.getMessage())
                    .build();
        }
    }

    private void pollUntilComplete(String taskId, String runId, String callbackUrl, String callbackToken) {
        int maxAttempts = 100;
        int intervalMs = 3000;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(intervalMs);
                RunStatus status = hermesClient.getRun(runId);
                if (status == null) continue;
                String state = status.getStatus();
                if ("completed".equals(state)) {
                    CallbackOutcome outcome = callbackParser.parseFromRunStatus(status, taskId);
                    if (outcome != null) {
                        log.info("Hermes run completed, taskId={}, runId={}", taskId, runId);
                        // POST callback if URL provided
                        if (callbackUrl != null && !callbackUrl.isEmpty()) {
                            postCallback(callbackUrl, callbackToken, outcome);
                        }
                    }
                    taskIdToRunId.remove(taskId);
                    return;
                } else if ("failed".equals(state) || "cancelled".equals(state)) {
                    log.warn("Hermes run {}, taskId={}, runId={}", state, taskId, runId);
                    taskIdToRunId.remove(taskId);
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Hermes poll error, taskId={}, attempt={}: {}", taskId, i, e.getMessage());
            }
        }
        log.warn("Hermes poll timeout, taskId={}, runId={}", taskId, runId);
    }

    private void postCallback(String callbackUrl, String callbackToken, CallbackOutcome outcome) {
        try {
            log.info("Posting callback to {} for taskId={}", callbackUrl, outcome.getTaskId());
            // Use HermesClient's HTTP capability or a simple HTTP call
            // For now, log the callback - actual HTTP POST implementation depends on the callback protocol
        } catch (Exception e) {
            log.error("Failed to post callback for taskId={}: {}", outcome.getTaskId(), e.getMessage());
        }
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            log.warn("Hermes cancel ignored: taskId is empty");
            return;
        }
        String runId = taskIdToRunId.remove(taskId);
        if (runId == null) {
            log.warn("Hermes cancel: no runId mapped for taskId={}", taskId);
            return;
        }
        try {
            hermesClient.stopRun(runId);
            log.info("Hermes cancel requested: taskId={}, runId={}", taskId, runId);
        } catch (Exception e) {
            log.error("Hermes cancel failed for taskId={}, runId={}: {}", taskId, runId, e.getMessage());
        }
    }

    @Override
    public CallbackOutcome handleCallback(RawCallbackPayload payload) {
        // Hermes has no webhook; polling drives callbacks
        return null;
    }
}
