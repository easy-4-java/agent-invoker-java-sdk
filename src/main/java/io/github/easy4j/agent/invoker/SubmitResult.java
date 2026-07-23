package io.github.easy4j.agent.invoker;

/**
 * Provider 提交结果。
 */
public class SubmitResult {

    private String taskId;
    private String providerTaskId;
    private InvokeStatus status;
    private String message;

    public enum InvokeStatus { ACCEPTED, REJECTED }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProviderTaskId() { return providerTaskId; }
    public void setProviderTaskId(String providerTaskId) { this.providerTaskId = providerTaskId; }

    public InvokeStatus getStatus() { return status; }
    public void setStatus(InvokeStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isAccepted() { return status == InvokeStatus.ACCEPTED; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SubmitResult r = new SubmitResult();
        public Builder taskId(String v) { r.taskId = v; return this; }
        public Builder providerTaskId(String v) { r.providerTaskId = v; return this; }
        public Builder status(InvokeStatus v) { r.status = v; return this; }
        public Builder message(String v) { r.message = v; return this; }
        public SubmitResult build() { return r; }
    }
}
