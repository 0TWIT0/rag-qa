package com.example.rag.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.rag.dto.SearchResult;
import com.example.rag.service.VectorStoreService;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VectorStoreService vectorStoreService;

    @Test
    void searchesAndReturnsResults() throws Exception {
        when(vectorStoreService.search(eq("how to install"), anyInt()))
                .thenReturn(List.of(new SearchResult("Installation steps...", 0.92, "doc.md", 2)));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"how to install\",\"topK\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("how to install"))
                .andExpect(jsonPath("$.resultCount").value(1))
                .andExpect(jsonPath("$.results[0].text").value("Installation steps..."))
                .andExpect(jsonPath("$.results[0].score").value(0.92))
                .andExpect(jsonPath("$.results[0].sourceName").value("doc.md"))
                .andExpect(jsonPath("$.results[0].index").value(2));
    }

    @Test
    void defaultsTopKWhenMissing() throws Exception {
        when(vectorStoreService.search(eq("q"), eq(4))).thenReturn(List.of());

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"q\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCount").value(0));
    }
}
