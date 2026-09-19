package com.example.rag.dto;

import java.util.List;

import com.example.rag.document.Chunk;

/**
 * 文档解析接口的返回体：提取出的全文 + 切块结果。
 *
 * @param filename  原始文件名
 * @param text      提取出的纯文本
 * @param charCount 字符数
 * @param chunkCount 切块数量
 * @param chunks    切片列表
 */
public record DocumentParseResponse(
        String filename,
        String text,
        int charCount,
        int chunkCount,
        List<Chunk> chunks) {
}
