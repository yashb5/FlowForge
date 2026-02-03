package com.flowforge.repository;

import com.flowforge.domain.entity.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {

    List<WorkflowTask> findByWorkflowIdOrderByExecutionOrderAsc(UUID workflowId);

    Optional<WorkflowTask> findByWorkflowIdAndTaskKey(UUID workflowId, String taskKey);

    List<WorkflowTask> findByWorkflowIdAndIsEnabledTrue(UUID workflowId);

    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.workflow.id = :workflowId AND wt.taskKey IN :taskKeys")
    List<WorkflowTask> findByWorkflowIdAndTaskKeys(@Param("workflowId") UUID workflowId, 
                                                    @Param("taskKeys") Set<String> taskKeys);

    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.taskDefinition.id = :taskDefinitionId")
    List<WorkflowTask> findByTaskDefinitionId(@Param("taskDefinitionId") UUID taskDefinitionId);

    @Query("SELECT DISTINCT wt.taskKey FROM WorkflowTask wt WHERE wt.workflow.id = :workflowId")
    Set<String> findAllTaskKeysByWorkflowId(@Param("workflowId") UUID workflowId);

    @Query("SELECT wt FROM WorkflowTask wt LEFT JOIN FETCH wt.taskDefinition WHERE wt.workflow.id = :workflowId ORDER BY wt.executionOrder ASC")
    List<WorkflowTask> findByWorkflowIdWithDefinitions(@Param("workflowId") UUID workflowId);

    @Query("SELECT COUNT(wt) FROM WorkflowTask wt WHERE wt.workflow.id = :workflowId AND wt.isEnabled = true")
    int countEnabledTasksByWorkflowId(@Param("workflowId") UUID workflowId);

    boolean existsByWorkflowIdAndTaskKey(UUID workflowId, String taskKey);

    void deleteByWorkflowId(UUID workflowId);
}
