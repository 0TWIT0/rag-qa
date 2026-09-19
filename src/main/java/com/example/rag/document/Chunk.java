package com.example.rag.document;

/**
 * 一个文本片段（chunk），是后续向量化与检索的最小单位。
 *
 * @param text       片段文本
 * @param index      片段序号（从 0 开始，用于拼接时保持顺序 / 溯源）
 * @param sourceName 来源文档名（阶段 5 回答时给出处）
 */
public record Chunk(String text, int index, String sourceName) {
}
