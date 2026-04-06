# Week 8 Practice Project — Spring AI RAG·ETL 파이프라인·Tool Calling

Spring AI의 RAG(Retrieval-Augmented Generation), ETL 파이프라인, Tool Calling 기능을 익히는 실습 프로젝트입니다.

## 기술 스택

- Java 17, Spring Boot 4.0.2, Spring AI 2.0.0-M2
- OpenAI (`gpt-4.1-nano`, `text-embedding-ada-002`)
- PostgreSQL + pgvector (벡터 저장소)
- Spring Data JPA (회의실 예약 도메인)
- Thymeleaf 웹 UI

---

## 이론

| 주제 | 핵심 개념 |
|------|-----------|
| ETL 파이프라인 | DocumentReader(추출) → DocumentTransformer(변환) → VectorStore(적재), 텍스트·PDF·DOCX·HTML 소스 지원 |
| 텍스트 분할 | TokenTextSplitter, 청크 크기·최소 크기·구분자 설정, 대규모 문서의 임베딩 최적화 |
| 메타데이터 강화 | KeywordMetadataEnricher, LLM 기반 키워드 자동 추출, 검색 필터링 활용 |
| QuestionAnswerAdvisor | 기본 RAG 패턴, 벡터 유사도 검색 → 문맥 주입 → LLM 응답 생성 |
| Query Transformer | CompressionQueryTransformer(대화 맥락 압축), RewriteQueryTransformer(질문 재작성), TranslationQueryTransformer(질문 번역) |
| Query Expander | MultiQueryExpander, 하나의 질문을 여러 관점으로 확장하여 검색 재현율 향상 |
| RetrievalAugmentationAdvisor | QueryTransformer + DocumentRetriever 조합, 모듈형 RAG 파이프라인 구성 |
| Tool Calling 기본 | `@Tool` + `@ToolParam` 어노테이션, LLM이 도구 호출 여부·파라미터를 자율 결정 |
| ToolContext | `toolContext(Map)` 으로 인증 정보 등 외부 컨텍스트를 도구에 전달 |

---

## 실습 내용

### 1. ETL 파이프라인 (`EtlService`)
- 파일 업로드 ETL: `TextReader`(txt), `TikaDocumentReader`(pdf, docx) 로 문서 추출
- 메타데이터 추가: title, author, source 등 공통 메타데이터를 Document에 수동 부여
- 텍스트 분할: `TokenTextSplitter` 로 대규모 문서를 청크 단위로 분할
- 키워드 강화: `KeywordMetadataEnricher` 로 LLM 기반 키워드 5개 자동 추출 (소량 txt 전용)
- PDF 청크 설정: chunkSize·minChunkSizeChars 파라미터로 분할 크기 직접 제어
- HTML ETL: `JsoupDocumentReader` + Jsoup 직접 연결로 웹 페이지 텍스트 추출 → 벡터 저장소 적재
- 벡터 저장소 초기화: `TRUNCATE TABLE vector_store` 로 전체 데이터 삭제

### 2. RAG 채팅 — 기본 (`RagService.ragChat`)
- `QuestionAnswerAdvisor`: 벡터 저장소에서 유사 문서 검색 후 프롬프트에 문맥 주입
- `SearchRequest.builder()` 로 topK(3), similarityThreshold, filterExpression(source 기반) 설정
- 가장 단순한 RAG 패턴으로, Query Transformer 없이 원본 질문 그대로 검색

### 3. RAG 채팅 — CompressionQueryTransformer (`RagService.chatWithCompression`)
- `CompressionQueryTransformer`: 대화 기억(MessageChatMemoryAdvisor)과 함께 사용
- 이전 대화 맥락을 고려하여 현재 질문을 독립적인 질문으로 압축
- `conversationId` 로 세션 분리, `InMemoryChatMemoryRepository` 사용

### 4. RAG 채팅 — RewriteQueryTransformer (`RagService.chatWithRewriteQuery`)
- `RewriteQueryTransformer`: 사용자의 질문을 검색에 최적화된 형태로 재작성
- 모호하거나 구어체인 질문을 명확한 검색 쿼리로 변환

