# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.2.7 RAG (Retrieval Augmented Generation) demo application using Spring AI 1.0.3, OpenAI models, and PGVector for vector storage. The application demonstrates a complete RAG workflow: document loading, vector storage, similarity search, and LLM-based generation.

**Package**: `com.swiftbeard.rag_demo` (note: uses underscore, not hyphen)

## Build System

Uses Gradle with Java 21 toolchain.

### Essential Commands

**Build the project:**
```bash
./gradlew build
```

**Run the application:**
```bash
./gradlew bootRun
```

**Run tests:**
```bash
./gradlew test
```

**Run a single test class:**
```bash
./gradlew test --tests RagDemoApplicationTests
```

**Clean build:**
```bash
./gradlew clean build
```

## Architecture

### RAG Flow

1. **Document Loading (DocumentLoader)**: Implements `CommandLineRunner` to load documents into the vector store at startup. Currently loads hardcoded StarlightDB documentation.

2. **Vector Storage (VectorStore)**: Uses PGVector with HNSW indexing and cosine distance similarity. Configured for 768-dimensional embeddings.

3. **Retrieval & Generation (RagService)**:
   - Performs similarity search against vector store with configurable top-K (default 4, range 1-20)
   - Augments prompt with retrieved document context
   - Sends augmented prompt to LLM for generation
   - Returns answer with source citations

4. **API Layer (RagController)**: REST endpoints for RAG queries, document upload, and document management.

5. **Document Management**:
   - DocumentMetadata entity tracks uploaded documents (filename, size, chunk count, timestamp)
   - DocumentUploadService processes and stores documents with metadata tagging
   - DocumentService provides CRUD operations for document metadata

6. **Query History**:
   - QueryHistory entity stores all queries (query, answer, topK, sourceCount, executionTime, timestamp)
   - Automatically saved after each RAG query execution
   - QueryHistoryService provides retrieval, filtering, and deletion operations

### Key Components

- **RagService**: Core RAG logic - retrieval from vector store, prompt augmentation, LLM generation, source citations, query history tracking
- **RagResponse**: Response DTO containing answer and list of SourceCitation objects
- **SourceCitation**: Contains document ID, filename, content excerpt, and similarity score
- **MessageRequest**: Request DTO with message and optional topK parameter (validated range 1-20, default 4)
- **DocumentUploadService**: Handles document upload, parsing (PDF/Tika), chunking, and metadata storage
- **DocumentService**: Manages document metadata operations (list, get, delete, count)
- **DocumentMetadata**: JPA entity for document metadata storage
- **QueryHistoryService**: Manages query history operations (save, retrieve, filter, delete)
- **QueryHistory**: JPA entity storing query details, answers, and execution metrics
- **DocumentLoader**: Startup component that populates vector store with initial documents
- **RagController**: REST endpoints for all RAG, document, and query history operations

### Configuration (application.yaml)

- **Local embeddings server**: `http://127.0.0.1:1234` using `text-embedding-nomic-embed-text-v2-moe` model
- **LLM generation**: OpenRouter API with `google/gemini-2.5-flash` (requires `OPENROUTER_API_KEY` env var)
- **PostgreSQL**: Local database `rag_demo` on port 5432
- **PGVector**: HNSW index with cosine distance, 768 dimensions

### Prompt Template

Located at `src/main/resources/prompts/rag-prompt.st`. Uses Spring AI's SystemPromptTemplate with `{information}` placeholder for retrieved context.

## Dependencies

Key Spring AI components:
- `spring-ai-starter-model-openai`: Chat client interface
- `spring-ai-starter-vector-store-pgvector`: PGVector integration
- `spring-ai-advisors-vector-store`: Vector store utilities

## Development Setup Requirements

1. PostgreSQL with PGVector extension running on `localhost:5432`
2. Database `rag_demo` created with username `a267246`
3. Local embedding server running on port 1234
4. Environment variable `OPENROUTER_API_KEY` set for LLM generation

## Testing the RAG Endpoint

```bash
curl -X POST http://localhost:8080/ai/rag \
  -H "Content-Type: application/json" \
  -d '{"message": "What is StarlightDB?"}'
```
