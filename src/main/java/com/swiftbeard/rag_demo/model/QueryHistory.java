package com.swiftbeard.rag_demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String query;

    @Column(nullable = false, length = 10000)
    private String answer;

    @Column(nullable = false)
    private Integer topK;

    @Column(nullable = false)
    private Integer sourceCount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Long executionTimeMs;

    // Default constructor required by JPA
    public QueryHistory() {
    }

    public QueryHistory(String query, String answer, Integer topK, Integer sourceCount, Long executionTimeMs) {
        this.query = query;
        this.answer = answer;
        this.topK = topK;
        this.sourceCount = sourceCount;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Integer getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(Integer sourceCount) {
        this.sourceCount = sourceCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
