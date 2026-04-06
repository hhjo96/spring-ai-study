package com.study.bookadvisor.service;

import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 시작 시 RDB → VectorStore 동기화
 */
@Slf4j
@Component
public class BookDataInitializer implements ApplicationRunner {

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookDataInitializer(BookRepository bookRepository, VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Book> books = bookRepository.findAll();

        if (books.isEmpty()) {
            log.warn("RDB에 도서 데이터가 없습니다. Docker init 스크립트를 확인하세요.");
            return;
        }

        log.info("=== RDB → VectorStore 도서 데이터 동기화 시작 ({} 권) ===", books.size());

        List<Document> documents = books.stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);

        log.info("=== VectorStore 도서 데이터 동기화 완료 ({} 건) ===", documents.size());
    }

    private Document toDocument(Book book) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookId", book.getId());
        metadata.put("title", book.getTitle());
        metadata.put("author", book.getAuthor());
        metadata.put("genre", book.getGenre());
        metadata.put("rating", book.getRating());
        metadata.put("publishedYear", book.getPublishedYear());
        metadata.put("isbn", book.getIsbn() != null ? book.getIsbn() : "");
        metadata.put("price", book.getPrice());
        metadata.put("stock", book.getStock());

        return new Document(book.toEmbeddingText(), metadata);
    }
}
