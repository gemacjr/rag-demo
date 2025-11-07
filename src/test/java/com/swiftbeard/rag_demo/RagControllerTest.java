package com.swiftbeard.rag_demo;

import com.swiftbeard.rag_demo.controller.RagController;
import com.swiftbeard.rag_demo.exception.DocumentNotFoundException;
import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.model.MessageRequest;
import com.swiftbeard.rag_demo.model.RagResponse;
import com.swiftbeard.rag_demo.model.SourceCitation;
import com.swiftbeard.rag_demo.service.DocumentService;
import com.swiftbeard.rag_demo.service.DocumentUploadService;
import com.swiftbeard.rag_demo.service.QueryHistoryService;
import com.swiftbeard.rag_demo.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private DocumentUploadService documentUploadService;

    @Mock
    private DocumentService documentService;

    @Mock
    private QueryHistoryService queryHistoryService;

    private RagController ragController;

    @BeforeEach
    void setUp() {
        ragController = new RagController(ragService, documentUploadService, documentService, queryHistoryService);
    }

    @Test
    void generate_shouldReturnResponseFromRagService() {
        // Given
        MessageRequest request = new MessageRequest("What is StarlightDB?");
        RagResponse expectedResponse = new RagResponse(
                "StarlightDB is a serverless graph database.",
                List.of(new SourceCitation("1", "doc.pdf", "Content", null))
        );
        when(ragService.retrieveAndGenerate(request.message(), 4)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo("StarlightDB is a serverless graph database.");
        assertThat(response.getBody().getSources()).hasSize(1);
        verify(ragService).retrieveAndGenerate(request.message(), 4);
    }

    @Test
    void generate_withDifferentMessage_shouldCallRagService() {
        // Given
        MessageRequest request = new MessageRequest("How does Chrono-Sync work?");
        RagResponse expectedResponse = new RagResponse(
                "Chrono-Sync allows time-travel queries.",
                List.of()
        );
        when(ragService.retrieveAndGenerate(request.message(), 4)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo("Chrono-Sync allows time-travel queries.");
        verify(ragService).retrieveAndGenerate("How does Chrono-Sync work?", 4);
    }

    @Test
    void uploadDocument_withValidFile_shouldReturnOkResponse() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );
        String expectedMessage = "Successfully uploaded and processed 5 document chunks from test.pdf";
        when(documentUploadService.uploadDocument(any(MultipartFile.class)))
                .thenReturn(expectedMessage);

        // When
        ResponseEntity<String> response = ragController.uploadDocument(file);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
        verify(documentUploadService).uploadDocument(file);
    }

    @Test
    void uploadDocument_withEmptyFile_shouldReturnBadRequest() throws IOException {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );
        when(documentUploadService.uploadDocument(any(MultipartFile.class)))
                .thenThrow(new IllegalArgumentException("File is empty"));

        // When
        ResponseEntity<String> response = ragController.uploadDocument(emptyFile);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("File is empty");
    }

    @Test
    void uploadDocument_withIOException_shouldReturnInternalServerError() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupted.pdf",
                "application/pdf",
                "corrupted data".getBytes()
        );
        when(documentUploadService.uploadDocument(any(MultipartFile.class)))
                .thenThrow(new IOException("Cannot read file"));

        // When
        ResponseEntity<String> response = ragController.uploadDocument(file);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Error processing file");
        assertThat(response.getBody()).contains("Cannot read file");
    }

    @Test
    void uploadDocument_withTextFile_shouldProcessSuccessfully() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "This is a text document.".getBytes()
        );
        String expectedMessage = "Successfully uploaded and processed 1 document chunks from document.txt";
        when(documentUploadService.uploadDocument(any(MultipartFile.class)))
                .thenReturn(expectedMessage);

        // When
        ResponseEntity<String> response = ragController.uploadDocument(file);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void uploadDocument_withDocxFile_shouldProcessSuccessfully() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes()
        );
        String expectedMessage = "Successfully uploaded and processed 3 document chunks from document.docx";
        when(documentUploadService.uploadDocument(any(MultipartFile.class)))
                .thenReturn(expectedMessage);

        // When
        ResponseEntity<String> response = ragController.uploadDocument(file);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Successfully uploaded");
        assertThat(response.getBody()).contains("document.docx");
    }

    @Test
    void generate_withEmptyMessage_shouldStillCallService() {
        // Given
        MessageRequest request = new MessageRequest("");
        RagResponse expectedResponse = new RagResponse("Please provide a question.", List.of());
        when(ragService.retrieveAndGenerate("", 4)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo("Please provide a question.");
        verify(ragService).retrieveAndGenerate("", 4);
    }

    @Test
    void generate_withLongMessage_shouldHandleCorrectly() {
        // Given
        String longMessage = "This is a very long message. ".repeat(100);
        MessageRequest request = new MessageRequest(longMessage);
        RagResponse expectedResponse = new RagResponse("Here is a detailed response.", List.of());
        when(ragService.retrieveAndGenerate(longMessage, 4)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo("Here is a detailed response.");
        verify(ragService).retrieveAndGenerate(longMessage, 4);
    }

    @Test
    void generate_withCustomTopK_shouldUseSpecifiedValue() {
        // Given
        MessageRequest request = new MessageRequest("Test query", 10);
        RagResponse expectedResponse = new RagResponse("Response with 10 sources", List.of());
        when(ragService.retrieveAndGenerate("Test query", 10)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(ragService).retrieveAndGenerate("Test query", 10);
    }

    @Test
    void generate_withTopKAboveMax_shouldCapAt20() {
        // Given
        MessageRequest request = new MessageRequest("Test query", 100);
        RagResponse expectedResponse = new RagResponse("Response capped at 20", List.of());
        when(ragService.retrieveAndGenerate("Test query", 20)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(ragService).retrieveAndGenerate("Test query", 20);
    }

    @Test
    void generate_withTopKBelowMin_shouldSetTo1() {
        // Given
        MessageRequest request = new MessageRequest("Test query", -5);
        RagResponse expectedResponse = new RagResponse("Response with min 1", List.of());
        when(ragService.retrieveAndGenerate("Test query", 1)).thenReturn(expectedResponse);

        // When
        ResponseEntity<RagResponse> response = ragController.generate(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(ragService).retrieveAndGenerate("Test query", 1);
    }

    @Test
    void listDocuments_shouldReturnAllDocuments() {
        // Given
        List<DocumentMetadata> documents = List.of(
                createDocumentMetadata(1L, "doc1.pdf"),
                createDocumentMetadata(2L, "doc2.txt")
        );
        when(documentService.listAllDocuments()).thenReturn(documents);

        // When
        ResponseEntity<List<DocumentMetadata>> response = ragController.listDocuments();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(documents);
        verify(documentService).listAllDocuments();
    }

    @Test
    void getDocument_shouldReturnDocument() {
        // Given
        DocumentMetadata document = createDocumentMetadata(1L, "test.pdf");
        when(documentService.getDocumentById(1L)).thenReturn(document);

        // When
        ResponseEntity<DocumentMetadata> response = ragController.getDocument(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(document);
        verify(documentService).getDocumentById(1L);
    }

    @Test
    void getDocument_whenNotFound_shouldReturnNotFound() {
        // Given
        when(documentService.getDocumentById(999L))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        // When
        ResponseEntity<DocumentMetadata> response = ragController.getDocument(999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteDocument_shouldDeleteSuccessfully() {
        // Given
        // No exception thrown means successful deletion

        // When
        ResponseEntity<String> response = ragController.deleteDocument(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Document deleted successfully");
        verify(documentService).deleteDocument(1L);
    }

    @Test
    void deleteDocument_whenNotFound_shouldReturnNotFound() {
        // Given
        doThrow(new DocumentNotFoundException("Document not found with id: 999"))
                .when(documentService).deleteDocument(999L);

        // When
        ResponseEntity<String> response = ragController.deleteDocument(999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Document not found with id: 999");
    }

    @Test
    void getDocumentCount_shouldReturnCount() {
        // Given
        when(documentService.getDocumentCount()).thenReturn(10L);

        // When
        ResponseEntity<Long> response = ragController.getDocumentCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(10L);
        verify(documentService).getDocumentCount();
    }

    private DocumentMetadata createDocumentMetadata(Long id, String filename) {
        DocumentMetadata metadata = new DocumentMetadata(
                filename,
                "application/pdf",
                1024L,
                5
        );
        metadata.setId(id);
        metadata.setUploadedAt(LocalDateTime.now());
        return metadata;
    }
}
