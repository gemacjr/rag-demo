package com.swiftbeard.rag_demo.service;


import com.swiftbeard.rag_demo.model.RagResponse;
import com.swiftbeard.rag_demo.model.SourceCitation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final QueryHistoryService queryHistoryService;

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    public RagService(ChatClient chatClient, VectorStore vectorStore, QueryHistoryService queryHistoryService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.queryHistoryService = queryHistoryService;
    }

    public RagResponse retrieveAndGenerate(final String message, final int topK) {
        long startTime = System.currentTimeMillis();

        // 1. Retrieve similar documents
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(topK)
                        .build()
        );

        String information = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 2. Augment the prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(ragPromptTemplate);
        Prompt prompt = new Prompt(List.of(
                systemPromptTemplate.createMessage(Map.of("information", information)),
                new UserMessage(message)));

        // 3. Generate the response
        String answer = chatClient.prompt(prompt).call().content();

        // 4. Build source citations
        List<SourceCitation> sources = similarDocuments.stream()
                .map(doc -> {
                    String documentId = doc.getMetadata().getOrDefault("document_id", "unknown").toString();
                    String filename = doc.getMetadata().getOrDefault("filename", "unknown").toString();
                    String content = doc.getText();
                    // Truncate content to first 200 characters for citation
                    String truncatedContent = content.length() > 200
                            ? content.substring(0, 200) + "..."
                            : content;

                    // Get similarity score if available (may not be in all implementations)
                    Double score = doc.getMetadata().containsKey("distance")
                            ? (Double) doc.getMetadata().get("distance")
                            : null;

                    return new SourceCitation(documentId, filename, truncatedContent, score);
                })
                .collect(Collectors.toList());

        // 5. Save query to history
        long executionTime = System.currentTimeMillis() - startTime;
        queryHistoryService.saveQuery(message, answer, topK, sources.size(), executionTime);

        return new RagResponse(answer, sources);
    }
}
