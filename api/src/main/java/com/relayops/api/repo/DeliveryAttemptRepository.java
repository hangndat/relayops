package com.relayops.api.repo;

import com.relayops.api.domain.DeliveryAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
  List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(UUID eventId);
}
