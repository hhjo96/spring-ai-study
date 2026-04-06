package com.study.bookadvisor.controller;

import com.study.bookadvisor.dto.*;
import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.entity.Event;
import com.study.bookadvisor.entity.Review;
import com.study.bookadvisor.repository.EventRepository;
import com.study.bookadvisor.repository.ReviewRepository;
import com.study.bookadvisor.service.BookAgentService;
import com.study.bookadvisor.service.BookSearchService;
import com.study.bookadvisor.service.BookService;
import com.study.bookadvisor.tool.BookReviewSearchTools;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookSearchService bookSearchService;
    private final BookAgentService bookAgentService;
    private final BookReviewSearchTools bookReviewSearchTools;
    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;

    // ── RDB 도서 목록 ──
    @GetMapping("/books")
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks().stream().map(this::toResponse).toList();
    }

    @GetMapping("/books/genre/{genre}")
    public List<BookResponse> getBooksByGenre(@PathVariable String genre) {
        return bookService.getBooksByGenre(genre).stream().map(this::toResponse).toList();
    }

    // ── 도서 상세 ──
    @GetMapping("/books/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return toResponse(bookService.getBookById(id));
    }

    // ── 도서 리뷰 (DB 저장된 리뷰) ──
    @GetMapping("/books/{id}/reviews")
    public List<ReviewResponse> getBookReviews(@PathVariable Long id) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(id).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    // ── 도서 이벤트 (진행 중인 이벤트) ──
    @GetMapping("/books/{id}/events")
    public List<EventResponse> getBookEvents(@PathVariable Long id) {
        return eventRepository.findByBookIdAndActiveTrue(id).stream()
                .map(this::toEventResponse)
                .toList();
    }

    // ── 인터넷 리뷰 검색 (Naver 검색 API 도구 활용) ──
    @GetMapping("/books/{id}/web-reviews")
    public String getBookWebReviews(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        return bookReviewSearchTools.searchBookReview(book.getTitle());
    }

    // ── 도서 등록 ──
    @PostMapping("/books")
    public BookResponse registerBook(@RequestBody BookCreateRequest request) {
        Book saved = bookService.registerBook(request);
        return toResponse(saved);
    }

    // ── 시맨틱 검색 ──
    @PostMapping("/search/simple")
    public List<BookSearchResult> searchSimple(@RequestBody SearchQueryRequest request) {
        return bookSearchService.searchSimple(request.query());
    }

    // ── AI 도서 추천 에이전트 채팅 ──
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = bookAgentService.chat(request.message(), request.conversationId());
        return new ChatResponse(answer, request.conversationId());
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getDescription(), book.getRating(),
                book.getPublishedYear(), book.getIsbn(),
                book.getPrice(), book.getStock());
    }

    private ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getId(), review.getBook().getId(),
                review.getReviewerName(), review.getContent(),
                review.getRating(), review.getSource(),
                review.getCreatedAt());
    }

    private EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(), event.getBook().getId(),
                event.getEventName(), event.getDescription(),
                event.getDiscountPercent(),
                event.getStartDate(), event.getEndDate(),
                event.getActive());
    }
}
