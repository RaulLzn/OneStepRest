package com.onesteprest.onesteprest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Generic repository interface for dynamic entities.
 * This interface extends JpaRepository to provide CRUD operations.
 *
 * @param <T>  the type of the entity
 * @param <ID> the type of the entity's identifier
 */
@NoRepositoryBean
public interface DynamicEntityRepository<T, ID> extends JpaRepository<T, ID> {
    
}