package com.example.rag.dto;

/** 单条检索结果：命中的原文块及其来源、相似度分数。 */
public record SearchResult(String text, Double score, String sourceName, Integer index) {}
