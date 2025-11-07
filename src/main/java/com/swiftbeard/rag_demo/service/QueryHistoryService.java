package com.swiftbeard.rag_demo.service;

import com.swiftbeard.rag_demo.model.QueryHistory;
import com.swiftbeard.rag_demo.repository.QueryHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueryHistoryService {

    private final QueryHistoryRepository queryHistoryRepository;

    public QueryHistoryService(QueryHistoryRepository queryHistoryRepository) {
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @Transactional
    public QueryHistory saveQuery(String query, String answer, int topK, int sourceCount, Long executionTimeMs) {
        QueryHistory history = new QueryHistory(query, answer, topK, sourceCount, executionTimeMs);
        return queryHistoryRepository.save(history);
    }

    public List<QueryHistory> getAllQueries() {
        return queryHistoryRepository.findAllByOrderByTimestampDesc();
    }

    public List<QueryHistory> getRecentQueries(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return queryHistoryRepository.findAllByOrderByTimestampDesc(pageable).getContent();
    }

    public QueryHistory getQueryById(Long id) {
        return queryHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found with id: " + id));
    }

    public List<QueryHistory> getQueriesByDateRange(LocalDateTime start, LocalDateTime end) {
        return queryHistoryRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    public long getTotalQueryCount() {
        return queryHistoryRepository.count();
    }

    public long getRecentQueryCount(LocalDateTime since) {
        return queryHistoryRepository.countByTimestampAfter(since);
    }

    @Transactional
    public void deleteQuery(Long id) {
        queryHistoryRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllQueries() {
        queryHistoryRepository.deleteAll();
    }
}
