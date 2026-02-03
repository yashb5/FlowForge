package com.flowforge.repository;

import com.flowforge.domain.entity.TaskExecution;
import com.flowforge.domain.enums.ExecutionStatus;
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
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {

    List<TaskExecution> findByWorkflowExecutionId(UUID workflowExecutionId);

    List<TaskExecution> findByWorkflowExecutionIdOrderByQueuedAtAsc(UUID workflowExecutionId);

    Page<TaskExecution> findByWorkflowTaskId(UUID workflowTaskId, Pageable pageable);

    Optional<TaskExecution> findByWorkflowExecutionIdAndWorkflowTaskId(UUID workflowExecutionId, UUID workflowTaskId);

    Page<TaskExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    @Query("SELECT te FROM TaskExecution te WHERE te.workflowExecution.id = :executionId AND te.status = :status")
    List<TaskExecution> findByWorkflowExecutionIdAndStatus(@Param("executionId") UUID executionId, 
                                                            @Param("status") ExecutionStatus status);

    @Query("SELECT te FROM TaskExecution te WHERE te.status = 'WAITING_FOR_RETRY' AND te.nextRetryAt <= :now")
    List<TaskExecution> findTasksReadyForRetry(@Param("now") Instant now);

    @Query("SELECT te FROM TaskExecution te WHERE te.status = 'RUNNING' AND te.timeoutAt < :now")
    List<TaskExecution> findTimedOutTasks(@Param("now") Instant now);

    @Query("SELECT te FROM TaskExecution te WHERE te.workflowExecution.id = :executionId AND te.status IN ('PENDING', 'RUNNING')")
    List<TaskExecution> findActiveTasks(@Param("executionId") UUID executionId);

    @Query("SELECT te.status, COUNT(te) FROM TaskExecution te WHERE te.workflowExecution.id = :executionId GROUP BY te.status")
    List<Object[]> getTaskStatusCountsByExecutionId(@Param("executionId") UUID executionId);

    @Query("SELECT te FROM TaskExecution te WHERE te.workflowExecution.id = :executionId AND te.isSkipped = false " +
           "AND te.status IN ('COMPLETED', 'FAILED', 'CANCELLED') ORDER BY te.completedAt DESC")
    List<TaskExecution> findCompletedTasksForExecution(@Param("executionId") UUID executionId);

    @Query("SELECT COUNT(te) FROM TaskExecution te WHERE te.workflowExecution.id = :executionId AND te.status = :status")
    long countByExecutionIdAndStatus(@Param("executionId") UUID executionId, @Param("status") ExecutionStatus status);

    @Modifying
    @Query("UPDATE TaskExecution te SET te.status = :status, te.startedAt = :startedAt, " +
           "te.timeoutAt = :timeoutAt, te.workerId = :workerId WHERE te.id = :taskExecutionId")
    void startTaskExecution(@Param("taskExecutionId") UUID taskExecutionId,
                             @Param("status") ExecutionStatus status,
                             @Param("startedAt") Instant startedAt,
                             @Param("timeoutAt") Instant timeoutAt,
                             @Param("workerId") String workerId);

    @Modifying
    @Query("UPDATE TaskExecution te SET te.status = :status, te.completedAt = :completedAt, " +
           "te.errorMessage = :errorMessage, te.errorCode = :errorCode WHERE te.id = :taskExecutionId")
    void completeTaskExecution(@Param("taskExecutionId") UUID taskExecutionId,
                                @Param("status") ExecutionStatus status,
                                @Param("completedAt") Instant completedAt,
                                @Param("errorMessage") String errorMessage,
                                @Param("errorCode") String errorCode);

    @Modifying
    @Query("UPDATE TaskExecution te SET te.status = 'WAITING_FOR_RETRY', te.nextRetryAt = :nextRetryAt, " +
           "te.attemptNumber = te.attemptNumber + 1 WHERE te.id = :taskExecutionId")
    void scheduleRetry(@Param("taskExecutionId") UUID taskExecutionId, @Param("nextRetryAt") Instant nextRetryAt);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (te.completedAt - te.startedAt))) FROM TaskExecution te " +
           "WHERE te.workflowTask.id = :taskId AND te.status = 'COMPLETED' AND te.completedAt IS NOT NULL")
    Double getAverageExecutionTimeSeconds(@Param("taskId") UUID taskId);

    @Query("SELECT te.workflowTask.taskKey, COUNT(te), " +
           "SUM(CASE WHEN te.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN te.status = 'FAILED' THEN 1 ELSE 0 END) " +
           "FROM TaskExecution te WHERE te.workflowExecution.workflow.id = :workflowId " +
           "GROUP BY te.workflowTask.taskKey")
    List<Object[]> getTaskStatsByWorkflowId(@Param("workflowId") UUID workflowId);
}
