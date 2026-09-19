package com.example.rag.dto;

import java.time.LocalDateTime;

/** 文档列表项（摘要，不含全文）。 */
public record DocumentSummary(Long id, String filename, String contentType, long fileSize,
                              int charCount, int chunkCount, LocalDateTime createdAt) {}
