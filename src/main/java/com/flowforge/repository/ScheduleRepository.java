package com.flowforge.repository;

import com.flowforge.domain.entity.Schedule;
import com.flowforge.domain.enums.ScheduleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    Page<Schedule> findByWorkflowId(UUID workflowId, Pageable pageable);

    Optional<Schedule> findByIdAndWorkflowId(UUID id, UUID workflowId);

    Page<Schedule> findByScheduleType(ScheduleType scheduleType, Pageable pageable);

    Page<Schedule> findByIsActiveTrue(Pageable pageable);

    Page<Schedule> findByIsActiveTrueAndIsPausedFalse(Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.isPaused = false AND " +
           "s.nextRunAt <= :now AND (s.endAt IS NULL OR s.endAt > :now) AND " +
           "(s.maxExecutions IS NULL OR s.executionCount < s.maxExecutions)")
    List<Schedule> findSchedulesDueForExecution(@Param("now") Instant now);

    @Query("SELECT s FROM Schedule s WHERE s.workflow.owner.id = :ownerId")
    Page<Schedule> findByWorkflowOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.isPaused = false AND " +
           "s.workflow.status = 'ACTIVE' AND s.workflow.isActive = true AND " +
           "s.nextRunAt <= :now")
    List<Schedule> findReadySchedules(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Schedule s SET s.nextRunAt = :nextRun, s.lastRunAt = :lastRun, " +
           "s.executionCount = s.executionCount + 1 WHERE s.id = :scheduleId")
    void updateAfterExecution(@Param("scheduleId") UUID scheduleId, 
                               @Param("lastRun") Instant lastRun, 
                               @Param("nextRun") Instant nextRun);

    @Modifying
    @Query("UPDATE Schedule s SET s.isPaused = :paused WHERE s.id = :scheduleId")
    void updatePausedStatus(@Param("scheduleId") UUID scheduleId, @Param("paused") boolean paused);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.workflow.id = :workflowId AND s.isActive = true")
    long countActiveByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleType = 'ONE_TIME' AND s.isActive = true AND " +
           "s.executionCount > 0")
    List<Schedule> findCompletedOneTimeSchedules();

    boolean existsByNameAndWorkflowId(String name, UUID workflowId);
}
