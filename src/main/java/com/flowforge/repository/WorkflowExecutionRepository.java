package com.flowforge.repository;

import com.flowforge.domain.entity.WorkflowExecution;
import com.flowforge.domain.enums.ExecutionStatus;
import com.flowforge.domain.enums.ExecutionTrigger;
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
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Page<WorkflowExecution> findByWorkflowId(UUID workflowId, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowIdAndStatus(UUID workflowId, ExecutionStatus status, Pageable pageable);

    Optional<WorkflowExecution> findByIdAndWorkflowId(UUID id, UUID workflowId);

    Page<WorkflowExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    Page<WorkflowExecution> findByTriggerType(ExecutionTrigger triggerType, Pageable pageable);

    Page<WorkflowExecution> findByScheduleId(UUID scheduleId, Pageable pageable);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.workflow.owner.id = :ownerId ORDER BY we.createdAt DESC")
    Page<WorkflowExecution> findByWorkflowOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.status IN :statuses")
    Page<WorkflowExecution> findByStatusIn(@Param("statuses") List<ExecutionStatus> statuses, Pageable pageable);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.status = 'RUNNING' AND we.timeoutAt < :now")
    List<WorkflowExecution> findTimedOutExecutions(@Param("now") Instant now);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.status IN ('PENDING', 'RUNNING') AND we.workflow.id = :workflowId")
    List<WorkflowExecution> findActiveExecutionsByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT COUNT(we) FROM WorkflowExecution we WHERE we.schedule.id = :scheduleId AND we.status IN ('PENDING', 'RUNNING')")
    long countActiveExecutionsByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT we FROM WorkflowExecution we LEFT JOIN FETCH we.taskExecutions WHERE we.id = :id")
    Optional<WorkflowExecution> findByIdWithTaskExecutions(@Param("id") UUID id);

    @Query("SELECT COUNT(we) FROM WorkflowExecution we WHERE we.workflow.id = :workflowId AND we.status = :status")
    long countByWorkflowIdAndStatus(@Param("workflowId") UUID workflowId, @Param("status") ExecutionStatus status);

    @Query("SELECT we.status, COUNT(we) FROM WorkflowExecution we WHERE we.workflow.id = :workflowId GROUP BY we.status")
    List<Object[]> getExecutionStatusCountsByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.startedAt >= :since AND we.status = :status")
    List<WorkflowExecution> findByStatusSince(@Param("status") ExecutionStatus status, @Param("since") Instant since);

    @Modifying
    @Query("UPDATE WorkflowExecution we SET we.status = :status, we.completedAt = :completedAt, " +
           "we.errorMessage = :errorMessage WHERE we.id = :executionId")
    void updateExecutionStatus(@Param("executionId") UUID executionId, 
                                @Param("status") ExecutionStatus status,
                                @Param("completedAt") Instant completedAt,
                                @Param("errorMessage") String errorMessage);

    @Modifying
    @Query("UPDATE WorkflowExecution we SET we.status = 'CANCELLED', we.isCancelled = true, " +
           "we.cancelledAt = :cancelledAt, we.cancelledBy.id = :cancelledBy, " +
           "we.cancellationReason = :reason WHERE we.id = :executionId")
    void cancelExecution(@Param("executionId") UUID executionId, 
                          @Param("cancelledAt") Instant cancelledAt,
                          @Param("cancelledBy") UUID cancelledBy,
                          @Param("reason") String reason);

    @Query("SELECT COUNT(we) FROM WorkflowExecution we WHERE we.workflow.id = :workflowId AND we.status = 'COMPLETED'")
    long countCompletedByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.workflow.id = :workflowId AND we.status = 'COMPLETED' AND we.completedAt IS NOT NULL")
    List<WorkflowExecution> findCompletedExecutions(@Param("workflowId") UUID workflowId);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.workflow.id = :workflowId AND we.startedAt >= :since")
    List<WorkflowExecution> findByWorkflowIdAndStartedAtAfter(@Param("workflowId") UUID workflowId, @Param("since") Instant since);

    @Query("SELECT COUNT(we) FROM WorkflowExecution we WHERE we.status = :status")
    long countByStatus(@Param("status") ExecutionStatus status);

    @Query("SELECT COUNT(we) FROM WorkflowExecution we WHERE we.workflow.id = :workflowId")
    long countByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT we FROM WorkflowExecution we WHERE we.startedAt >= :since ORDER BY we.startedAt ASC")
    List<WorkflowExecution> findByStartedAtAfter(@Param("since") Instant since);

    @Query("SELECT we.status, COUNT(we) FROM WorkflowExecution we GROUP BY we.status")
    List<Object[]> getExecutionStatusCounts();

    @Query("SELECT we FROM WorkflowExecution we WHERE we.status = 'COMPLETED' " +
           "AND we.completedAt IS NOT NULL AND we.startedAt IS NOT NULL")
    List<WorkflowExecution> findCompletedExecutionsWithDuration();

    @Query("SELECT we FROM WorkflowExecution we WHERE we.workflow.id = :workflowId AND we.status = 'COMPLETED' " +
           "AND we.completedAt IS NOT NULL AND we.startedAt IS NOT NULL")
    List<WorkflowExecution> findCompletedExecutionsWithDurationByWorkflow(@Param("workflowId") UUID workflowId);
}
