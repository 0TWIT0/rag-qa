package com.example.rag.document;

import org.springframework.data.jpa.repository.JpaRepository;

/** 文档元信息的 JPA 仓库（阶段 6）。 */
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
}
