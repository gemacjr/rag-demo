package com.swiftbeard.rag_demo;

import com.swiftbeard.rag_demo.exception.DocumentNotFoundException;
import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.repository.DocumentMetadataRepository;
import com.swiftbeard.rag_demo.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private VectorStore vectorStore;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentMetadataRepository, vectorStore);
    }

    @Test
    void listAllDocuments_shouldReturnAllDocuments() {
        // Given
        List<DocumentMetadata> expectedDocuments = List.of(
                createDocumentMetadata(1L, "doc1.pdf"),
                createDocumentMetadata(2L, "doc2.txt")
        );
        when(documentMetadataRepository.findAll()).thenReturn(expectedDocuments);

        // When
        List<DocumentMetadata> result = documentService.listAllDocuments();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedDocuments);
        verify(documentMetadataRepository).findAll();
    }

    @Test
    void listAllDocuments_whenEmpty_shouldReturnEmptyList() {
        // Given
        when(documentMetadataRepository.findAll()).thenReturn(List.of());

        // When
        List<DocumentMetadata> result = documentService.listAllDocuments();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getDocumentById_shouldReturnDocument() {
        // Given
        DocumentMetadata expectedDocument = createDocumentMetadata(1L, "test.pdf");
        when(documentMetadataRepository.findById(1L)).thenReturn(Optional.of(expectedDocument));

        // When
        DocumentMetadata result = documentService.getDocumentById(1L);

        // Then
        assertThat(result).isEqualTo(expectedDocument);
        verify(documentMetadataRepository).findById(1L);
    }

    @Test
    void getDocumentById_whenNotFound_shouldThrowException() {
        // Given
        when(documentMetadataRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentService.getDocumentById(999L))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found with id: 999");
    }

    @Test
    void deleteDocument_shouldDeleteMetadata() {
        // Given
        DocumentMetadata document = createDocumentMetadata(1L, "test.pdf");
        when(documentMetadataRepository.findById(1L)).thenReturn(Optional.of(document));
        doNothing().when(documentMetadataRepository).delete(any(DocumentMetadata.class));

        // When
        documentService.deleteDocument(1L);

        // Then
        verify(documentMetadataRepository).findById(1L);
        verify(documentMetadataRepository).delete(document);
    }

    @Test
    void deleteDocument_whenNotFound_shouldThrowException() {
        // Given
        when(documentMetadataRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentService.deleteDocument(999L))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found with id: 999");

        verify(documentMetadataRepository).findById(999L);
        verify(documentMetadataRepository, never()).delete(any(DocumentMetadata.class));
    }

    @Test
    void getDocumentCount_shouldReturnCount() {
        // Given
        when(documentMetadataRepository.count()).thenReturn(5L);

        // When
        long count = documentService.getDocumentCount();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(documentMetadataRepository).count();
    }

    @Test
    void getDocumentCount_whenNoDocuments_shouldReturnZero() {
        // Given
        when(documentMetadataRepository.count()).thenReturn(0L);

        // When
        long count = documentService.getDocumentCount();

        // Then
        assertThat(count).isEqualTo(0L);
    }

    private DocumentMetadata createDocumentMetadata(Long id, String filename) {
        DocumentMetadata metadata = new DocumentMetadata(
                filename,
                "application/pdf",
                1024L,
                10
        );
        metadata.setId(id);
        metadata.setUploadedAt(LocalDateTime.now());
        return metadata;
    }
}
