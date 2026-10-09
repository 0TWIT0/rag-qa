package com.example.rag.config;

import java.time.Duration;

import org.springframework.ai.retry.TransientAiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

/**
 * 大模型调用的重试策略。
 *
 * <p><b>为什么需要这个类：</b>Spring AI 的自动配置会自己建一个 {@link RetryTemplate}，
 * 但它只重试 {@link TransientAiException}（HTTP 错误码那一类），
 * <b>漏掉了 {@link ResourceAccessException}</b>——也就是"请求没能发出去或没收回来"
 * 这种网络层异常。结果是网络抖一下就直接把 500 抛给调用方，一次重试都不做。</p>
 *
 * <p>有意思的是 Spring AI 自己内部的 {@code RetryUtils.DEFAULT_RETRY_TEMPLATE}
 * 是包含 ResourceAccessException 的，只是自动配置那一版没有同步，这里把它补上。</p>
 *
 * <p>本类定义的同名 Bean 会取代自动配置的那个（它标了 {@code @ConditionalOnMissingBean}）。
 * 由于 Chat 和 Embedding 共用这个模板，DeepSeek 和 SiliconFlow 两边都受保护。</p>
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                // 首次调用 + 最多 3 次重试。
                // 这里刻意不用 Spring AI 默认的 10 次：那是偏批处理场景的取值，
                // 配合指数退避会一路涨到 180 秒，交互式问答可能卡上十几分钟。
                // 问答场景要的是"轻微抖动自动恢复"，不是"网络真断了也死等"。
                .maxAttempts(4)
                .retryOn(TransientAiException.class)
                .retryOn(ResourceAccessException.class)
                // 退避 1s → 2s → 4s，最坏情况总共多等约 7 秒。
                .exponentialBackoff(Duration.ofSeconds(1), 2.0, Duration.ofSeconds(8))
                .build();
    }
}
