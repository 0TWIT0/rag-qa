package com.example.rag.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 把一段长文本切成若干「固定长度 + 重叠」的片段。
 *
 * <p>策略：滑动窗口。每块最多 {@code chunkSize} 个字符，相邻两块重叠
 * {@code chunkOverlap} 个字符。对中文按字符计（每个汉字算 1 个字符），
 * 因此直接按 {@link String#substring} 切即可。</p>
 */
@Service
public class TextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    public TextSplitter(
            @Value("${rag.chunk-size:500}") int chunkSize,
            @Value("${rag.chunk-overlap:50}") int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("rag.chunk-size 必须大于 0");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("rag.chunk-overlap 必须在 [0, rag.chunk-size) 范围内");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 切块。
     *
     * @param text       原始文本
     * @param sourceName 来源文档名
     * @return 切片列表；文本为空时返回空列表
     */
    public List<Chunk> split(String text, String sourceName) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalized = normalize(text);
        int start = 0;
        int index = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(new Chunk(piece, index++, sourceName));
            }
            if (end >= normalized.length()) {
                break;
            }
            start = end - chunkOverlap;
        }
        return chunks;
    }

    /** 统一换行符并折叠多余空白，让块边界更整齐。 */
    private String normalize(String text) {
        return text.replace("\r\n", "\n")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n");
    }
}
