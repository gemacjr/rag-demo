# RAG Demo

A Spring Boot application demonstrating Retrieval Augmented Generation (RAG) using Spring AI, OpenAI models, and PostgreSQL with PGVector for vector storage.

## Features

- Document upload and processing (PDF, TXT, DOCX, HTML, and more)
- Automatic text extraction and chunking
- Vector embeddings using local embedding server
- Similarity search with PGVector (HNSW indexing)
- RAG-based question answering using LLM
- REST API for document upload and querying

## Architecture

The application implements a complete RAG pipeline:

1. **Document Upload**: Accepts various file formats via REST API
2. **Text Extraction**: Uses Spring AI document readers (PDF reader and Tika reader)
3. **Chunking**: Splits documents into smaller chunks using TokenTextSplitter
4. **Embedding**: Generates 768-dimensional embeddings using local embedding model
5. **Vector Storage**: Stores embeddings in PostgreSQL with PGVector extension
6. **Retrieval**: Performs similarity search to find relevant document chunks
7. **Generation**: Augments prompts with retrieved context and generates responses using LLM

## Prerequisites

- Java 21
- PostgreSQL with PGVector extension
- Local embedding server running on port 1234 (e.g., LM Studio)
- OpenRouter API key (for LLM generation)

## Setup

### 1. Database Setup

Create a PostgreSQL database with PGVector extension:

```sql
CREATE DATABASE rag_demo;
\c rag_demo
CREATE EXTENSION vector;
```

### 2. Environment Variables

Set the following environment variable:

```bash
export OPENROUTER_API_KEY=your_openrouter_api_key_here
```

### 3. Local Embedding Server

Ensure you have a local embedding server running on `http://127.0.0.1:1234` that supports the `text-embedding-nomic-embed-text-v2-moe` model. You can use [LM Studio](https://lmstudio.ai/) or similar tools.

### 4. Configuration

Update `src/main/resources/application.yaml` if needed:

- Database connection settings
- Embedding model configuration
- LLM model selection

### 5. Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`.

## API Endpoints

### Upload Document

Upload a document to be processed and added to the vector store.

**Endpoint**: `POST /ai/upload`

**Request**:
```bash
curl -X POST http://localhost:8080/ai/upload \
  -F "file=@/path/to/document.pdf"
```

**Supported file types**: PDF, TXT, DOCX, HTML, and other formats supported by Apache Tika

**Response**:
```
Successfully uploaded and processed 42 document chunks from document.pdf
```

### Query RAG System

Ask questions based on uploaded documents.

**Endpoint**: `POST /ai/rag`

**Request**:
```bash
curl -X POST http://localhost:8080/ai/rag \
  -H "Content-Type: application/json" \
  -d '{"message": "What is StarlightDB?"}'
```

**Response**:
```
StarlightDB is a serverless graph database designed for real-time analytics...
```

## Configuration Details

### Embedding Configuration

- **Model**: `text-embedding-nomic-embed-text-v2-moe`
- **Dimensions**: 768
- **Server**: Local server at `http://127.0.0.1:1234`

### LLM Configuration

- **Provider**: OpenRouter API
- **Model**: `google/gemini-2.5-flash`
- **API Key**: Set via `OPENROUTER_API_KEY` environment variable

### Vector Store Configuration

- **Database**: PostgreSQL with PGVector extension
- **Index Type**: HNSW (Hierarchical Navigable Small World)
- **Distance Type**: Cosine Distance
- **Dimensions**: 768
- **Top K**: 4 (retrieves top 4 similar documents)

## How It Works

### Document Upload Flow

1. User uploads a file via `/ai/upload` endpoint
2. `DocumentUploadService` detects the file type
3. Appropriate document reader (PDF or Tika) extracts text
4. `TokenTextSplitter` splits text into manageable chunks
5. Chunks are embedded and stored in PGVector

### RAG Query Flow

1. User sends a question to `/ai/rag` endpoint
2. `RagService` performs similarity search in vector store
3. Top 4 relevant document chunks are retrieved
4. System prompt is augmented with retrieved context
5. LLM generates answer based on context and question
6. Response is returned to user

## Project Structure

```
src/main/java/com/swiftbeard/rag_demo/
├── RagDemoApplication.java       # Main application entry point
├── RagController.java             # REST API endpoints
├── RagService.java                # RAG logic (retrieval + generation)
├── DocumentUploadService.java     # Document upload and processing
├── DocumentLoader.java            # Loads initial sample documents
└── MessageRequest.java            # Request DTO

src/main/resources/
├── application.yaml               # Application configuration
└── prompts/
    └── rag-prompt.st              # System prompt template
```

## Dependencies

- Spring Boot 3.2.7
- Spring AI 1.0.3
- PostgreSQL JDBC Driver
- Apache Tika (via Spring AI)

## Testing

Run tests:

```bash
./gradlew test
```

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests RagDemoApplicationTests
```

### Clean Build

```bash
./gradlew clean build
```

## Troubleshooting

### Database Connection Issues

Ensure PostgreSQL is running and the PGVector extension is installed:
```bash
psql -d rag_demo -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### Embedding Server Not Available

Verify the local embedding server is running on port 1234:
```bash
curl http://127.0.0.1:1234/v1/models
```

### OpenRouter API Issues

Verify your API key is set correctly:
```bash
echo $OPENROUTER_API_KEY
```

## License

This is a demonstration project.
