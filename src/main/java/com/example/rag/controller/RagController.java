package com.example.rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.service.RagService;

/**
 * RAG 问答接口（阶段 5）：把检索到的文档片段拼进 prompt，返回带出处的回答。
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 20;

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /** POST /api/rag/ask —— {"question": "...", "topK": 4} → 带出处的回答。 */
    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        int topK = (request.topK() == null || request.topK() <= 0)
                ? DEFAULT_TOP_K
                : Math.min(request.topK(), MAX_TOP_K);
        return ragService.ask(request.question(), topK);
    }
}
