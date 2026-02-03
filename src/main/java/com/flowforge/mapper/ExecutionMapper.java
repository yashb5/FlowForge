package com.flowforge.mapper;

import com.flowforge.domain.entity.TaskExecution;
import com.flowforge.domain.entity.WorkflowExecution;
import com.flowforge.dto.response.WorkflowExecutionResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExecutionMapper {

    @Mapping(target = "workflowId", source = "workflow.id")
    @Mapping(target = "workflowName", source = "workflow.name")
    @Mapping(target = "scheduleId", source = "schedule.id")
    @Mapping(target = "scheduleName", source = "schedule.name")
    @Mapping(target = "triggeredById", source = "triggeredBy.id")
    @Mapping(target = "triggeredByUsername", source = "triggeredBy.username")
    @Mapping(target = "durationMs", expression = "java(execution.getDuration().toMillis())")
    @Mapping(target = "progressPercentage", expression = "java(execution.getProgressPercentage())")
    WorkflowExecutionResponse toResponse(WorkflowExecution execution);

    @Mapping(target = "taskKey", source = "workflowTask.taskKey")
    @Mapping(target = "taskName", expression = "java(taskExecution.getWorkflowTask().getDisplayName())")
    @Mapping(target = "durationMs", expression = "java(taskExecution.getExecutionDuration().toMillis())")
    WorkflowExecutionResponse.TaskExecutionResponse toTaskExecutionResponse(TaskExecution taskExecution);

    List<WorkflowExecutionResponse.TaskExecutionResponse> toTaskExecutionResponseList(List<TaskExecution> taskExecutions);
}
