package com.relayops.api.repo;

import com.relayops.api.domain.Destination;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
  List<Destination> findByApiClientIdOrderByCreatedAtDesc(UUID apiClientId);

  Optional<Destination> findByIdAndApiClientId(UUID id, UUID apiClientId);
}
