package com.example.rag.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.SearchQuery;
import com.example.rag.dto.SearchResponse;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.VectorStoreService;

/**
 * 相似度检索接口（阶段 4）。阶段 5 会在此基础上把检索结果拼进 prompt 交给大模型。
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 20;

    private final VectorStoreService vectorStoreService;

    public SearchController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /** POST /api/search —— {"query": "...", "topK": 4} → 返回 top-K 最相似原文块。 */
    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchQuery query) {
        int topK = (query.topK() == null || query.topK() <= 0) ? DEFAULT_TOP_K : Math.min(query.topK(), MAX_TOP_K);
        List<SearchResult> results = vectorStoreService.search(query.query(), topK);
        return new SearchResponse(query.query(), results.size(), results);
    }
}
