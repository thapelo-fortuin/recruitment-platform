package com.recruitplatform.soc_service.repository;
import com.recruitplatform.soc_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByUserIdAndEventTypeAndTimestampAfter(String userId, String eventType, LocalDateTime after);
    List<Event> findByServiceNameAndTimestampAfter(String serviceName, LocalDateTime after);
}