### 5. RAG 채팅 — TranslationQueryTransformer (`RagService.chatWithTranslation`)
- `TranslationQueryTransformer`: 질문을 지정 언어(Korean)로 번역 후 검색
- 영어 문서를 한국어 질문으로 검색할 때, 질문을 영어로 번역하여 검색 정확도 향상

### 6. RAG 채팅 — MultiQueryExpander (`RagService.chatWithMultiQuery`)
- `MultiQueryExpander`: 하나의 질문을 3개의 다른 관점으로 확장 (`numberOfQueries=3`)
- `includeOriginal(true)` 로 원본 질문도 포함하여 총 4개 쿼리로 검색
- 다양한 표현으로 검색하여 재현율(recall) 향상

### 7. Tool Calling — 날짜/시간 (`DateTimeTools`)
- `@Tool`: 메서드 설명을 LLM에게 제공하여 도구 호출 판단 근거 제공
- `getCurrentDateTime()`: 현재 날짜/시간 조회, 타임존 반영
- `setAlarm()`: `@ToolParam(description, required)` 으로 파라미터 설명, ISO-8601 시간 파싱

### 8. Tool Calling — 날씨 (`WeatherTools`)
- `getWeather()`: 단일 도시 날씨 조회 (더미 데이터 기반)
- `compareWeather()`: 두 도시 비교 전용 도구, `@ToolParam` 으로 city1·city2 파라미터 정의
- 시스템 프롬프트로 도구 사용 가이드 제공 ("비교 요청에는 반드시 compareWeather 사용")

### 9. Tool Calling — 회의실 예약 (`BookingTools` + ToolContext)
- `ToolContext`: `toolContext(Map.of("userName", userName))` 으로 사용자 인증 정보를 도구에 전달
- `listRooms()`: JPA로 회의실 목록 조회 (이름, 수용 인원, 위치, 장비 정보)
- `getBookings()`: 특정 날짜의 예약 현황 조회
- `bookRoom()`: 회의실 예약 생성, 시간 겹침 검증, ToolContext에서 예약자 정보 추출
- `cancelBooking()`: 예약 취소, 본인 예약만 취소 가능한 권한 검증

---

## 실행 방법

```bash
# 1. PostgreSQL + pgvector 실행
docker-compose up -d

# 2. 애플리케이션 실행
export OPENAI_API_KEY=sk-your-key-here
./gradlew bootRun
```

실행 후 http://localhost:8080 에서 웹 UI로 모든 기능을 테스트할 수 있습니다.

---

## API 엔드포인트

### ETL API (`/api/etl`)

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/etl/file` | 파일 업로드 ETL (txt, pdf, docx) — title, author, file 파라미터 |
| `POST` | `/api/etl/pdf` | PDF 전용 ETL — source, chunkSize, minChunkSizeChars 설정 가능 |
| `POST` | `/api/etl/html` | HTML ETL — title, author, url 파라미터 |
| `DELETE` | `/api/etl/clear` | 벡터 저장소 전체 초기화 |

### RAG 채팅 API (`/api/rag`)

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/rag/chat` | RAG 채팅 — ragType으로 전략 선택 (basic, compression, rewrite, translation, multiQuery) |

### Tool Calling API (`/api/tool`)

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/tool/datetime` | 날짜/시간 도구 채팅 |
| `POST` | `/api/tool/weather` | 날씨 도구 채팅 |
| `POST` | `/api/tool/booking` | 회의실 예약 도구 채팅 (userName 필요) |

---

## 더미 데이터

### RAG 테스트용 문서 (`src/main/resources/dummy/`)
- `sample-company-policy.txt` — 사내 규정 안내서
- `sample-product-guide.txt` — AI 제품 가이드
- `sample-dev-setup.docx` — 개발 환경 설정 가이드
- `intelligent-applications-with-spring-ai.pdf` — Spring AI 교재
- `html-etl.txt` — HTML ETL 테스트용 URL 목록

### 회의실 예약 더미 데이터 (DB 초기화 시 자동 생성)
- 회의실 4개: 회의실A(4인), 회의실B(8인), 회의실C(12인), 대강당(50인)
- 오늘~모레까지 9건의 예약 데이터

---
