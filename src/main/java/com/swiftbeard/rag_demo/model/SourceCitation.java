package com.swiftbeard.rag_demo.model;

public class SourceCitation {

    private String documentId;
    private String filename;
    private String content;
    private Double similarityScore;

    public SourceCitation() {
    }

    public SourceCitation(String documentId, String filename, String content, Double similarityScore) {
        this.documentId = documentId;
        this.filename = filename;
        this.content = content;
        this.similarityScore = similarityScore;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }
}
