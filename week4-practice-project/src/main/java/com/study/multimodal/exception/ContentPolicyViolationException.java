package com.study.multimodal.exception;

public class ContentPolicyViolationException extends RuntimeException {

    public ContentPolicyViolationException() {
        super("프롬프트가 OpenAI 콘텐츠 정책에 위반됩니다. "
                + "실제 인물, 저작권 캐릭터, 폭력적/선정적 표현 등을 제거하고 다시 시도해주세요.");
    }
}
