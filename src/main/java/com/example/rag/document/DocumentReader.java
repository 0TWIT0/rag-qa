package com.example.rag.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

/**
 * 从文档二进制内容中提取纯文本，按扩展名分发到对应的解析器。
 *
 * <p>支持：txt / md / markdown（直读，UTF-8）、pdf（PDFBox，仅文字型）、
 * docx（POI）。扫描件（图片型 PDF）需要 OCR，本阶段不做。</p>
 */
@Service
public class DocumentReader {

    public String extract(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".pdf")) {
            return extractPdf(content);
        }
        if (name.endsWith(".docx")) {
            return extractDocx(content);
        }
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
            return stripBom(new String(content, StandardCharsets.UTF_8));
        }
        throw new IllegalArgumentException("暂不支持的文件类型：" + filename);
    }

    private String extractPdf(byte[] content) {
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            throw new IllegalStateException("PDF 解析失败：" + e.getMessage(), e);
        }
    }

    private String extractDocx(byte[] content) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new IllegalStateException("Word 解析失败：" + e.getMessage(), e);
        }
    }

    /** Windows 记事本等工具保存的 txt 常带 UTF-8 BOM，剥离开头不可见字符。 */
    private String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.codePointAt(0) == 0xFEFF) {
            return text.substring(1);
        }
        return text;
    }
}
