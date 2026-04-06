package com.study.bookadvisor.tool;

import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.entity.Event;
import com.study.bookadvisor.repository.BookRepository;
import com.study.bookadvisor.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 도서 이벤트 조회 도구
 *
 * Event 테이블에서 도서에 해당하는 진행 중인 이벤트(할인, 무료 배송, 적립금 등)를 조회합니다.
 * LLM이 도서를 추천할 때, 현재 진행 중인 이벤트 혜택을 함께 안내합니다.
 *
 * [아키텍처]
 * 사용자 질문 → LLM 판단 → 이벤트 조회 도구 호출
 *   → BookRepository로 도서 검색 → EventRepository로 활성 이벤트 조회
 *   → 이벤트 정보 반환 → LLM이 추천 응답에 이벤트 정보를 포함
 *
 * [참고] 재고/가격은 BookInventoryTools에서 별도로 조회합니다.
 */
@Component
@Slf4j
public class BookEventTools {

    private final BookRepository bookRepository;
    private final EventRepository eventRepository;

    public BookEventTools(BookRepository bookRepository, EventRepository eventRepository) {
        this.bookRepository = bookRepository;
        this.eventRepository = eventRepository;
    }

    // ========================================================================
    // TODO 5: 도서 제목으로 진행 중인 이벤트 조회 도구
    // ========================================================================
    /**
     * 도서 제목으로 현재 진행 중인 이벤트를 조회합니다.
     *
     * [요구사항]
     * - bookRepository.findByTitleContainingIgnoreCase()를 사용하여 도서 검색
     * - eventRepository.findByBookIdAndActiveTrue()로 진행 중인 이벤트 목록 조회
     * - 이벤트가 있으면 각 이벤트의 이름, 설명, 할인율, 기간을 포함한 문자열 반환
     * - 이벤트가 없으면 "'{title}' 도서에 현재 진행 중인 이벤트가 없습니다." 반환
     * - 도서를 찾을 수 없으면 "'{title}' 도서를 찾을 수 없습니다." 반환
     *
     * [힌트]
     * 1) 도서 조회:
     *    Optional<Book> bookOpt = bookRepository.findByTitleContainingIgnoreCase(title);
     *
     * 2) 이벤트 조회:
     *    List<Event> events = eventRepository.findByBookIdAndActiveTrue(book.getId());
     *
     * 3) 이벤트 정보 조합:
     *    StringBuilder sb = new StringBuilder();
     *    sb.append("'").append(book.getTitle()).append("' 도서의 진행 중인 이벤트:\n");
     *    events.forEach(e -> {
     *        sb.append("- ").append(e.getEventName())
     *            .append(": ").append(e.getDescription());
     *        if (e.getDiscountPercent() != null) {
     *            sb.append(" (").append(e.getDiscountPercent()).append("% 할인)");
     *        }
     *        sb.append(" | 기간: ").append(e.getStartDate()).append(" ~ ").append(e.getEndDate());
     *        sb.append("\n");
     *    });
     *
     * @param title 조회할 도서 제목 (부분 일치 검색)
     * @return 이벤트 정보 문자열
     */
    @Tool(description = "도서 제목으로 현재 진행 중인 이벤트(할인, 무료 배송, 적립금 등)를 조회합니다. 도서의 프로모션 혜택을 확인할 때 사용합니다.")
    public String checkEventByTitle(
            @ToolParam(description = "조회할 도서 제목 (부분 일치 가능)", required = true) String title) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 5: 도서 제목으로 이벤트 조회 도구를 구현하세요");
    }
}
