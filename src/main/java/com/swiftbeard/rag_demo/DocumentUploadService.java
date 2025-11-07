package com.swiftbeard.rag_demo;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentUploadService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public DocumentUploadService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter();
    }

    public String uploadDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        List<Document> documents = parseDocument(file, filename, contentType);

        // Split documents into smaller chunks for better retrieval
        List<Document> splitDocuments = textSplitter.apply(documents);

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
