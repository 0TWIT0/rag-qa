package com.example.rag.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocumentReaderTest {

    private final DocumentReader reader = new DocumentReader();

    @Test
    void extractsTxt() {
        String text = reader.extract("你好，世界".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertThat(text).isEqualTo("你好，世界");
    }

    @Test
    void extractsMarkdown() {
        String text = reader.extract("# 标题\n正文".getBytes(StandardCharsets.UTF_8), "b.md");
        assertThat(text).contains("标题", "正文");
    }

    @Test
    void stripsUtf8Bom() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "你好".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);

        String text = reader.extract(withBom, "bom.txt");
        assertThat(text).isEqualTo("你好");
    }

    @Test
    void extractsDocx() throws Exception {
        byte[] bytes;
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Hello from Word");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            bytes = out.toByteArray();
        }

        String text = reader.extract(bytes, "c.docx");
        assertThat(text).contains("Hello from Word");
    }

    @Test
    void extractsPdf() throws Exception {
        byte[] bytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Hello PDF");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            bytes = out.toByteArray();
        }

        String text = reader.extract(bytes, "d.pdf");
        assertThat(text).contains("Hello PDF");
    }

    @Test
    void rejectsUnsupportedType() {
        assertThatThrownBy(() -> reader.extract("x".getBytes(StandardCharsets.UTF_8), "e.exe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("暂不支持");
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> reader.extract(new byte[0], "f.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
