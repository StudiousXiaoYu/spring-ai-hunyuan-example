package io.github.studiousxiaoyu.chat.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StreamResponse {
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("content")
    private String content;
    
    public StreamResponse(String type, String content) {
        this.type = type;
        this.content = content;
    }
}
