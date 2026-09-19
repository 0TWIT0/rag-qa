package com.example.rag.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.example.rag.document.Chunk;
import com.example.rag.dto.SearchResult;

/**
 * 向量化入库与相似度检索。
 *
 * <p>入库：把阶段 3 切出的 chunk 转成 Spring AI 的 {@link Document}（带上来源文件名、
 * 序号与 documentId 作为元数据，id 固定为 {@code documentId-index}），交给向量库计算
 * embedding 并存储。检索：把查询词交给向量库做相似度检索，返回 top-K 个最相似的原文块。</p>
 */
@Service
public class VectorStoreService {

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** 将一批 chunk 向量化后入库（带上 documentId），返回实际入库数量。 */
    public int ingest(List<Chunk> chunks, Long documentId) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }
        List<Document> documents = new ArrayList<>(chunks.size());
        for (Chunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sourceName", chunk.sourceName());
            metadata.put("index", chunk.index());
            metadata.put("documentId", documentId);
            documents.add(Document.builder()
                    .id(documentId + "-" + chunk.index())
                    .text(chunk.text())
                    .metadata(metadata)
                    .build());
        }
        vectorStore.add(documents);
        return documents.size();
    }

    /** 按 documentId 删除该文档的所有向量块。 */
    public void deleteByDocumentId(Long documentId, int chunkCount) {
        if (chunkCount <= 0) {
            return;
        }
        List<String> ids = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            ids.add(documentId + "-" + i);
        }
        vectorStore.delete(ids);
    }

    /** 相似度检索 top-K，返回带分数的最相似原文块。 */
    public List<SearchResult> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);

        List<SearchResult> results = new ArrayList<>(documents.size());
        for (Document doc : documents) {
            String sourceName = asString(doc.getMetadata().get("sourceName"));
            Integer index = asInt(doc.getMetadata().get("index"));
            // SimpleVectorStore 返回的 getScore() 即余弦相似度（0~1，越大越相关）
            results.add(new SearchResult(doc.getText(), doc.getScore(), sourceName, index));
        }
        return results;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
