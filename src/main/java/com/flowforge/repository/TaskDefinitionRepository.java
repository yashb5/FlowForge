package com.flowforge.repository;

import com.flowforge.domain.entity.TaskDefinition;
import com.flowforge.domain.enums.TaskType;
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
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, UUID> {

    Page<TaskDefinition> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<TaskDefinition> findByOwnerIdAndIsActiveTrue(UUID ownerId, Pageable pageable);

    Optional<TaskDefinition> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<TaskDefinition> findByTaskType(TaskType taskType, Pageable pageable);

    Page<TaskDefinition> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT t FROM TaskDefinition t WHERE t.isActive = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TaskDefinition> searchByNameOrDescription(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM TaskDefinition t JOIN t.tags tag WHERE tag IN :tags AND t.isActive = true")
    Page<TaskDefinition> findByTagsIn(@Param("tags") Set<String> tags, Pageable pageable);

    @Query("SELECT t FROM TaskDefinition t WHERE t.owner.id = :ownerId AND t.taskType = :taskType AND t.isActive = true")
    Page<TaskDefinition> findByOwnerAndTaskType(@Param("ownerId") UUID ownerId, 
                                                 @Param("taskType") TaskType taskType, 
                                                 Pageable pageable);

    @Query("SELECT DISTINCT tag FROM TaskDefinition t JOIN t.tags tag WHERE t.owner.id = :ownerId")
    Set<String> findAllTagsByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT COUNT(t) FROM TaskDefinition t WHERE t.owner.id = :ownerId AND t.isActive = true")
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT t FROM TaskDefinition t WHERE t.id IN :ids AND t.isActive = true")
    List<TaskDefinition> findAllActiveByIds(@Param("ids") Set<UUID> ids);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
