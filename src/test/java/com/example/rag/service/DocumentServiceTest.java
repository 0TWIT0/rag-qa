package com.example.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.rag.document.Chunk;
import com.example.rag.document.DocumentEntity;
import com.example.rag.document.DocumentNotFoundException;
import com.example.rag.document.DocumentRepository;
import com.example.rag.dto.DocumentDetail;
import com.example.rag.dto.DocumentSummary;

class DocumentServiceTest {

    @Test
    void ingestPersistsAndVectorsWithDocumentId() {
        DocumentRepository repository = mock(DocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        DocumentService service = new DocumentService(repository, vectorStoreService);

        DocumentEntity saved = new DocumentEntity(1L, "a.md", "text/markdown", 100L, 5, 2, "hello", LocalDateTime.now());
        when(repository.save(any(DocumentEntity.class))).thenReturn(saved);

        List<Chunk> chunks = List.of(new Chunk("hello", 0, "a.md"));
        DocumentEntity result = service.ingest("a.md", "text/markdown", 100L, "hello", chunks);

        assertThat(result).isSameAs(saved);
        assertThat(result.getId()).isEqualTo(1L);
        verify(vectorStoreService).ingest(eq(chunks), eq(1L));
    }

    @Test
    void listReturnsSummariesWithoutContent() {
        DocumentRepository repository = mock(DocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        DocumentService service = new DocumentService(repository, vectorStoreService);

        when(repository.findAll()).thenReturn(List.of(
                new DocumentEntity(1L, "a.md", "text/markdown", 100L, 5, 2, "hello", LocalDateTime.now())));

        List<DocumentSummary> summaries = service.list();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id()).isEqualTo(1L);
        assertThat(summaries.get(0).filename()).isEqualTo("a.md");
    }

    @Test
    void getReturnsDetailWithContent() {
        DocumentRepository repository = mock(DocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        DocumentService service = new DocumentService(repository, vectorStoreService);

        when(repository.findById(1L)).thenReturn(Optional.of(
                new DocumentEntity(1L, "a.md", "text/markdown", 100L, 5, 2, "hello", LocalDateTime.now())));

        DocumentDetail detail = service.get(1L);

        assertThat(detail.filename()).isEqualTo("a.md");
        assertThat(detail.content()).isEqualTo("hello");
    }

    @Test
    void getThrowsWhenNotFound() {
        DocumentRepository repository = mock(DocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        DocumentService service = new DocumentService(repository, vectorStoreService);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void deleteRemovesRecordAndVectorBlocks() {
        DocumentRepository repository = mock(DocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        DocumentService service = new DocumentService(repository, vectorStoreService);

        DocumentEntity entity = new DocumentEntity(7L, "a.md", "text/markdown", 100L, 5, 2, "hello", LocalDateTime.now());
        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        service.delete(7L);

        verify(vectorStoreService).deleteByDocumentId(7L, 2);
        verify(repository).delete(entity);
    }
}
