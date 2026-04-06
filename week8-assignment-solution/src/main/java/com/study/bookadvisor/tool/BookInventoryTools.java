package com.study.bookadvisor.tool;

import com.study.bookadvisor.entity.Book;
import com.study.bookadvisor.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 도서 재고/가격 조회 도구
 *
 * RDB에 저장된 도서의 실시간 재고 수량과 가격 정보를 조회합니다.
 * LLM이 도서를 추천할 때, 현재 구매 가능 여부와 가격을 안내합니다.
 *
 * [아키텍처]
 * 사용자 질문 → LLM 판단 → 재고/가격 조회 도구 호출
 *   → BookRepository를 통해 RDB 조회 → 재고/가격 정보 반환
 *   → LLM이 추천 응답에 재고/가격 정보를 포함
 *
 * [참고] 이벤트 정보는 BookEventTools에서 별도로 조회합니다.
 */
@Component
@Slf4j
public class BookInventoryTools {

    private final BookRepository bookRepository;

    public BookInventoryTools(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ========================================================================
    // TODO 3: 도서 제목으로 재고/가격 조회 도구
    // ========================================================================
    /**
     * 도서 제목으로 재고 수량과 가격 정보를 조회합니다.
     *
     * [요구사항]
     * - bookRepository.findByTitleContainingIgnoreCase()를 사용하여 도서 검색
     * - 도서가 존재하면 아래 정보를 포함한 문자열 반환:
     *   · 도서 제목, 저자
     *   · 가격 (원 단위, 콤마 포맷)
     *   · 재고 수량, 구매 가능 여부 (품절/재고 부족/구매 가능)
     * - 도서를 찾을 수 없으면 "'{title}' 도서의 재고/가격 정보를 찾을 수 없습니다." 반환
     *
     * [힌트]
     * 1) 도서 조회:
     *    Optional<Book> bookOpt = bookRepository.findByTitleContainingIgnoreCase(title);
     *
     * 2) 재고 상태 판단:
     *    String status;
     *    if (book.getStock() == 0) status = "품절";
     *    else if (book.getStock() <= 5) status = "재고 부족 (곧 품절 예상)";
     *    else status = "구매 가능";
     *
     * 3) 결과 포맷:
     *    String.format(
     *        "도서: %s | 저자: %s | 가격: %,d원 | 재고: %d권 | 상태: %s",
     *        book.getTitle(), book.getAuthor(), book.getPrice(),
     *        book.getStock(), status
     *    );
     *
     * @param title 조회할 도서 제목 (부분 일치 검색)
     * @return 재고/가격 정보 문자열
     */
    @Tool(description = "도서 제목으로 현재 재고 수량과 가격을 조회합니다. 도서의 구매 가능 여부와 가격을 확인할 때 사용합니다.")
    public String checkInventoryByTitle(
            @ToolParam(description = "조회할 도서 제목 (부분 일치 가능)", required = true) String title) {
        log.info("재고/가격 조회 도구 호출 (제목): title={}", title);
        Optional<Book> bookOpt = bookRepository.findByTitleContainingIgnoreCase(title);

        if (bookOpt.isEmpty()) {
            return "'" + title + "' 도서의 재고/가격 정보를 찾을 수 없습니다.";
        }

        Book book = bookOpt.get();
        String status;
        if (book.getStock() == 0) status = "품절";
        else if (book.getStock() <= 5) status = "재고 부족 (곧 품절 예상)";
        else status = "구매 가능";

        return String.format(
                "도서: %s | 저자: %s | 가격: %,d원 | 재고: %d권 | 상태: %s",
                book.getTitle(), book.getAuthor(), book.getPrice(),
                book.getStock(), status
        );
    }

    // ========================================================================
    // TODO 4: ISBN으로 재고/가격 조회 도구
    // ========================================================================
    /**
     * ISBN으로 도서의 재고 수량과 가격 정보를 조회합니다.
     *
     * [요구사항]
     * - bookRepository.findByIsbn()을 사용하여 도서 검색
     * - 도서가 존재하면 제목, ISBN, 가격, 재고, 상태를 포함한 문자열 반환
     * - 재고 상태: 0이면 "품절", 5권 이하이면 "재고 부족", 그 이상이면 "구매 가능"
     * - 도서를 찾을 수 없으면 "ISBN '{isbn}'에 해당하는 도서를 찾을 수 없습니다." 반환
     *
     * [힌트]
     * - checkInventoryByTitle()과 동일한 로직, 검색 방법만 findByIsbn() 사용
     *
     * @param isbn 조회할 도서 ISBN
     * @return 재고/가격 정보 문자열
     */
    @Tool(description = "ISBN으로 도서의 재고 수량과 가격을 조회합니다. 정확한 도서 식별이 필요할 때 사용합니다.")
    public String checkInventoryByIsbn(
            @ToolParam(description = "조회할 도서 ISBN", required = true) String isbn) {
        log.info("재고/가격 조회 도구 호출 (ISBN): isbn={}", isbn);
        Optional<Book> bookOpt = bookRepository.findByIsbn(isbn);

        if (bookOpt.isEmpty()) {
            return "ISBN '" + isbn + "'에 해당하는 도서를 찾을 수 없습니다.";
        }

        Book book = bookOpt.get();
        String status;
        if (book.getStock() == 0) status = "품절";
        else if (book.getStock() <= 5) status = "재고 부족 (곧 품절 예상)";
        else status = "구매 가능";

        return String.format(
                "도서: %s | ISBN: %s | 가격: %,d원 | 재고: %d권 | 상태: %s",
                book.getTitle(), book.getIsbn(), book.getPrice(),
                book.getStock(), status
        );
    }
}
