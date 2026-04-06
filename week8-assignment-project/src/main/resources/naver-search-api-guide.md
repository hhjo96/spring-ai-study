# Naver 검색 API 설정 가이드

이 프로젝트에서는 Naver 검색 API를 사용하여 도서의 전문가 서평과 리뷰 링크를 검색합니다.
아래 단계를 따라 Client ID와 Client Secret을 발급받으세요.

---

## 1. Naver Developers 애플리케이션 등록

1. [Naver Developers](https://developers.naver.com/)에 접속하여 로그인합니다.
2. 상단 메뉴에서 **Application** > **애플리케이션 등록**을 클릭합니다.
3. 애플리케이션 정보를 입력합니다:
    - **애플리케이션 이름**: `Book Review Search` (원하는 이름)
    - **사용 API**: **검색** 선택
    - **비로그인 오픈 API 서비스 환경**: **WEB 설정** > `http://localhost:8080` 입력
4. **등록하기**를 클릭합니다.
5. 등록 완료 후 **Client ID**와 **Client Secret**을 복사해 둡니다.

## 2. application.yml 설정

발급받은 Client ID와 Client Secret을 프로젝트에 설정합니다.

### 방법 A: 환경 변수 사용 (권장)

```bash
export NAVER_CLIENT_ID=여기에_Client_ID_입력
export NAVER_CLIENT_SECRET=여기에_Client_Secret_입력
```

`application.yml`에는 이미 환경 변수 바인딩이 설정되어 있습니다:

```yaml
naver:
  search:
    blog-endpoint: https://openapi.naver.com/v1/search/blog.json
    client-id: ${NAVER_CLIENT_ID:your-naver-client-id}
    client-secret: ${NAVER_CLIENT_SECRET:your-naver-client-secret}
```

### 방법 B: application.yml에 직접 입력

```yaml
naver:
  search:
    blog-endpoint: https://openapi.naver.com/v1/search/blog.json
    client-id: 실제_Client_ID
    client-secret: 실제_Client_Secret
```

> **주의**: Client Secret을 코드에 직접 입력할 경우 Git에 커밋하지 않도록 주의하세요.

## 3. API 호출 테스트

설정이 올바른지 터미널에서 curl로 테스트할 수 있습니다:

```bash
curl -G "https://openapi.naver.com/v1/search/blog.json" \
  --data-urlencode "query=클린코드 서평 리뷰" \
  -d "display=3" -d "sort=sim" \
  -H "X-Naver-Client-Id: YOUR_CLIENT_ID" \
  -H "X-Naver-Client-Secret: YOUR_CLIENT_SECRET"
```

정상 응답 시 아래와 같은 JSON이 반환됩니다:

```json
{
  "lastBuildDate": "Mon, 06 Apr 2026 12:00:00 +0900",
  "total": 12345,
  "start": 1,
  "display": 3,
  "items": [
    {
      "title": "클린 코드 <b>서평</b> - 읽기 좋은 코드란",
      "link": "https://blog.naver.com/example/123456",
      "description": "로버트 마틴의 클린 코드는 ...",
      "bloggername": "개발자 블로그",
      "bloggerlink": "https://blog.naver.com/example",
      "postdate": "20260401"
    }
  ]
}
```

> **참고**: Naver 검색 결과의 `title`과 `description`에는 `<b>` 태그가 포함될 수 있습니다. 코드에서 `replaceAll("<.*?>", "")`로 HTML 태그를 제거하세요.

## 4. 무료 사용량 및 요금

- **무료**: 하루 25,000회 검색 쿼리
- **유료 전환 불필요**: 일반적인 개발/운영 용도로 충분
- 자세한 내용은 [Naver Developers API 사용 가이드](https://developers.naver.com/docs/common/openapiguide/)를 참고하세요.

> Google Custom Search API (하루 100회)에 비해 매우 넉넉한 무료 할당량입니다.

## 5. Naver 검색 API 주요 파라미터

| 파라미터 | 필수 | 설명 | 예시 |
|---------|------|------|------|
| `query` | O | 검색어 (UTF-8 인코딩) | `클린코드 서평 리뷰` |
| `display` | X | 검색 결과 개수 (기본 10, 최대 100) | `3` |
| `start` | X | 검색 시작 위치 (기본 1, 최대 1000) | `1` |
| `sort` | X | 정렬 기준: `sim`(유사도순), `date`(최신순) | `sim` |

## 6. 프로젝트에서의 사용 흐름

```
사용자 질문: "클린 코드 추천해줘"
    ↓
AI 에이전트 → searchBookReview("클린 코드") 도구 호출
    ↓
Naver 블로그 검색 API 호출
    → GET /v1/search/blog.json?query=클린코드+서평+리뷰&display=3
    ↓
검색 결과 (제목, 링크, 요약) 반환
    ↓
AI 에이전트 → (선택) fetchPageContent(링크URL) 도구 호출
    ↓
서평 페이지 본문 텍스트 추출 (Jsoup, 최대 2000자)
    ↓
AI가 서평 근거 + <a href="링크">서평 제목</a> 포함한 HTML 응답 생성
```

## 7. 인증 헤더 정보

Naver 검색 API는 HTTP 헤더에 인증 정보를 포함합니다:

| 헤더 | 값 |
|------|-----|
| `X-Naver-Client-Id` | 애플리케이션 등록 시 발급받은 Client ID |
| `X-Naver-Client-Secret` | 애플리케이션 등록 시 발급받은 Client Secret |

## 8. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `401 Unauthorized` | Client ID/Secret 오류 | Naver Developers에서 인증 정보 재확인 |
| `403 Forbidden` | API 권한 없음 | 애플리케이션 설정에서 "검색" API 사용 여부 확인 |
| `429 Too Many Requests` | 일일 할당량 초과 | 다음 날 재시도 (하루 25,000회) |
| 빈 `items` 배열 | 검색 결과 없음 | 검색어를 변경하여 재시도 |
| `NAVER_CLIENT_ID` 미설정 | 환경 변수 누락 | 환경 변수 설정 또는 yml 직접 입력 |
| HTML 태그 포함된 결과 | Naver 검색 결과 특성 | `replaceAll("<.*?>", "")`로 태그 제거 |
