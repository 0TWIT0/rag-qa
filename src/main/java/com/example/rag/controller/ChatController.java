package com.example.rag.controller;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.ChatRequest;

/**
 * 聊天接口：接收问题，调用大模型返回回答。
 *
 * <p>阶段 2 只做「问 → 答」；阶段 5 会把检索到的文档片段拼进 prompt，
 * 让回答带出处。</p>
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * POST /api/chat —— 传入 {"question": "..."}，返回 {"answer": "..."}。
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String answer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();
        return Map.of("answer", answer);
    }
}
