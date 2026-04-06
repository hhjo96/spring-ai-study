package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookCreateRequest;
import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 도서 관리 서비스
 *
 * RDB 기반 도서 CRUD + VectorStore 동기화
 */
@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookService(BookRepository bookRepository, VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    /** 전체 도서 목록 조회 */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /** 장르별 도서 목록 조회 */
    public List<Book> getBooksByGenre(String genre) {
        return bookRepository.findByGenre(genre);
    }

    /** 도서 상세 조회 */
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서를 찾을 수 없습니다. id=" + id));
    }

    // ========================================================================
    // TODO 8: 도서 등록 시 RDB + VectorStore 동시 저장
    // ========================================================================
    /**
     * 새 도서를 등록하고, VectorStore에도 동기화합니다.
     *
     * [요구사항]
     * 1) BookCreateRequest로부터 Book 엔티티를 생성하여 RDB에 저장
     *    - price, stock 포함
     * 2) 저장된 Book을 VectorStore Document로 변환하여 VectorStore에 추가
     * 3) RDB에 저장된 Book 엔티티를 반환
     *
     * [힌트]
     * 1) Book 엔티티 생성:
     *    Book book = Book.builder()
     *        .title(request.title())
     *        .author(request.author())
     *        .genre(request.genre())
     *        .description(request.description())
     *        .rating(request.rating())
     *        .publishedYear(request.publishedYear())
     *        .isbn(request.isbn())
     *        .price(request.price() != null ? request.price() : 0)
     *        .stock(request.stock() != null ? request.stock() : 0)
     *        .build();
     *
     * 2) RDB 저장: Book saved = bookRepository.save(book);
     * 3) VectorStore 저장: vectorStore.add(List.of(toDocument(saved)));
     * 4) 반환: saved
     */
    public Book registerBook(BookCreateRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 8: 도서 등록 + VectorStore 동기화를 구현하세요");
    }

    /** Book → VectorStore Document 변환 헬퍼 */
    private Document toDocument(Book book) {
        Map<String, Object> metadata = Map.of(
                "bookId", book.getId(),
                "title", book.getTitle(),
                "author", book.getAuthor(),
                "genre", book.getGenre(),
                "rating", book.getRating(),
                "publishedYear", book.getPublishedYear(),
                "isbn", book.getIsbn() != null ? book.getIsbn() : "",
                "price", book.getPrice(),
                "stock", book.getStock()
        );
        return new Document(book.toEmbeddingText(), metadata);
    }
}
