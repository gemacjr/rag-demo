package com.swiftbeard.rag_demo;


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

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    public String retrieveAndGenerate(final String message) {

        // 1. Retrieve similar documents
        List<Document> similarDocuments = vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(4).build());
        String information = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 2. Augment the prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(ragPromptTemplate);
        Prompt prompt = new Prompt(List.of(
                systemPromptTemplate.createMessage(Map.of("information", information)),
                new UserMessage(message)));

        // 3. Generate the response
        return chatClient.prompt(prompt).call().content();
    }
}
