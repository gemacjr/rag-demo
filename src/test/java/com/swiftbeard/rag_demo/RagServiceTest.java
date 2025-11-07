package com.swiftbeard.rag_demo;

import com.swiftbeard.rag_demo.model.RagResponse;
import com.swiftbeard.rag_demo.service.QueryHistoryService;
import com.swiftbeard.rag_demo.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private QueryHistoryService queryHistoryService;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RagService(chatClient, vectorStore, queryHistoryService);

        // Set up the prompt template resource
        String promptTemplate = "You are a helpful assistant. Use the following information to answer the question in detail.\n\n" +
                "Information:\n{information}\n\nAnswer:";
        Resource mockResource = new ByteArrayResource(promptTemplate.getBytes());
        ReflectionTestUtils.setField(ragService, "ragPromptTemplate", mockResource);
    }

    @Test
    void retrieveAndGenerate_shouldRetrieveDocumentsAndGenerateResponse() {
        // Given
        String userMessage = "What is StarlightDB?";
        List<Document> similarDocuments = List.of(
                createDocumentWithMetadata("StarlightDB is a serverless graph database.", "1", "doc1.pdf"),
                createDocumentWithMetadata("It is designed for real-time analytics.", "1", "doc1.pdf"),
                createDocumentWithMetadata("It features quantum-leap query engine.", "2", "doc2.pdf"),
                createDocumentWithMetadata("It provides Chrono-Sync for time-travel queries.", "2", "doc2.pdf")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(similarDocuments);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("StarlightDB is a serverless graph database designed for real-time analytics.");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 4);

        // Then
        assertThat(result.getAnswer()).isEqualTo("StarlightDB is a serverless graph database designed for real-time analytics.");
        assertThat(result.getSources()).hasSize(4);
        assertThat(result.getSourceCount()).isEqualTo(4);
        assertThat(result.getSources().get(0).getFilename()).isEqualTo("doc1.pdf");
        assertThat(result.getSources().get(0).getDocumentId()).isEqualTo("1");

        // Verify similarity search was called with correct parameters
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo(userMessage);
        assertThat(capturedRequest.getTopK()).isEqualTo(4);

        // Verify prompt was sent to chat client
        verify(chatClient).prompt(any(Prompt.class));
    }

    private Document createDocumentWithMetadata(String text, String documentId, String filename) {
        Document doc = new Document(text);
        doc.getMetadata().put("document_id", documentId);
        doc.getMetadata().put("filename", filename);
        return doc;
    }

    @Test
    void retrieveAndGenerate_withNoSimilarDocuments_shouldStillGenerateResponse() {
        // Given
        String userMessage = "What is quantum computing?";
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("I don't know.");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 4);

        // Then
        assertThat(result.getAnswer()).isEqualTo("I don't know.");
        assertThat(result.getSources()).isEmpty();
        assertThat(result.getSourceCount()).isEqualTo(0);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    void retrieveAndGenerate_shouldUseTopK4ForRetrieval() {
        // Given
        String userMessage = "Tell me about the features";
        List<Document> documents = List.of(
                createDocumentWithMetadata("Feature 1", "1", "features.txt"),
                createDocumentWithMetadata("Feature 2", "1", "features.txt"),
                createDocumentWithMetadata("Feature 3", "1", "features.txt"),
                createDocumentWithMetadata("Feature 4", "1", "features.txt")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(documents);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Here are the features...");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 4);

        // Then
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest request = searchRequestCaptor.getValue();
        assertThat(request.getTopK()).isEqualTo(4);
        assertThat(result.getSources()).hasSize(4);
    }

    @Test
    void retrieveAndGenerate_shouldCombineDocumentsWithNewlines() {
        // Given
        String userMessage = "What are the key points?";
        List<Document> documents = List.of(
                createDocumentWithMetadata("Point A is important.", "1", "points.txt"),
                createDocumentWithMetadata("Point B is crucial.", "1", "points.txt"),
                createDocumentWithMetadata("Point C is essential.", "1", "points.txt")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(documents);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("The key points are A, B, and C.");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 4);

        // Then
        assertThat(result.getAnswer()).isNotNull();
        assertThat(result.getSources()).hasSize(3);
        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    void retrieveAndGenerate_shouldPassUserMessageToVectorStore() {
        // Given
        String userMessage = "How does Nebula visualization work?";
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(createDocumentWithMetadata("Nebula renders 3D graphs.", "1", "nebula.txt")));
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Nebula visualization renders 3D graphs.");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 4);

        // Then
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest request = searchRequestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo(userMessage);
        assertThat(result.getSources()).hasSize(1);
        assertThat(result.getSources().get(0).getContent()).contains("Nebula renders 3D graphs");
    }

    @Test
    void retrieveAndGenerate_withCustomTopK_shouldUseSpecifiedValue() {
        // Given
        String userMessage = "Test with custom topK";
        List<Document> documents = List.of(
                createDocumentWithMetadata("Doc 1", "1", "test.txt"),
                createDocumentWithMetadata("Doc 2", "1", "test.txt"),
                createDocumentWithMetadata("Doc 3", "1", "test.txt"),
                createDocumentWithMetadata("Doc 4", "1", "test.txt"),
                createDocumentWithMetadata("Doc 5", "1", "test.txt"),
                createDocumentWithMetadata("Doc 6", "1", "test.txt")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(documents);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Test response");

        // When
        RagResponse result = ragService.retrieveAndGenerate(userMessage, 6);

        // Then
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest request = searchRequestCaptor.getValue();
        assertThat(request.getTopK()).isEqualTo(6);
        assertThat(result.getSources()).hasSize(6);
    }
}
