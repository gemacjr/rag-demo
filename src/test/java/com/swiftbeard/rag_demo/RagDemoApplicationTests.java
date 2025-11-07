package com.swiftbeard.rag_demo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest
class RagDemoApplicationTests {

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient chatClient;

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
