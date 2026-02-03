package com.flowforge.mapper;

import com.flowforge.domain.entity.Schedule;
import com.flowforge.dto.request.CreateScheduleRequest;
import com.flowforge.dto.response.ScheduleResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflow", ignore = true)
    @Mapping(target = "nextRunAt", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "executionCount", constant = "0")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isPaused", constant = "false")
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Schedule toEntity(CreateScheduleRequest request);

    @Mapping(target = "workflowId", source = "workflow.id")
    @Mapping(target = "workflowName", source = "workflow.name")
    ScheduleResponse toResponse(Schedule schedule);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflow", ignore = true)
    @Mapping(target = "nextRunAt", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "executionCount", ignore = true)
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(CreateScheduleRequest request, @MappingTarget Schedule schedule);
}
