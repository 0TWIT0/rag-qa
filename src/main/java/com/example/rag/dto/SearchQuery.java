package com.example.rag.dto;

/** 相似度检索请求体。 */
public record SearchQuery(String query, Integer topK) {}
