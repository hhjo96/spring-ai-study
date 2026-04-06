package com.study.springai.repository;

import com.study.springai.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByBookingDateOrderByStartTime(LocalDate bookingDate);

    boolean existsByRoomIdAndBookingDateAndStartTimeBeforeAndEndTimeAfter(
            Integer roomId, LocalDate bookingDate, LocalTime endTime, LocalTime startTime);
}
