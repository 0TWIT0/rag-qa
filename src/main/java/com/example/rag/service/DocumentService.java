package com.example.rag.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.rag.document.Chunk;
import com.example.rag.document.DocumentEntity;
import com.example.rag.document.DocumentNotFoundException;
import com.example.rag.document.DocumentRepository;
import com.example.rag.dto.DocumentDetail;
import com.example.rag.dto.DocumentSummary;

/**
 * 文档管理（阶段 6）：把上传文档的元信息 + 全文持久化到 MySQL，并提供增删查。
 *
 * <p>ingest / delete 用事务包住「落库 + 向量库操作」：向量化若失败则回滚落库，保证一致性。</p>
 */
@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final VectorStoreService vectorStoreService;

    public DocumentService(DocumentRepository repository, VectorStoreService vectorStoreService) {
        this.repository = repository;
        this.vectorStoreService = vectorStoreService;
    }

    /** 文档列表（摘要，不含全文）。 */
    public List<DocumentSummary> list() {
        return repository.findAll().stream().map(this::toSummary).toList();
    }

    /** 文档详情（含全文）。 */
    public DocumentDetail get(Long id) {
        return toDetail(find(id));
    }

    /** 入库：先落库拿到自增 id，再向量化（带 documentId）；向量化失败则整体回滚。 */
    @Transactional
    public DocumentEntity ingest(String filename, String contentType, long fileSize,
                                 String text, List<Chunk> chunks) {
        DocumentEntity entity = new DocumentEntity(filename, contentType, fileSize,
                text.length(), chunks.size(), text, LocalDateTime.now());
        entity = repository.save(entity);
        vectorStoreService.ingest(chunks, entity.getId());
        return entity;
    }

    /** 删除：删 MySQL 记录 + 删向量库对应块。 */
    @Transactional
    public void delete(Long id) {
        DocumentEntity entity = find(id);
        vectorStoreService.deleteByDocumentId(entity.getId(), entity.getChunkCount());
        repository.delete(entity);
    }

    private DocumentEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("文档不存在：id=" + id));
    }

    private DocumentSummary toSummary(DocumentEntity e) {
        return new DocumentSummary(e.getId(), e.getFilename(), e.getContentType(),
                e.getFileSize(), e.getCharCount(), e.getChunkCount(), e.getCreatedAt());
    }

    private DocumentDetail toDetail(DocumentEntity e) {
        return new DocumentDetail(e.getId(), e.getFilename(), e.getContentType(),
                e.getFileSize(), e.getCharCount(), e.getChunkCount(), e.getCreatedAt(), e.getContent());
    }
}
