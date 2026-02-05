package com.flowforge.mapper;

import com.flowforge.domain.entity.TaskDefinition;
import com.flowforge.dto.request.CreateTaskDefinitionRequest;
import com.flowforge.dto.response.TaskDefinitionResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TaskDefinitionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "workflowTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    TaskDefinition toEntity(CreateTaskDefinitionRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    TaskDefinitionResponse toResponse(TaskDefinition taskDefinition);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "workflowTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(CreateTaskDefinitionRequest request, @MappingTarget TaskDefinition taskDefinition);
}
