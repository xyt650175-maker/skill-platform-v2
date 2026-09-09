package com.company.llmaif.chat.service.vo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequestDTO {

    @NotEmpty(message = "消息不能为空")
    @Valid
    private List<MessageDTO> messages;

    /** Skill Creator 运行时使用的工作区快照。 */
    private Map<String, String> files;

    /** 当前 Skill 名称，仅用于给运行时补充任务上下文。 */
    private String skillName;

    /** 当前平台版本，由 Skill Creator 写入生成文件。 */
    private String skillVersion;

    @Data
    public static class MessageDTO {
        @NotBlank(message = "消息角色不能为空")
        private String role;

        @NotBlank(message = "消息内容不能为空")
        private String content;
    }
}
