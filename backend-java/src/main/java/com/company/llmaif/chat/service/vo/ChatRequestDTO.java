package com.company.llmaif.chat.service.vo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ChatRequestDTO {

    @NotEmpty(message = "消息不能为空")
    @Valid
    private List<MessageDTO> messages;

    @Data
    public static class MessageDTO {
        @NotBlank(message = "消息角色不能为空")
        private String role;

        @NotBlank(message = "消息内容不能为空")
        private String content;
    }
}
