package com.swiftbeard.rag_demo.model;

import java.util.List;

public class RagResponse {

    private String answer;
    private List<SourceCitation> sources;
    private int sourceCount;

    public RagResponse() {
    }

    public RagResponse(String answer, List<SourceCitation> sources) {
        this.answer = answer;
        this.sources = sources;
        this.sourceCount = sources != null ? sources.size() : 0;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<SourceCitation> getSources() {
        return sources;
    }

    public void setSources(List<SourceCitation> sources) {
        this.sources = sources;
        this.sourceCount = sources != null ? sources.size() : 0;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }
}
