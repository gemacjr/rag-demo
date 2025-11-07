package com.swiftbeard.rag_demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentLoaderTest {

    @Mock
    private VectorStore vectorStore;

    @Captor
    private ArgumentCaptor<List<Document>> documentCaptor;

    private DocumentLoader documentLoader;

    @BeforeEach
    void setUp() {
        documentLoader = new DocumentLoader(vectorStore);
    }

    @Test
    void run_shouldLoadDocumentsIntoVectorStore() throws Exception {
        // When
        documentLoader.run();

        // Then
        verify(vectorStore).add(documentCaptor.capture());
        List<Document> capturedDocuments = documentCaptor.getValue();

        assertThat(capturedDocuments).hasSize(5);
        assertThat(capturedDocuments.get(0).getText())
                .contains("StarlightDB is a serverless graph database");
        assertThat(capturedDocuments.get(1).getText())
                .contains("Quantum-Leap");
        assertThat(capturedDocuments.get(2).getText())
                .contains("Chrono-Sync");
        assertThat(capturedDocuments.get(3).getText())
                .contains("Nebula");
        assertThat(capturedDocuments.get(4).getText())
                .contains("Cosmic Shield");
    }

    @Test
    void run_shouldLoadStarlightDBDocumentation() throws Exception {
        // When
        documentLoader.run();

        // Then
        verify(vectorStore).add(anyList());
    }

    @Test
    void run_shouldLoadExactlyFiveDocuments() throws Exception {
        // When
        documentLoader.run();

        // Then
        verify(vectorStore).add(documentCaptor.capture());
        List<Document> documents = documentCaptor.getValue();
        assertThat(documents).hasSize(5);
    }

    @Test
    void run_allDocumentsShouldContainStarlightDBContent() throws Exception {
        // When
        documentLoader.run();

        // Then
        verify(vectorStore).add(documentCaptor.capture());
        List<Document> documents = documentCaptor.getValue();

        documents.forEach(doc ->
                assertThat(doc.getText().toLowerCase()).contains("starlightdb")
        );
    }

    @Test
    void run_shouldLoadDocumentsWithUniqueContent() throws Exception {
        // When
        documentLoader.run();

        // Then
        verify(vectorStore).add(documentCaptor.capture());
        List<Document> documents = documentCaptor.getValue();

        // Verify each document has unique content
        assertThat(documents.stream().map(Document::getText).distinct().count())
                .isEqualTo(5);
    }
}
