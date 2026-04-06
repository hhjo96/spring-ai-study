package com.study.bookadvisor.repository;

import com.study.bookadvisor.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByBookIdAndActiveTrue(Long bookId);

    List<Event> findByBookId(Long bookId);

}
