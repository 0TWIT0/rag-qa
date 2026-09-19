package com.example.rag.dto;

/**
 * 聊天请求体：只包含用户问题。
 */
public record ChatRequest(String question) {
}
