package com.swiftbeard.rag_demo.repository;

import com.swiftbeard.rag_demo.model.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    // Find queries ordered by timestamp (most recent first)
    List<QueryHistory> findAllByOrderByTimestampDesc();

    // Find queries with pagination
    Page<QueryHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    // Find queries within a date range
    List<QueryHistory> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    // Count queries in a time period
    long countByTimestampAfter(LocalDateTime after);
}
