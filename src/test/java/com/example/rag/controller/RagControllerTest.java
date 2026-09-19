package com.example.rag.controller;

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

import com.example.rag.dto.AskResponse;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.RagService;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void askReturnsAnswerAndSources() throws Exception {
        when(ragService.ask(eq("如何安装"), eq(4)))
                .thenReturn(new AskResponse("如何安装", "安装步骤是……", 1,
                        List.of(new SearchResult("安装步骤", 0.9, "a.md", 0))));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"如何安装\",\"topK\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("如何安装"))
                .andExpect(jsonPath("$.answer").value("安装步骤是……"))
                .andExpect(jsonPath("$.sourceCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceName").value("a.md"))
                .andExpect(jsonPath("$.sources[0].index").value(0));
    }

    @Test
    void defaultsTopKWhenMissing() throws Exception {
        when(ragService.ask(eq("q"), eq(4)))
                .thenReturn(new AskResponse("q", "资料库中没有找到相关信息。", 0, List.of()));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"q\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("资料库中没有找到相关信息。"))
                .andExpect(jsonPath("$.sourceCount").value(0));
    }
}
