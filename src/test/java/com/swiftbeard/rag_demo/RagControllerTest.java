package com.swiftbeard.rag_demo;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private DocumentUploadService documentUploadService;

    private RagController ragController;

    @BeforeEach
    void setUp() {
        ragController = new RagController(ragService, documentUploadService);
    }

    @Test
    void generate_shouldReturnResponseFromRagService() {
        // Given
        MessageRequest request = new MessageRequest("What is StarlightDB?");
        String expectedResponse = "StarlightDB is a serverless graph database.";
        when(ragService.retrieveAndGenerate(request.message())).thenReturn(expectedResponse);

        // When
        String response = ragController.generate(request);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(ragService).retrieveAndGenerate(request.message());
    }

    @Test
    void generate_withDifferentMessage_shouldCallRagService() {
        // Given
        MessageRequest request = new MessageRequest("How does Chrono-Sync work?");
        String expectedResponse = "Chrono-Sync allows time-travel queries.";
        when(ragService.retrieveAndGenerate(request.message())).thenReturn(expectedResponse);

        // When
        String response = ragController.generate(request);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(ragService).retrieveAndGenerate("How does Chrono-Sync work?");
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
        when(ragService.retrieveAndGenerate("")).thenReturn("Please provide a question.");

        // When
        String response = ragController.generate(request);

        // Then
        assertThat(response).isEqualTo("Please provide a question.");
        verify(ragService).retrieveAndGenerate("");
    }

    @Test
    void generate_withLongMessage_shouldHandleCorrectly() {
        // Given
        String longMessage = "This is a very long message. ".repeat(100);
        MessageRequest request = new MessageRequest(longMessage);
        String expectedResponse = "Here is a detailed response.";
        when(ragService.retrieveAndGenerate(longMessage)).thenReturn(expectedResponse);

        // When
        String response = ragController.generate(request);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(ragService).retrieveAndGenerate(longMessage);
    }
}
