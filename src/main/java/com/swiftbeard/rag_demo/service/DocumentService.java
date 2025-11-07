package com.swiftbeard.rag_demo.service;

import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.exception.DocumentNotFoundException;
import com.swiftbeard.rag_demo.repository.DocumentMetadataRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentMetadataRepository documentMetadataRepository;
    private final VectorStore vectorStore;

    public DocumentService(DocumentMetadataRepository documentMetadataRepository,
                          VectorStore vectorStore) {
        this.documentMetadataRepository = documentMetadataRepository;
        this.vectorStore = vectorStore;
    }

    public List<DocumentMetadata> listAllDocuments() {
        return documentMetadataRepository.findAll();
    }

    public DocumentMetadata getDocumentById(Long id) {
        return documentMetadataRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));
    }

    @Transactional
    public void deleteDocument(Long id) {
        DocumentMetadata document = getDocumentById(id);

        // Note: Vector store deletion by metadata filter is not universally supported
        // In a production system, you would either:
        // 1. Store document IDs with vector embeddings and use a custom delete query
        // 2. Implement a background cleanup job
        // 3. Use a vector store implementation that supports metadata-based deletion
        // For now, we only delete the metadata entry
        // The vector embeddings will remain but won't be returned in document list

        // Delete metadata from database
        documentMetadataRepository.delete(document);
    }

    public long getDocumentCount() {
        return documentMetadataRepository.count();
    }
}
