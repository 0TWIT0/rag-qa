package com.example.rag.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口，用于验证服务已成功启动并能返回 JSON。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * GET /api/health —— 返回服务状态。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "rag-qa",
                "timestamp", Instant.now().toString()
        );
    }
}
