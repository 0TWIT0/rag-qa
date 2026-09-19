package com.example.rag.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.rag.document.Chunk;
import com.example.rag.document.DocumentEntity;
import com.example.rag.document.DocumentReader;
import com.example.rag.document.TextSplitter;
import com.example.rag.dto.DocumentDetail;
import com.example.rag.dto.DocumentSummary;
import com.example.rag.service.DocumentService;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentReader reader;

    @MockitoBean
    private TextSplitter splitter;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void parsesUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        when(reader.extract(any(byte[].class), eq("note.txt"))).thenReturn("hello");
        when(splitter.split("hello", "note.txt"))
                .thenReturn(List.of(new Chunk("hello", 0, "note.txt")));

        mockMvc.perform(multipart("/api/documents/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("note.txt"))
                .andExpect(jsonPath("$.charCount").value(5))
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andExpect(jsonPath("$.chunks[0].text").value("hello"));
    }

    @Test
    void ingestsUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        when(reader.extract(any(byte[].class), eq("note.txt"))).thenReturn("hello");
        when(splitter.split("hello", "note.txt"))
                .thenReturn(List.of(new Chunk("hello", 0, "note.txt")));
        when(documentService.ingest(eq("note.txt"), eq("text/plain"), eq(5L), eq("hello"), any()))
                .thenReturn(new DocumentEntity(1L, "note.txt", "text/plain", 5L, 5, 1, "hello", LocalDateTime.now()));

        mockMvc.perform(multipart("/api/documents/ingest").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("note.txt"))
                .andExpect(jsonPath("$.charCount").value(5))
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andExpect(jsonPath("$.storedCount").value(1))
                .andExpect(jsonPath("$.documentId").value(1));
    }

    @Test
    void listsDocuments() throws Exception {
        when(documentService.list()).thenReturn(List.of(
                new DocumentSummary(1L, "a.md", "text/markdown", 100L, 5, 2, LocalDateTime.now())));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].filename").value("a.md"));
    }

    @Test
    void getsDocumentDetail() throws Exception {
        when(documentService.get(1L)).thenReturn(
                new DocumentDetail(1L, "a.md", "text/markdown", 100L, 5, 2, LocalDateTime.now(), "hello"));

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("a.md"))
                .andExpect(jsonPath("$.content").value("hello"));
    }

    @Test
    void deletesDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isOk());

        verify(documentService).delete(1L);
    }
}
