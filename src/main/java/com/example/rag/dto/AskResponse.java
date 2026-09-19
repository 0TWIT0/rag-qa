package com.example.rag.dto;

import java.util.List;

/**
 * RAG 问答响应：带出处的回答。
 *
 * <p>{@code sources} 复用检索结果 {@link SearchResult}（含原文块、来源文件名、块序号、相似度）。
 * 检索不到相关块时走严格模式：{@code answer} 为「资料库中没有找到相关信息」，{@code sources} 为空。</p>
 */
public record AskResponse(String question, String answer, int sourceCount, List<SearchResult> sources) {}
