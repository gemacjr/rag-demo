package com.swiftbeard.rag_demo.service;

import com.swiftbeard.rag_demo.model.DocumentMetadata;
import com.swiftbeard.rag_demo.repository.DocumentMetadataRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentUploadService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final DocumentMetadataRepository documentMetadataRepository;

    public DocumentUploadService(VectorStore vectorStore,
                                DocumentMetadataRepository documentMetadataRepository) {
        this.vectorStore = vectorStore;
        this.documentMetadataRepository = documentMetadataRepository;
        this.textSplitter = new TokenTextSplitter();
    }

    @Transactional
    public String uploadDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        List<Document> documents = parseDocument(file, filename, contentType);

        // Split documents into smaller chunks for better retrieval
        List<Document> splitDocuments = textSplitter.apply(documents);

        // Save metadata to database first to get an ID
        DocumentMetadata metadata = new DocumentMetadata(
                filename,
                contentType,
                fileSize,
                splitDocuments.size()
        );
        metadata = documentMetadataRepository.save(metadata);

        // Tag each chunk with the document ID for later deletion
        final Long documentId = metadata.getId();
        splitDocuments.forEach(doc -> {
            Map<String, Object> docMetadata = new HashMap<>(doc.getMetadata());
            docMetadata.put("document_id", documentId.toString());
            docMetadata.put("filename", filename);
            doc.getMetadata().putAll(docMetadata);
        });

        // Add to vector store
        vectorStore.add(splitDocuments);

        return String.format("Successfully uploaded and processed %d document chunks from %s",
                           splitDocuments.size(), filename);
    }

    private List<Document> parseDocument(MultipartFile file, String filename, String contentType)
            throws IOException {

        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        // Use PDF reader for PDF files
        if (contentType != null && contentType.equals("application/pdf")) {
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
            return pdfReader.get();
        }

        // Use Tika reader for other file types (txt, docx, html, etc.)
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        return tikaReader.get();
    }
}
