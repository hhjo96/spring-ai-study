-- PostgreSQL 초기화 스크립트
\connect app_vector_db;

-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 회의실 테이블
CREATE TABLE IF NOT EXISTS room (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    location VARCHAR(100),
    has_projector BOOLEAN DEFAULT false,
    has_whiteboard BOOLEAN DEFAULT false
);

-- 예약 테이블
CREATE TABLE IF NOT EXISTS booking (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES room(id),
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    booked_by VARCHAR(50) NOT NULL,
    purpose VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 회의실 더미 데이터
INSERT INTO room (name, capacity, location, has_projector, has_whiteboard) VALUES
    ('회의실A', 4, '3층 동쪽', false, true),
    ('회의실B', 8, '3층 서쪽', true, true),
    ('회의실C', 12, '5층 동쪽', true, true),
    ('대강당', 50, '1층 로비 옆', true, false)
ON CONFLICT (name) DO NOTHING;

-- 예약 더미 데이터 (오늘~이번 주 기준으로 생성)
INSERT INTO booking (room_id, booking_date, start_time, end_time, booked_by, purpose) VALUES
    -- 오늘 예약
    (1, CURRENT_DATE, '09:00', '10:00', '김철수', '데일리 스크럼'),
    (2, CURRENT_DATE, '10:00', '11:30', '이영희', '스프린트 플래닝'),
    (3, CURRENT_DATE, '14:00', '16:00', '박민수', '디자인 리뷰'),
    (4, CURRENT_DATE, '13:00', '15:00', '정수진', '전체 타운홀 미팅'),
    -- 내일 예약
    (1, CURRENT_DATE + 1, '10:00', '11:00', '김철수', '1:1 미팅'),
    (2, CURRENT_DATE + 1, '14:00', '15:30', '최지원', 'API 설계 회의'),
    (3, CURRENT_DATE + 1, '09:00', '12:00', '이영희', '워크샵'),
    -- 모레 예약
    (1, CURRENT_DATE + 2, '15:00', '16:00', '박민수', '코드 리뷰'),
    (2, CURRENT_DATE + 2, '10:00', '11:00', '정수진', '신규 입사자 온보딩');
