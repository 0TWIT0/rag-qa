package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG-QA 应用入口。
 *
 * <p>阶段 1：仅提供一个可返回 JSON 的 REST 服务骨架；
 * 后续阶段将在此基础上接入大模型、文档切块、向量存储与检索问答。</p>
 */
@SpringBootApplication
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
