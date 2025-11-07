package com.swiftbeard.rag_demo;

import com.swiftbeard.rag_demo.model.QueryHistory;
import com.swiftbeard.rag_demo.repository.QueryHistoryRepository;
import com.swiftbeard.rag_demo.service.QueryHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryHistoryServiceTest {

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @Captor
    private ArgumentCaptor<QueryHistory> queryHistoryCaptor;

    private QueryHistoryService queryHistoryService;

    @BeforeEach
    void setUp() {
        queryHistoryService = new QueryHistoryService(queryHistoryRepository);
    }

    @Test
    void saveQuery_shouldSaveQueryHistory() {
        // Given
        String query = "What is StarlightDB?";
        String answer = "A serverless graph database";
        int topK = 4;
        int sourceCount = 3;
        Long executionTime = 1500L;

        QueryHistory expectedHistory = new QueryHistory(query, answer, topK, sourceCount, executionTime);
        expectedHistory.setId(1L);
        when(queryHistoryRepository.save(any(QueryHistory.class))).thenReturn(expectedHistory);

        // When
        QueryHistory result = queryHistoryService.saveQuery(query, answer, topK, sourceCount, executionTime);

        // Then
        verify(queryHistoryRepository).save(queryHistoryCaptor.capture());
        QueryHistory captured = queryHistoryCaptor.getValue();
        assertThat(captured.getQuery()).isEqualTo(query);
        assertThat(captured.getAnswer()).isEqualTo(answer);
        assertThat(captured.getTopK()).isEqualTo(topK);
        assertThat(captured.getSourceCount()).isEqualTo(sourceCount);
        assertThat(captured.getExecutionTimeMs()).isEqualTo(executionTime);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAllQueries_shouldReturnAllQueries() {
        // Given
        List<QueryHistory> expectedQueries = List.of(
                createQueryHistory(1L, "Query 1"),
                createQueryHistory(2L, "Query 2")
        );
        when(queryHistoryRepository.findAllByOrderByTimestampDesc()).thenReturn(expectedQueries);

        // When
        List<QueryHistory> result = queryHistoryService.getAllQueries();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedQueries);
        verify(queryHistoryRepository).findAllByOrderByTimestampDesc();
    }

    @Test
    void getRecentQueries_shouldReturnLimitedQueries() {
        // Given
        List<QueryHistory> queries = List.of(
                createQueryHistory(1L, "Query 1"),
                createQueryHistory(2L, "Query 2"),
                createQueryHistory(3L, "Query 3")
        );
        Page<QueryHistory> page = new PageImpl<>(queries);
        when(queryHistoryRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(page);

        // When
        List<QueryHistory> result = queryHistoryService.getRecentQueries(10);

        // Then
        assertThat(result).hasSize(3);
        verify(queryHistoryRepository).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    void getQueryById_shouldReturnQuery() {
        // Given
        QueryHistory expectedQuery = createQueryHistory(1L, "Test query");
        when(queryHistoryRepository.findById(1L)).thenReturn(Optional.of(expectedQuery));

        // When
        QueryHistory result = queryHistoryService.getQueryById(1L);

        // Then
        assertThat(result).isEqualTo(expectedQuery);
        verify(queryHistoryRepository).findById(1L);
    }

    @Test
    void getQueryById_whenNotFound_shouldThrowException() {
        // Given
        when(queryHistoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> queryHistoryService.getQueryById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Query not found with id: 999");
    }

    @Test
    void getTotalQueryCount_shouldReturnCount() {
        // Given
        when(queryHistoryRepository.count()).thenReturn(15L);

        // When
        long count = queryHistoryService.getTotalQueryCount();

        // Then
        assertThat(count).isEqualTo(15L);
        verify(queryHistoryRepository).count();
    }

    @Test
    void getRecentQueryCount_shouldReturnCountSinceDate() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        when(queryHistoryRepository.countByTimestampAfter(since)).thenReturn(5L);

        // When
        long count = queryHistoryService.getRecentQueryCount(since);

        // Then
        assertThat(count).isEqualTo(5L);
        verify(queryHistoryRepository).countByTimestampAfter(since);
    }

    @Test
    void deleteQuery_shouldDeleteQuery() {
        // Given
        doNothing().when(queryHistoryRepository).deleteById(1L);

        // When
        queryHistoryService.deleteQuery(1L);

        // Then
        verify(queryHistoryRepository).deleteById(1L);
    }

    @Test
    void deleteAllQueries_shouldDeleteAll() {
        // Given
        doNothing().when(queryHistoryRepository).deleteAll();

        // When
        queryHistoryService.deleteAllQueries();

        // Then
        verify(queryHistoryRepository).deleteAll();
    }

    private QueryHistory createQueryHistory(Long id, String query) {
        QueryHistory history = new QueryHistory(query, "Answer", 4, 2, 1000L);
        history.setId(id);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }
}
