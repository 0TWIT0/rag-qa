package com.example.rag.dto;

import java.time.LocalDateTime;

/** 文档详情（含全文）。 */
public record DocumentDetail(Long id, String filename, String contentType, long fileSize,
                             int charCount, int chunkCount, LocalDateTime createdAt, String content) {}
