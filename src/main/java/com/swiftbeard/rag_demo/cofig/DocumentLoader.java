package com.swiftbeard.rag_demo.cofig;


import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentLoader implements CommandLineRunner {

    private final VectorStore vectorStore;

    public DocumentLoader(final VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    @Override
    public void run(String... args) throws Exception {
        List<Document> documents = List.of(
                new Document("StarlightDB is a serverless graph database designed for real-time analytics on complex, interconnected data."),
                new Document("The core of StarlightDB is its 'Quantum-Leap' query engine, which uses speculative execution to deliver query results up to 100x faster than traditional graph databases."),
                new Document("StarlightDB features 'Chrono-Sync' for effortless time-travel queries, allowing developers to query the state of their graph at any point in the past."),
                new Document("StarlightDB includes a built-in visualization tool called 'Nebula' that renders interactive 3D graphs directly within the development environment for easier analysis."),
                new Document("Security in StarlightDB is handled by 'Cosmic Shield', which provides end-to-end encryption and fine-grained access control at the node and edge level.")
        );
        vectorStore.add(documents);
        System.out.println("Documents loaded into VectorStore.");
    }
}
