package com.example.rag.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.rag.dto.AskResponse;
import com.example.rag.dto.SearchResult;

/**
 * RAG 检索问答核心（阶段 5）：检索 top-K 相关块 → 拼进 prompt → 交给大模型生成带出处的回答。
 *
 * <p>严格模式：检索不到任何相关块时不调用大模型，直接返回「资料库中没有找到相关信息」，
 * 避免模型在没有依据的情况下自由发挥、产生幻觉。</p>
 */
@Service
public class RagService {

    private static final String NO_INFO_ANSWER = "资料库中没有找到相关信息。";

    private static final String SYSTEM_PROMPT = "你是一个严谨的知识库问答助手。请只根据下面提供的资料回答问题，"
            + "不要编造资料中没有的内容。如果资料中没有相关信息，请直接回答“资料中没有相关信息”。"
            + "回答时标注来源，写成「（来源：文件名）」。"
            // 前端按纯文本渲染答案，模型若输出 Markdown 标记会原样露出星号和井号。
            + "用纯文本回答，不要使用 Markdown 标记（如 **、#、- 等符号），也不要分点罗列。";

    private final VectorStoreService vectorStoreService;
    private final ChatClient chatClient;

    public RagService(VectorStoreService vectorStoreService, ChatClient.Builder builder) {
        this.vectorStoreService = vectorStoreService;
        this.chatClient = builder.build();
    }

    /** 提问：检索 → 拼 prompt → 大模型回答，返回回答与引用来源。 */
    public AskResponse ask(String question, int topK) {
        List<SearchResult> sources = vectorStoreService.search(question, topK);
        if (sources.isEmpty()) {
            return new AskResponse(question, NO_INFO_ANSWER, 0, List.of());
        }
        String context = buildContext(sources);
        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(question, context))
                .call()
                .content();
        return new AskResponse(question, answer, sources.size(), sources);
    }

    /** 把 top-K 块拼成给模型看的参考资料。块号用 1 起，与前端「第 N 块」的显示保持一致。 */
    private String buildContext(List<SearchResult> sources) {
        StringBuilder sb = new StringBuilder();
        int no = 1;
        for (SearchResult s : sources) {
            sb.append("【资料 ").append(no++).append("】来源：").append(s.sourceName())
                    .append("（第 ").append(s.index() + 1).append(" 块）\n")
                    .append(s.text()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildUserPrompt(String question, String context) {
        return "请根据下面的资料回答用户问题。\n\n用户问题：" + question + "\n\n参考资料：\n" + context;
    }
}
