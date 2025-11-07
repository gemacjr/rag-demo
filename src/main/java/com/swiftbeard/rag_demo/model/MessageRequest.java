package com.swiftbeard.rag_demo.model;

public record MessageRequest(String message, Integer topK) {

    // Constructor with default topK value
    public MessageRequest(String message) {
        this(message, null);
    }

    // Validation method
    public Integer getValidatedTopK() {
        if (topK == null) {
            return 4; // Default value
        }
        if (topK < 1) {
            return 1; // Minimum value
        }
        if (topK > 20) {
            return 20; // Maximum value
        }
        return topK;
    }
}
