package com.example.rag.dto;

import java.util.List;

/** 相似度检索响应。 */
public record SearchResponse(String query, int resultCount, List<SearchResult> results) {}
