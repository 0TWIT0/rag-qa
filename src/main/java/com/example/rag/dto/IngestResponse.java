package com.example.rag.dto;

/** 向量化入库结果（阶段 6 起带上落库后的 documentId）。 */
public record IngestResponse(String filename, int charCount, int chunkCount, int storedCount, Long documentId) {}
