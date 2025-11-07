package com.swiftbeard.rag_demo;

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
import org.springframework.beans.factory.annotation.Value;
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
        ragService = new RagService(chatClient, vectorStore);

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
                new Document("StarlightDB is a serverless graph database."),
                new Document("It is designed for real-time analytics."),
                new Document("It features quantum-leap query engine."),
                new Document("It provides Chrono-Sync for time-travel queries.")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(similarDocuments);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("StarlightDB is a serverless graph database designed for real-time analytics.");

        // When
        String result = ragService.retrieveAndGenerate(userMessage);

        // Then
        assertThat(result).isEqualTo("StarlightDB is a serverless graph database designed for real-time analytics.");

        // Verify similarity search was called with correct parameters
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo(userMessage);
        assertThat(capturedRequest.getTopK()).isEqualTo(4);

        // Verify prompt was sent to chat client
        verify(chatClient).prompt(any(Prompt.class));
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
        String result = ragService.retrieveAndGenerate(userMessage);

        // Then
        assertThat(result).isEqualTo("I don't know.");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    void retrieveAndGenerate_shouldUseTopK4ForRetrieval() {
        // Given
        String userMessage = "Tell me about the features";
        List<Document> documents = List.of(
                new Document("Feature 1"),
                new Document("Feature 2"),
                new Document("Feature 3"),
                new Document("Feature 4"),
                new Document("Feature 5")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(documents.subList(0, 4));
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Here are the features...");

        // When
        ragService.retrieveAndGenerate(userMessage);

        // Then
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest request = searchRequestCaptor.getValue();
        assertThat(request.getTopK()).isEqualTo(4);
    }

    @Test
    void retrieveAndGenerate_shouldCombineDocumentsWithNewlines() {
        // Given
        String userMessage = "What are the key points?";
        List<Document> documents = List.of(
                new Document("Point A is important."),
                new Document("Point B is crucial."),
                new Document("Point C is essential.")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(documents);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("The key points are A, B, and C.");

        // When
        String result = ragService.retrieveAndGenerate(userMessage);

        // Then
        assertThat(result).isNotNull();
        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    void retrieveAndGenerate_shouldPassUserMessageToVectorStore() {
        // Given
        String userMessage = "How does Nebula visualization work?";
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Nebula renders 3D graphs.")));
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Nebula visualization renders 3D graphs.");

        // When
        ragService.retrieveAndGenerate(userMessage);

        // Then
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        SearchRequest request = searchRequestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo(userMessage);
    }
}
