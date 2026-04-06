# Week 8 과제: AI 도서 추천 에이전트

## 개요

Spring AI의 **Tool Calling(도구 호출)** 기능을 활용하여, **검색 근거(리뷰/소개)** + **실시간 재고/가격** + **이벤트 안내**를 제공하는 AI 도서 추천 에이전트를 구현합니다.

### Week 6 vs Week 8 비교

| 구분 | Week 6 | Week 8 |
|------|--------|--------|
| 핵심 개념 | RAG + Memory + Advisor | RAG + Memory + Advisor + **Tool Calling** |
| 도서 검색 | VectorStore 시맨틱 검색만 | VectorStore + **인터넷 리뷰 검색 도구** |
| 가격/재고 | 없음 | **RDB 실시간 조회 도구** |
| 이벤트 | 없음 | **이벤트 여부 및 내용 안내** |
| UI | 학습용 다중 탭 | **실서비스급 2페이지 (도서탐색 + AI추천)** |

### 아키텍처

```
사용자 질문
  → SafeGuardAdvisor (민감 단어 차단)
  → PromptChatMemoryAdvisor (이전 대화 기억)
  → VectorStore 시맨틱 검색 (관련 도서 컨텍스트)
  → LLM 판단
      ├── searchBookReview() 호출 → Google 검색 → 리뷰/서평 반환
      ├── fetchPageContent() 호출 → 웹 페이지 본문 추출
      ├── checkInventoryByTitle() 호출 → RDB 재고/가격/이벤트 조회
      └── checkInventoryByIsbn() 호출 → ISBN 기반 조회
  → LLM 최종 응답 생성
  → SimpleLoggerAdvisor (로깅)
  → 사용자에게 응답 반환
```

## 사전 준비

### 1. Docker 실행

```bash
docker-compose up -d
```

### 2. 환경 변수 설정

```bash
export OPENAI_API_KEY=sk-proj-xxxxxxxxxx
export GOOGLE_SEARCH_API_KEY=your-google-api-key
export GOOGLE_SEARCH_ENGINE_ID=your-engine-id
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

브라우저에서 http://localhost:8080 접속

## 화면 구성

### 도서 탐색 페이지
- 장르 필터링, 도서 카드 그리드
- 카드 클릭 → **상세 모달** (책소개, 가격, 재고, 이벤트, **인터넷 리뷰 자동 검색**)
- 모달에서 "AI에게 추천 물어보기" 클릭 → AI 추천 페이지로 이동

### AI 추천 페이지
- Tool Calling 기반 AI 에이전트 채팅
- 자동으로 리뷰 검색, 재고/가격/이벤트 확인 후 응답
- 대화 기억으로 멀티턴 추천

## TODO 목록

### TODO 1: 도서 리뷰/소개 인터넷 검색 도구
- **파일**: `tool/BookReviewSearchTools.java` → `searchBookReview()`
- **내용**: Google Custom Search API로 도서 리뷰/서평 검색

### TODO 2: 웹 페이지 본문 텍스트 추출 도구
- **파일**: `tool/BookReviewSearchTools.java` → `fetchPageContent()`
- **내용**: Jsoup으로 HTML 파싱 후 본문 텍스트 반환

### TODO 3: 도서 제목으로 재고/가격/이벤트 조회 도구
- **파일**: `tool/BookInventoryTools.java` → `checkInventoryByTitle()`
- **내용**: RDB에서 도서 제목으로 가격/재고/이벤트 정보 조회

### TODO 4: ISBN으로 재고/가격/이벤트 조회 도구
- **파일**: `tool/BookInventoryTools.java` → `checkInventoryByIsbn()`

### TODO 5: ChatClient 빌드 — Advisor 체인 구성
- **파일**: `service/BookAgentService.java` → 생성자

### TODO 6: 도구 호출을 활용한 AI 도서 추천 에이전트 채팅
- **파일**: `service/BookAgentService.java` → `chat()`
- **핵심**: `chatClient.prompt().tools(bookReviewSearchTools, bookInventoryTools)`

### TODO 7: 도서 등록 시 RDB + VectorStore 동기화
- **파일**: `service/BookService.java` → `registerBook()`

## 참고

- **Chapter 10**: RAG, VectorStore, ETL
- **Chapter 11**: Tool Calling — `@Tool`, `@ToolParam`, `tools()`, `ToolContext`
