package com.cjrequena.sample.es.core.persistence.repository;

import com.cjrequena.sample.es.core.persistence.entity.AggregateSnapshotEntity;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing aggregate snapshots in the event store.
 * <p>
 * Snapshots are used to optimize aggregate reconstruction by storing the complete
 * state of an aggregate at a specific version. This allows the system to avoid
 * replaying all events from the beginning when loading an aggregate.
 * </p>
 * <p>
 * This repository provides operations for storing and retrieving aggregate snapshots,
 * with support for version-based snapshot selection.
 * </p>
 */
@Repository
@Transactional
public interface AggregateSnapshotRepository extends JpaRepository<AggregateSnapshotEntity, UUID> {

  /**
   * Retrieves the most recent snapshot for a given aggregate, optionally up to a specific version.
   * <p>
   * This method returns the latest snapshot that is at or before the specified aggregate version.
   * If no version is specified (null), it returns the most recent snapshot available for the aggregate.
   * </p>
   * <p>
   * The query orders snapshots by version in descending order and returns only the first match,
   * ensuring optimal performance when multiple snapshots exist.
   * </p>
   *
   * @param aggregateId the unique identifier of the aggregate; must not be {@code null}
   * @param aggregateVersion the maximum version to consider; if {@code null}, returns the latest snapshot
   * @return an Optional containing the snapshot if found, empty otherwise
   */
  @Query(value = """
    SELECT *
      FROM es_aggregate_snapshot
     WHERE aggregate_id = :aggregateId
       AND (:aggregateVersion IS NULL OR aggregate_version <= :aggregateVersion)
     ORDER BY aggregate_version DESC
     LIMIT 1
    """, nativeQuery = true)
  Optional<AggregateSnapshotEntity> retrieveAggregateSnapshot(
    @Param("aggregateId") UUID aggregateId, 
    @Param("aggregateVersion") @Nullable Long aggregateVersion
  );

}
