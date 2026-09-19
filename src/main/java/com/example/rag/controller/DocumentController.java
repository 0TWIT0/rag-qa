package com.example.rag.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.rag.document.Chunk;
import com.example.rag.document.DocumentEntity;
import com.example.rag.document.DocumentReader;
import com.example.rag.document.TextSplitter;
import com.example.rag.dto.DocumentDetail;
import com.example.rag.dto.DocumentParseResponse;
import com.example.rag.dto.DocumentSummary;
import com.example.rag.dto.IngestResponse;
import com.example.rag.service.DocumentService;

/**
 * 文档接口：阶段 3 解析 + 阶段 4 向量化入库 + 阶段 6 持久化与增删查。
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentReader reader;
    private final TextSplitter splitter;
    private final DocumentService documentService;

    public DocumentController(DocumentReader reader, TextSplitter splitter, DocumentService documentService) {
        this.reader = reader;
        this.splitter = splitter;
        this.documentService = documentService;
    }

    /** POST /api/documents/parse —— 上传文件，返回全文 + chunks（不向量化、不落库）。 */
    @PostMapping("/parse")
    public DocumentParseResponse parse(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        String text = extract(file);
        List<Chunk> chunks = splitter.split(text, filename);
        return new DocumentParseResponse(filename, text, text.length(), chunks.size(), chunks);
    }

    /** POST /api/documents/ingest —— 上传 → 解析 → 切块 → 落库 MySQL → 向量化入库。 */
    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        String text = extract(file);
        List<Chunk> chunks = splitter.split(text, filename);
        DocumentEntity entity = documentService.ingest(
                filename, file.getContentType(), file.getSize(), text, chunks);
        return new IngestResponse(filename, text.length(),
                entity.getChunkCount(), entity.getChunkCount(), entity.getId());
    }

    /** GET /api/documents —— 文档列表（摘要，不含全文）。 */
    @GetMapping
    public List<DocumentSummary> list() {
        return documentService.list();
    }

    /** GET /api/documents/{id} —— 文档详情（含全文）。 */
    @GetMapping("/{id}")
    public DocumentDetail get(@PathVariable Long id) {
        return documentService.get(id);
    }

    /** DELETE /api/documents/{id} —— 删除文档及其向量块。 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }

    private String extract(MultipartFile file) {
        try {
            return reader.extract(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败：" + e.getMessage(), e);
        }
    }
}
