package com.swiftbeard.rag_demo.controller;


import com.swiftbeard.rag_demo.exception.DocumentNotFoundException;
import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.model.MessageRequest;
import com.swiftbeard.rag_demo.model.QueryHistory;
import com.swiftbeard.rag_demo.model.RagResponse;
import com.swiftbeard.rag_demo.service.DocumentService;
import com.swiftbeard.rag_demo.service.DocumentUploadService;
import com.swiftbeard.rag_demo.service.QueryHistoryService;
import com.swiftbeard.rag_demo.service.RagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class RagController {

    private final RagService ragService;
    private final DocumentUploadService documentUploadService;
    private final DocumentService documentService;
    private final QueryHistoryService queryHistoryService;

    public RagController(RagService ragService,
                        DocumentUploadService documentUploadService,
                        DocumentService documentService,
                        QueryHistoryService queryHistoryService) {
        this.ragService = ragService;
        this.documentUploadService = documentUploadService;
        this.documentService = documentService;
        this.queryHistoryService = queryHistoryService;
    }

    @PostMapping("/ai/rag")
    public ResponseEntity<RagResponse> generate(@RequestBody MessageRequest request) {
        int topK = request.getValidatedTopK();
        RagResponse response = ragService.retrieveAndGenerate(request.message(), topK);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            String result = documentUploadService.uploadDocument(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping("/ai/documents")
    public ResponseEntity<List<DocumentMetadata>> listDocuments() {
        List<DocumentMetadata> documents = documentService.listAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/ai/documents/{id}")
    public ResponseEntity<DocumentMetadata> getDocument(@PathVariable Long id) {
        try {
            DocumentMetadata document = documentService.getDocumentById(id);
            return ResponseEntity.ok(document);
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/ai/documents/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok("Document deleted successfully");
        } catch (DocumentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Document not found with id: " + id);
        }
    }

    @GetMapping("/ai/documents/count")
    public ResponseEntity<Long> getDocumentCount() {
        long count = documentService.getDocumentCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/ai/history")
    public ResponseEntity<List<QueryHistory>> getAllQueryHistory() {
        List<QueryHistory> history = queryHistoryService.getAllQueries();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/ai/history/recent")
    public ResponseEntity<List<QueryHistory>> getRecentQueryHistory(
            @RequestParam(defaultValue = "10") int limit) {
        List<QueryHistory> history = queryHistoryService.getRecentQueries(limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/ai/history/{id}")
    public ResponseEntity<QueryHistory> getQueryHistoryById(@PathVariable Long id) {
        try {
            QueryHistory history = queryHistoryService.getQueryById(id);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ai/history/count")
    public ResponseEntity<Long> getQueryHistoryCount() {
        long count = queryHistoryService.getTotalQueryCount();
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/ai/history/{id}")
    public ResponseEntity<String> deleteQueryHistory(@PathVariable Long id) {
        try {
            queryHistoryService.deleteQuery(id);
            return ResponseEntity.ok("Query history deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Query history not found with id: " + id);
        }
    }

    @DeleteMapping("/ai/history")
    public ResponseEntity<String> deleteAllQueryHistory() {
        queryHistoryService.deleteAllQueries();
        return ResponseEntity.ok("All query history deleted successfully");
    }
}
