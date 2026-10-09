package com.example.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.example.rag.document.Chunk;
import com.example.rag.dto.SearchResult;

class VectorStoreServiceTest {

    @Test
    void ingestAddsDocumentsWithMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, 0.35);
        List<Chunk> chunks = List.of(
                new Chunk("第一块", 0, "a.md"),
                new Chunk("第二块", 1, "a.md"));

        int stored = service.ingest(chunks, 7L);

        assertThat(stored).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> docs = captor.getValue();
        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getText()).isEqualTo("第一块");
        assertThat(docs.get(0).getId()).isEqualTo("7-0");
        assertThat(docs.get(0).getMetadata())
                .containsEntry("sourceName", "a.md")
                .containsEntry("index", 0)
                .containsEntry("documentId", 7L);
        assertThat(docs.get(1).getId()).isEqualTo("7-1");
        assertThat(docs.get(1).getMetadata()).containsEntry("index", 1);
    }

    @Test
    void ingestEmptyReturnsZero() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, 0.35);

        assertThat(service.ingest(List.of(), 1L)).isZero();
        assertThat(service.ingest(null, 1L)).isZero();
    }

    @Test
    void deleteByDocumentIdDeletesVectorBlocks() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, 0.35);

        service.deleteByDocumentId(7L, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).delete(captor.capture());
        assertThat(captor.getValue()).containsExactly("7-0", "7-1", "7-2");
    }

    @Test
    void searchMapsResultsToDto() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, 0.35);
        Document d1 = Document.builder()
                .text("匹配块")
                .metadata(Map.of("sourceName", "a.md", "index", 3))
                .score(0.8)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1));

        List<SearchResult> results = service.search("查询词", 4);

        assertThat(results).hasSize(1);
        SearchResult r = results.get(0);
        assertThat(r.text()).isEqualTo("匹配块");
        assertThat(r.sourceName()).isEqualTo("a.md");
        assertThat(r.index()).isEqualTo(3);
        assertThat(r.score()).isEqualTo(0.8);
    }

    @Test
    void searchPassesConfiguredSimilarityThreshold() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, 0.35);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search("查询词", 4);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("查询词");
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.35);
    }
}
