package com.relayops.api.repo;

import com.relayops.api.domain.InboundEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundEventRepository extends JpaRepository<InboundEvent, UUID> {
  Optional<InboundEvent> findByApiClientIdAndIdempotencyKey(UUID apiClientId, String idempotencyKey);

  Optional<InboundEvent> findByIdAndApiClientId(UUID id, UUID apiClientId);
}
