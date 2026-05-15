package com.island.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SimulationSnapshotRepository extends JpaRepository<SimulationSnapshotEntity, Long> {
    Optional<SimulationSnapshotEntity> findByFilename(String filename);
}
