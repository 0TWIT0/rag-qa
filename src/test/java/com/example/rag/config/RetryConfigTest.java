package com.example.rag.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

class RetryConfigTest {

    /**
     * 核心用例：网络层异常（ResourceAccessException）必须被重试。
     *
     * <p>这正是 Spring AI 自动配置漏掉的那一类——不重试的话，网络抖动会直接
     * 变成用户看到的 500。</p>
     */
    @Test
    void retriesOnResourceAccessException() {
        RetryTemplate template = new RetryConfig().retryTemplate();
        AtomicInteger calls = new AtomicInteger();

        String result = template.execute(context -> {
            if (calls.incrementAndGet() < 3) {
                throw new ResourceAccessException("模拟网络抖动");
            }
            return "成功";
        });

        assertThat(result).isEqualTo("成功");
        assertThat(calls.get()).isEqualTo(3);
    }

    /**
     * 反向用例：非瞬时错误（比如 API key 错误）不该重试——重试只是白白多等几秒。
     */
    @Test
    void doesNotRetryNonTransientErrors() {
        RetryTemplate template = new RetryConfig().retryTemplate();
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> template.execute(context -> {
            calls.incrementAndGet();
            throw new NonTransientAiException("模拟 key 无效");
        })).isInstanceOf(NonTransientAiException.class);

        assertThat(calls.get()).isEqualTo(1);
    }
}
