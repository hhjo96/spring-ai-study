package com.study.springai.tool;

import com.study.springai.entity.Booking;
import com.study.springai.entity.Room;
import com.study.springai.repository.BookingRepository;
import com.study.springai.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 회의실 예약 도구 (ToolContext + JPA 연동)
 * PostgreSQL에서 실시간으로 회의실/예약 데이터를 조회·생성·삭제합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTools {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    @Tool(description = "사용 가능한 회의실 목록을 조회합니다. 회의실 이름, 수용 인원, 위치, 프로젝터/화이트보드 유무를 반환합니다.")
    public String listRooms() {
        log.info("회의실 목록 조회");

        List<Room> rooms = roomRepository.findAll();

        if (rooms.isEmpty()) {
            return "등록된 회의실이 없습니다.";
        }

        StringBuilder sb = new StringBuilder("사용 가능한 회의실:\n");
        for (Room room : rooms) {
            sb.append("- %s (%d인) | 위치: %s | 프로젝터: %s | 화이트보드: %s\n".formatted(
                    room.getName(),
                    room.getCapacity(),
                    room.getLocation(),
                    Boolean.TRUE.equals(room.getHasProjector()) ? "있음" : "없음",
                    Boolean.TRUE.equals(room.getHasWhiteboard()) ? "있음" : "없음"
            ));
        }
        return sb.toString();
    }

    @Tool(description = "특정 날짜의 회의실 예약 현황을 조회합니다.")
    public String getBookings(
            @ToolParam(description = "조회할 날짜 (yyyy-MM-dd 형식)", required = true) String date) {
        log.info("예약 현황 조회 - 날짜: {}", date);

        LocalDate bookingDate = LocalDate.parse(date);
        List<Booking> bookings = bookingRepository.findByBookingDateOrderByStartTime(bookingDate);

        if (bookings.isEmpty()) {
            return "%s 에는 예약이 없습니다.".formatted(date);
        }

        StringBuilder sb = new StringBuilder("%s 예약 현황:\n".formatted(date));
        for (Booking b : bookings) {
            sb.append("- [#%d] %s | %s~%s | 예약자: %s | 목적: %s\n".formatted(
                    b.getId(), b.getRoom().getName(),
                    b.getStartTime(), b.getEndTime(),
                    b.getBookedBy(), b.getPurpose()
            ));
        }
        return sb.toString();
    }

    @Tool(description = """
        회의실을 예약합니다.
        예약이 성공하면 예약 ID와 상세 정보를 반환합니다.
        예약이 실패하면 실패 사유를 반환합니다.
    """)
    public String bookRoom(
            @ToolParam(description = "예약할 회의실 이름", required = true) String room,
            @ToolParam(description = "예약 날짜 (yyyy-MM-dd 형식)", required = true) String date,
            @ToolParam(description = "시작 시간 (HH:mm 형식)", required = true) String startTime,
            @ToolParam(description = "종료 시간 (HH:mm 형식)", required = true) String endTime,
            @ToolParam(description = "예약 목적", required = false) String purpose,
            ToolContext toolContext) {

        // ToolContext에서 사용자 정보 가져오기
        String userName = (String) toolContext.getContext().get("userName");
        if (userName == null || userName.isBlank()) {
            log.warn("예약 실패 - 사용자 정보 없음");
            return "예약 실패: 사용자 인증 정보가 필요합니다.";
        }

        // 회의실 존재 여부 확인
        Room foundRoom = roomRepository.findByName(room).orElse(null);
        if (foundRoom == null) {
            List<String> allRoomNames = roomRepository.findAll().stream()
                    .map(Room::getName)
                    .collect(Collectors.toList());
            return "예약 실패: '%s' 은(는) 존재하지 않는 회의실입니다. 사용 가능: %s".formatted(room, String.join(", ", allRoomNames));
        }

        LocalDate bookingDate = LocalDate.parse(date);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // 시간 겹침 확인
        boolean conflict = bookingRepository.existsByRoomIdAndBookingDateAndStartTimeBeforeAndEndTimeAfter(
                foundRoom.getId(), bookingDate, end, start);

        if (conflict) {
            return "예약 실패: %s은(는) %s %s~%s에 이미 다른 예약이 있습니다.".formatted(room, date, startTime, endTime);
        }

        // 예약 생성
        Booking booking = Booking.builder()
                .room(foundRoom)
                .bookingDate(bookingDate)
                .startTime(start)
                .endTime(end)
                .bookedBy(userName)
                .purpose(purpose != null ? purpose : "")
                .build();
        bookingRepository.save(booking);

        log.info("예약 성공 - ID: {}, 회의실: {}, 날짜: {}, 시간: {}~{}, 예약자: {}",
                booking.getId(), room, date, startTime, endTime, userName);
        return "예약 성공! [예약번호: #%d] %s, %s %s~%s, 예약자: %s".formatted(
                booking.getId(), room, date, startTime, endTime, userName);
    }

    @Tool(description = "예약을 취소합니다. 예약 ID가 필요합니다.")
    public String cancelBooking(
            @ToolParam(description = "취소할 예약 ID", required = true) int bookingId,
            ToolContext toolContext) {

        String userName = (String) toolContext.getContext().get("userName");

        // 예약 존재 여부 확인
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return "취소 실패: 예약번호 #%d 을(를) 찾을 수 없습니다.".formatted(bookingId);
        }

        if (!booking.getBookedBy().equals(userName)) {
            return "취소 실패: 본인의 예약만 취소할 수 있습니다. (예약자: %s)".formatted(booking.getBookedBy());
        }

        bookingRepository.delete(booking);
        log.info("예약 취소 - ID: {}, 예약자: {}", bookingId, userName);
        return "예약 #%d 이(가) 성공적으로 취소되었습니다.".formatted(bookingId);
    }
}
