package com.company.llmaif.chat.service;

import com.company.llmaif.chat.service.vo.ChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    SseEmitter stream(ChatRequestDTO dto);
}
