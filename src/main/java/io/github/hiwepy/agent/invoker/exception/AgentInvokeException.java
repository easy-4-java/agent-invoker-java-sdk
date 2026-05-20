package io.github.hiwepy.agent.invoker;

/**
 * AI Agent 调用异常。
 */
public class AgentInvokeException extends RuntimeException {

    public AgentInvokeException(String message) {
        super(message);
    }

    public AgentInvokeException(String message, Throwable cause) {
        super(message, cause);
    }
}
