package com.company.llmaif.chat.service;

import com.company.llmaif.chat.service.vo.ChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    SseEmitter stream(ChatRequestDTO dto);

    /** 通过真实 DeepAgent 运行时加载 Skill Creator 并生成 Skill 工作区文件。 */
    SseEmitter streamSkillCreator(ChatRequestDTO dto);
}
