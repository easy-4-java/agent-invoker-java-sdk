package io.github.hiwepy.agent.invoker;

import java.util.List;
import java.util.Map;

/**
 * Provider 回调处理后的业务语义结果。
 * <p>由 adapter 完成协议解析与字段翻译后返回，业务层不接触原始回调格式。</p>
 */
public class CallbackOutcome {

    private String taskId;
    private CallbackStatus status;
    private String apiKey;
    private String title;
    private String subtitle;
    private String content;
    private String coverUrl;
    private List<String> imageUrls;
    private List<String> tags;
    private List<String> topics;
    private String sourceChannel;
    private String outputType;
    private String imagePrompt;
    private Map<String, Object> extra;
    private String errorMessage;

    public enum CallbackStatus { SUCCESS, FAILED, PARTIAL }

    public String getTaskId() { return taskId; }
    public void setTaskId(String v) { this.taskId = v; }

    public CallbackStatus getStatus() { return status; }
    public void setStatus(CallbackStatus v) { this.status = v; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { this.apiKey = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String v) { this.subtitle = v; }

    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String v) { this.coverUrl = v; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> v) { this.imageUrls = v; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> v) { this.topics = v; }

    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String v) { this.sourceChannel = v; }

    public String getOutputType() { return outputType; }
    public void setOutputType(String v) { this.outputType = v; }

    public String getImagePrompt() { return imagePrompt; }
    public void setImagePrompt(String v) { this.imagePrompt = v; }

    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> v) { this.extra = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public boolean isSuccess() { return status == CallbackStatus.SUCCESS; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CallbackOutcome o = new CallbackOutcome();
        public Builder taskId(String v) { o.taskId = v; return this; }
        public Builder status(CallbackStatus v) { o.status = v; return this; }
        public Builder apiKey(String v) { o.apiKey = v; return this; }
        public Builder title(String v) { o.title = v; return this; }
        public Builder subtitle(String v) { o.subtitle = v; return this; }
        public Builder content(String v) { o.content = v; return this; }
        public Builder coverUrl(String v) { o.coverUrl = v; return this; }
        public Builder imageUrls(List<String> v) { o.imageUrls = v; return this; }
        public Builder tags(List<String> v) { o.tags = v; return this; }
        public Builder topics(List<String> v) { o.topics = v; return this; }
        public Builder sourceChannel(String v) { o.sourceChannel = v; return this; }
        public Builder outputType(String v) { o.outputType = v; return this; }
        public Builder imagePrompt(String v) { o.imagePrompt = v; return this; }
        public Builder extra(Map<String, Object> v) { o.extra = v; return this; }
        public Builder errorMessage(String v) { o.errorMessage = v; return this; }
        public CallbackOutcome build() { return o; }
    }
}
