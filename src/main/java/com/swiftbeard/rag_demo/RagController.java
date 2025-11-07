package com.swiftbeard.rag_demo;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class RagController {

    private final RagService ragService;
    private final DocumentUploadService documentUploadService;

    public RagController(RagService ragService, DocumentUploadService documentUploadService) {
        this.ragService = ragService;
        this.documentUploadService = documentUploadService;
    }

    @PostMapping("/ai/rag")
    public String generate(@RequestBody MessageRequest request) {
        return ragService.retrieveAndGenerate(request.message());
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
}
