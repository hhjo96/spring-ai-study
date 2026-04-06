package com.study.springai.controller;

import com.study.springai.dto.MessageResponse;
import com.study.springai.service.EtlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/etl")
@RequiredArgsConstructor
public class EtlController {

    private final EtlService etlService;

    /**
     * 벡터 저장소 초기화
     */
    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearVectorStore() {
        etlService.clearVectorStore();
        return ResponseEntity.ok(new MessageResponse("벡터 저장소가 초기화되었습니다."));
    }

    /**
     * 파일 업로드 ETL (txt, pdf, doc, docx)
     */
    @PostMapping("/file")
    public ResponseEntity<MessageResponse> etlFromFile(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("file") MultipartFile attach) throws Exception {
        String result = etlService.etlFromFile(title, author, attach);
        return ResponseEntity.ok(new MessageResponse(result));
    }

    /**
     * PDF 업로드 ETL (청크 크기 지정 가능)
     */
    @PostMapping("/pdf")
    public ResponseEntity<MessageResponse> etlFromPdf(
            @RequestParam("file") MultipartFile attach,
            @RequestParam("source") String source,
            @RequestParam(value = "chunkSize", defaultValue = "800") int chunkSize,
            @RequestParam(value = "minChunkSizeChars", defaultValue = "350") int minChunkSizeChars) throws Exception {
        String result = etlService.etlFromPdf(attach, source, chunkSize, minChunkSizeChars);
        return ResponseEntity.ok(new MessageResponse(result));
    }

    /**
     * HTML ETL
     */
    @PostMapping("/html")
    public ResponseEntity<MessageResponse> etlFromHtml(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("url") String url) throws Exception {
        String result = etlService.etlFromHtml(title, author, url);
        return ResponseEntity.ok(new MessageResponse(result));
    }
}
