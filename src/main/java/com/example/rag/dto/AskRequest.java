package com.example.rag.dto;

/** RAG 问答请求体：用户问题 + 可选 topK（检索的相关块数）。 */
public record AskRequest(String question, Integer topK) {}
