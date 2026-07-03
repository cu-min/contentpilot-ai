package com.aicontent.marketing.platformcontent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlatformContentUpdateRequest {

    @NotBlank(message = "请输入标题")
    @Size(max = 200, message = "标题最多 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要最多 500 个字符")
    private String summary;

    private String content;

    @Size(max = 500, message = "标签最多 500 个字符")
    private String tags;

    @Size(max = 500, message = "关键词最多 500 个字符")
    private String keywords;
}
