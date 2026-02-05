package com.flowforge.mapper;

import com.flowforge.domain.entity.Workflow;
import com.flowforge.domain.entity.WorkflowTask;
import com.flowforge.dto.request.CreateWorkflowRequest;
import com.flowforge.dto.response.WorkflowResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Workflow toEntity(CreateWorkflowRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "taskCount", expression = "java(workflow.getTasks() != null ? workflow.getTasks().size() : 0)")
    @Mapping(target = "scheduleCount", expression = "java(workflow.getSchedules() != null ? workflow.getSchedules().size() : 0)")
    WorkflowResponse toResponse(Workflow workflow);

    @Mapping(target = "taskDefinitionId", source = "taskDefinition.id")
    @Mapping(target = "taskDefinitionName", source = "taskDefinition.name")
    @Mapping(target = "name", expression = "java(workflowTask.getDisplayName())")
    WorkflowResponse.WorkflowTaskResponse toTaskResponse(WorkflowTask workflowTask);

    List<WorkflowResponse.WorkflowTaskResponse> toTaskResponseList(List<WorkflowTask> workflowTasks);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(CreateWorkflowRequest request, @MappingTarget Workflow workflow);
}
