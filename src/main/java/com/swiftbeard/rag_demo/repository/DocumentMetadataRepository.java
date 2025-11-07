package com.swiftbeard.rag_demo.repository;

import com.swiftbeard.rag_demo.model.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    Optional<DocumentMetadata> findByFilename(String filename);

    boolean existsByFilename(String filename);
}
