package com.relayops.api.repo;

import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.domain.InboundEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundEventRepository extends JpaRepository<InboundEvent, UUID> {
  Optional<InboundEvent> findByApiClientIdAndIdempotencyKey(UUID apiClientId, String idempotencyKey);

  Optional<InboundEvent> findByIdAndApiClientId(UUID id, UUID apiClientId);

  List<InboundEvent> findByApiClientIdOrderByCreatedAtDesc(UUID apiClientId, Pageable pageable);

  List<InboundEvent> findByApiClientIdAndStatusOrderByCreatedAtDesc(
      UUID apiClientId, DeliveryStatus status, Pageable pageable);
}
