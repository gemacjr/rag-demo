package com.swiftbeard.rag_demo;

import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.repository.DocumentMetadataRepository;
import com.swiftbeard.rag_demo.service.DocumentUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Captor
    private ArgumentCaptor<List<Document>> documentCaptor;

    private DocumentUploadService documentUploadService;

    @BeforeEach
    void setUp() {
        documentUploadService = new DocumentUploadService(vectorStore, documentMetadataRepository);

        // Mock the save operation to return a document with an ID (lenient for tests that throw early)
        lenient().when(documentMetadataRepository.save(any(DocumentMetadata.class))).thenAnswer(invocation -> {
            DocumentMetadata metadata = invocation.getArgument(0);
            metadata.setId(1L);
            return metadata;
        });
    }

    @Test
    void uploadDocument_withHtmlFile_shouldProcessAndStore() throws IOException {
        // Given
        String htmlContent = "<html><body><h1>Test Document</h1><p>This is a test paragraph.</p></body></html>";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.html",
                "text/html",
                htmlContent.getBytes()
        );

        doNothing().when(vectorStore).add(anyList());

        // When
        String result = documentUploadService.uploadDocument(file);

        // Then
        assertThat(result).contains("Successfully uploaded and processed");
        assertThat(result).contains("test.html");
        verify(vectorStore).add(anyList());
    }

    @Test
    void uploadDocument_withTextFile_shouldProcessAndStore() throws IOException {
        // Given
        String textContent = "This is a test document with some content that should be processed.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                textContent.getBytes()
        );

        doNothing().when(vectorStore).add(anyList());

        // When
        String result = documentUploadService.uploadDocument(file);

        // Then
        assertThat(result).contains("Successfully uploaded and processed");
        assertThat(result).contains("test.txt");
        verify(vectorStore).add(documentCaptor.capture());

        List<Document> capturedDocuments = documentCaptor.getValue();
        assertThat(capturedDocuments).isNotEmpty();
    }

    @Test
    void uploadDocument_withEmptyFile_shouldThrowException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // When/Then
        assertThatThrownBy(() -> documentUploadService.uploadDocument(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty");
    }

    @Test
    void uploadDocument_withDocxFile_shouldProcessAndStore() throws IOException {
        // Given
        byte[] docxContent = "Sample DOCX content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent
        );

        doNothing().when(vectorStore).add(anyList());

        // When
        String result = documentUploadService.uploadDocument(file);

        // Then
        assertThat(result).contains("Successfully uploaded and processed");
        assertThat(result).contains("test.docx");
        verify(vectorStore).add(anyList());
    }

    @Test
    void uploadDocument_withLargeTextFile_shouldSplitIntoChunks() throws IOException {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is sentence number ").append(i)
                    .append(". It contains some meaningful content. ");
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent.toString().getBytes()
        );

        doNothing().when(vectorStore).add(anyList());

        // When
        String result = documentUploadService.uploadDocument(file);

        // Then
        assertThat(result).contains("Successfully uploaded and processed");
        verify(vectorStore).add(documentCaptor.capture());

        List<Document> capturedDocuments = documentCaptor.getValue();
        assertThat(capturedDocuments).isNotEmpty();
        // Should be split into multiple chunks
        assertThat(capturedDocuments.size()).isGreaterThan(1);
    }
}
