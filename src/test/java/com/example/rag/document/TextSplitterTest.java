package com.example.rag.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter(10, 3);

    @Test
    void splitsLongTextIntoOverlappingChunks() {
        // 长度 20，chunkSize=10，overlap=3
        // 期望：["0123456789", "789ABCDEFG", "EFGHIJ"]
        List<Chunk> chunks = splitter.split("0123456789ABCDEFGHIJ", "test.txt");

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).text()).isEqualTo("0123456789");
        assertThat(chunks.get(1).text()).isEqualTo("789ABCDEFG");
        assertThat(chunks.get(2).text()).isEqualTo("EFGHIJ");
    }

    @Test
    void assignsIndexAndSourceName() {
        List<Chunk> chunks = splitter.split("0123456789ABCDEFGHIJ", "note.md");

        assertThat(chunks).extracting(Chunk::index).containsExactly(0, 1, 2);
        assertThat(chunks).allSatisfy(c -> assertThat(c.sourceName()).isEqualTo("note.md"));
    }

    @Test
    void shortTextBecomesSingleChunk() {
        List<Chunk> chunks = splitter.split("短文本", "a.txt");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("短文本");
    }

    @Test
    void emptyOrBlankTextReturnsNoChunks() {
        assertThat(splitter.split(null, "a.txt")).isEmpty();
        assertThat(splitter.split("", "a.txt")).isEmpty();
        assertThat(splitter.split("   \n  ", "a.txt")).isEmpty();
    }

    @Test
    void normalizesNewlinesAndSpaces() {
        // 短文本单块，避免重叠切块干扰断言
        String text = "a\r\n\r\n\r\nb   c";
        List<Chunk> chunks = splitter.split(text, "a.txt");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("a\n\nb c");
    }

    @Test
    void rejectsInvalidChunkSize() {
        assertThatThrownBy(() -> new TextSplitter(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk-size");
    }

    @Test
    void rejectsOverlapNotSmallerThanSize() {
        assertThatThrownBy(() -> new TextSplitter(10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk-overlap");
    }
}
