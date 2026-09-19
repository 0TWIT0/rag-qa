package com.example.rag.document;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 已入库文档的元信息与全文（阶段 6 持久化到 MySQL）。
 *
 * <p>存全文是为了将来换 pgvector / 重建向量库时无需重新上传文件。</p>
 */
@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    private String contentType;

    private long fileSize;

    private int charCount;

    private int chunkCount;

    @Lob
    private String content;

    private LocalDateTime createdAt;

    /** JPA 要求的无参构造。 */
    protected DocumentEntity() {
    }

    /** 新建时使用（id 由数据库自增生成）。 */
    public DocumentEntity(String filename, String contentType, long fileSize,
                          int charCount, int chunkCount, String content, LocalDateTime createdAt) {
        this(null, filename, contentType, fileSize, charCount, chunkCount, content, createdAt);
    }

    /** 完整构造（测试或从数据库读回时使用）。 */
    public DocumentEntity(Long id, String filename, String contentType, long fileSize,
                          int charCount, int chunkCount, String content, LocalDateTime createdAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.charCount = charCount;
        this.chunkCount = chunkCount;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getCharCount() {
        return charCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
