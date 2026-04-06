package com.study.springai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class EtlService {

    // ##### 필드 #####
    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    // ##### 생성자 #####
    public EtlService(ChatModel chatModel, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ##### 벡터 저장소의 데이터를 모두 삭제하는 메소드 #####
    public void clearVectorStore() {
        jdbcTemplate.update("TRUNCATE TABLE vector_store");
        log.info("벡터 저장소 초기화 완료");
    }

    // ##### 업로드된 파일을 가지고 ETL 과정을 처리하는 메소드 #####
    public String etlFromFile(String title, String author, MultipartFile attach) throws IOException {
        // 추출하기
        List<Document> documents = extractFromFile(attach);
        if (documents == null) {
            return ".txt, .pdf, .doc, .docx 파일 중에 하나를 올려주세요.";
        }
        log.info("추출된 Document 수: {} 개", documents.size());

        // 메타데이터에 공통 정보 추가하기
        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            metadata.putAll(Map.of(
                    "title", title,
                    "author", author,
                    "source", Objects.requireNonNull(attach.getOriginalFilename())));
        }

        // 변환하기 (txt는 키워드 추출 포함, pdf/docx는 분할만)
        boolean isTxt = "text/plain".equals(attach.getContentType());
        if (isTxt) {
            documents = transformWithKeywords(documents);
        } else {
            documents = transform(documents);
        }
        log.info("변환된 Document 수: {} 개", documents.size());

        // 적재하기
        vectorStore.add(documents);

        return "올린 문서를 추출-변환-적재 완료 했습니다.";
    }

    // ##### PDF 파일을 ETL 처리하는 메소드 (청크 크기 지정 가능) #####
    public String etlFromPdf(MultipartFile attach, String source, int chunkSize, int minChunkSizeChars) throws IOException {
        // 추출하기
        Resource resource = new ByteArrayResource(attach.getBytes());
        DocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();

        // 메타데이터 추가
        for (Document doc : documents) {
            doc.getMetadata().put("source", source);
        }

        // 변환하기
        DocumentTransformer transformer = new TokenTextSplitter(
                chunkSize, minChunkSizeChars, 5, 10000, true,
                List.of('.', '!', '?', '\n'));
        List<Document> transformedDocuments = transformer.apply(documents);
        log.info("변환된 Document 수: {} 개", transformedDocuments.size());

        // 적재하기
        vectorStore.add(transformedDocuments);

        return "PDF 추출-변환-적재 완료 (청크 크기: %d, 최소 청크: %d)".formatted(chunkSize, minChunkSizeChars);
    }

    // ##### HTML의 ETL 과정을 처리하는 메소드 #####
    public String etlFromHtml(String title, String author, String url) throws Exception {
        // Jsoup으로 직접 HTML 가져오기 (User-Agent 설정으로 403 방지)
        String html = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; SpringAI/1.0)")
                .timeout(10000)
                .get()
                .html();
        Resource resource = new ByteArrayResource(html.getBytes("UTF-8"));

        JsoupDocumentReader reader = new JsoupDocumentReader(
                resource,
                JsoupDocumentReaderConfig.builder()
                        .charset("UTF-8")
                        .selector("body")
                        .additionalMetadata(Map.of(
                                "title", title,
                                "author", author,
                                "url", url))
                        .build());
        List<Document> documents = reader.read();
        log.info("추출된 Document 수: {} 개", documents.size());

        DocumentTransformer transformer = new TokenTextSplitter();
        List<Document> transformedDocuments = transformer.apply(documents);
        log.info("변환된 Document 수: {} 개", transformedDocuments.size());

        vectorStore.add(transformedDocuments);

        return "HTML에서 추출-변환-적재 완료 했습니다.";
    }

    // ##### 업로드된 파일로부터 텍스트를 추출하는 메소드 #####
    private List<Document> extractFromFile(MultipartFile attach) throws IOException {
        Resource resource = new ByteArrayResource(attach.getBytes());

        List<Document> documents = null;
        if (attach.getContentType().equals("text/plain")) {
            DocumentReader reader = new TextReader(resource);
            documents = reader.read();
        } else if (attach.getContentType().equals("application/pdf")) {
            // TikaDocumentReader 사용 (PagePdfDocumentReader는 특정 PDF에서 StackOverflow 발생 가능)
            DocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.read();
        } else if (attach.getContentType().contains("wordprocessingml")) {
            DocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.read();
        }

        return documents;
    }

    // ##### 작은 크기로 분할하는 메소드 #####
    private List<Document> transform(List<Document> documents) {
        // 작게 분할하기
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> transformedDocuments = tokenTextSplitter.apply(documents);

        return transformedDocuments;
    }

    // ##### 키워드 메타데이터까지 추가하는 변환 메소드 (소량 문서용) #####
    private List<Document> transformWithKeywords(List<Document> documents) {
        List<Document> transformedDocuments = transform(documents);

        // 메타데이터에 키워드 추가하기 (청크가 많으면 LLM 호출이 많아져 느려질 수 있음)
        KeywordMetadataEnricher keywordMetadataEnricher =
                new KeywordMetadataEnricher(chatModel, 5);
        transformedDocuments = keywordMetadataEnricher.apply(transformedDocuments);

        return transformedDocuments;
    }
}
