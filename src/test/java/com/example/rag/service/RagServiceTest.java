package com.example.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import com.example.rag.dto.AskResponse;
import com.example.rag.dto.SearchResult;

class RagServiceTest {

    @Test
    void askReturnsAnswerWithSources() {
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(vectorStoreService.search(eq("如何安装"), anyInt()))
                .thenReturn(List.of(
                        new SearchResult("安装步骤：先下载再解压", 0.9, "a.md", 0),
                        new SearchResult("卸载方法：直接删除目录", 0.8, "a.md", 1)));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("根据资料，安装步骤是先下载再解压。");

        RagService ragService = new RagService(vectorStoreService, builderReturning(chatClient));

        AskResponse response = ragService.ask("如何安装", 4);

        assertThat(response.answer()).isEqualTo("根据资料，安装步骤是先下载再解压。");
        assertThat(response.sourceCount()).isEqualTo(2);
        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().get(0).sourceName()).isEqualTo("a.md");

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(userPromptCaptor.capture());
        String userPrompt = userPromptCaptor.getValue();
        assertThat(userPrompt).contains("如何安装").contains("来源：a.md").contains("安装步骤：先下载再解压");
    }

    @Test
    void askReturnsNoInfoWhenNoResultsAndDoesNotCallLlm() {
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        ChatClient chatClient = mock(ChatClient.class);

        when(vectorStoreService.search(anyString(), anyInt())).thenReturn(List.of());

        RagService ragService = new RagService(vectorStoreService, builderReturning(chatClient));

        AskResponse response = ragService.ask("没有资料的问题", 4);

        assertThat(response.answer()).isEqualTo("资料库中没有找到相关信息。");
        assertThat(response.sourceCount()).isZero();
        assertThat(response.sources()).isEmpty();
        verifyNoInteractions(chatClient);
    }

    private static ChatClient.Builder builderReturning(ChatClient chatClient) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        return builder;
    }
}
