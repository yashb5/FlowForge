package com.flowforge.repository;

import com.flowforge.domain.entity.Workflow;
import com.flowforge.domain.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    Page<Workflow> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Workflow> findByOwnerIdAndStatus(UUID ownerId, WorkflowStatus status, Pageable pageable);

    Optional<Workflow> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Workflow> findByStatus(WorkflowStatus status, Pageable pageable);

    Page<Workflow> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT w FROM Workflow w WHERE w.isActive = true AND " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Workflow> searchByNameOrDescription(@Param("search") String search, Pageable pageable);

    @Query("SELECT w FROM Workflow w JOIN w.tags tag WHERE tag IN :tags AND w.isActive = true")
    Page<Workflow> findByTagsIn(@Param("tags") Set<String> tags, Pageable pageable);

    @Query("SELECT w FROM Workflow w WHERE w.owner.id = :ownerId AND w.status = :status AND w.isActive = true")
    Page<Workflow> findActiveByOwnerAndStatus(@Param("ownerId") UUID ownerId, 
                                               @Param("status") WorkflowStatus status, 
                                               Pageable pageable);

    @Query("SELECT DISTINCT tag FROM Workflow w JOIN w.tags tag WHERE w.owner.id = :ownerId")
    Set<String> findAllTagsByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.owner.id = :ownerId AND w.isActive = true")
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.tasks WHERE w.id = :id")
    Optional<Workflow> findByIdWithTasks(@Param("id") UUID id);

    @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.schedules WHERE w.id = :id")
    Optional<Workflow> findByIdWithSchedules(@Param("id") UUID id);

    @Query("SELECT w FROM Workflow w WHERE w.status = 'ACTIVE' AND w.isActive = true AND " +
           "EXISTS (SELECT s FROM Schedule s WHERE s.workflow = w AND s.isActive = true AND s.isPaused = false)")
    List<Workflow> findAllScheduledWorkflows();

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
