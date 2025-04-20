package com.onesteprest.onesteprest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onesteprest.onesteprest.annotations.RestModel;
import com.onesteprest.onesteprest.exceptions.EntityValidationException;
import com.onesteprest.onesteprest.utils.RelationshipUtil;
import com.onesteprest.onesteprest.utils.EntityRelationshipManager;
import com.onesteprest.onesteprest.utils.TypeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Service to handle dynamic entity operations.
 */
@Service
public class DynamicEntityService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ValidationService validationService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Maps path segment (e.g. "productos") to its Class
    private final Map<String, Class<?>> modelMap = new HashMap<>();
    
    // Maps Class to its validation setting
    private final Map<Class<?>, Boolean> validationEnabledMap = new HashMap<>();
    
    // Default relationship depth
    private static final int DEFAULT_RELATIONSHIP_DEPTH = 2;
    
    /**
     * Registers a model class for use with dynamic REST endpoints.
     *
     * @param modelClass The class to register
     */
    public void registerModel(Class<?> modelClass) {
        RestModel annotation = modelClass.getAnnotation(RestModel.class);
        if (annotation != null) {
            String path = annotation.path();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            modelMap.put(path, modelClass);
            validationEnabledMap.put(modelClass, annotation.enableValidation());
            System.out.println("Registered model class: " + modelClass.getName() + " for path: " + path);
        }
    }
    
    /**
     * Finds all entities of a given model type with pagination.
     *
     * @param modelPath The path segment for the model
     * @param pageable Pagination information
     * @return Page of entities
     */
    @Transactional(readOnly = true)
    public Page<Object> findAll(String modelPath, Pageable pageable) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Create count query
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<?> root = countQuery.from(modelClass);
        countQuery.select(cb.count(root));
        
        Long count = entityManager.createQuery(countQuery).getSingleResult();
        
        // Create main query
        CriteriaQuery<Object> query = cb.createQuery((Class<Object>)modelClass);
        Root<?> queryRoot = query.from(modelClass);
        query.select(queryRoot);
        
        // Apply sorting if specified
        if (pageable.getSort().isSorted()) {
            // Apply sorting logic
        }
        
        // Execute query with pagination
        TypedQuery<?> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<?> resultList = typedQuery.getResultList();
        
        // Load relationships for each entity
        List<Object> enrichedResults = new ArrayList<>();
        for (Object entity : resultList) {
            enrichedResults.add(RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH));
        }
        
        return new PageImpl<>(enrichedResults, pageable, count);
    }
    
    /**
     * Finds all entities of a given model type.
     *
     * @param modelPath The path segment for the model
     * @return List of all entities
     */
    @Transactional(readOnly = true)
    public List<Object> findAll(String modelPath) {
        Class<?> modelClass = getModelClass(modelPath);
        String queryString = "SELECT e FROM " + modelClass.getSimpleName() + " e";
        TypedQuery<Object> query = entityManager.createQuery(queryString, (Class<Object>)modelClass);
        List<Object> resultList = query.getResultList();
        
        // Load relationships for each entity
        List<Object> enrichedResults = new ArrayList<>();
        for (Object entity : resultList) {
            enrichedResults.add(RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH));
        }
        
        return enrichedResults;
    }
    
    /**
     * Finds an entity by its ID.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     * @return Optional containing the entity if found
     */
    @Transactional(readOnly = true)
    public Optional<Object> findById(String modelPath, Object id) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type if needed
        Object typedId = convertToAppropriateType(id, getIdType(modelClass));
        
        Object entity = entityManager.find(modelClass, typedId);
        if (entity != null) {
            // For bidirectional relationships, we need to manually load collections
            // that are mapped by the other side
            loadMappedByCollections(entity);
            
            entity = RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH);
            return Optional.of(entity);
        }
        return Optional.empty();
    }
    
    /**
     * Loads collections that are mapped by the other side of a bidirectional relationship.
     * This is needed because JPA's lazy loading doesn't automatically populate these.
     *
     * @param entity The entity to load collections for
     */
    private void loadMappedByCollections(Object entity) {
        try {
            Class<?> entityClass = entity.getClass();
            Object entityId = getEntityId(entity);
            
            if (entityId == null) {
                return;
            }
            
            for (Field field : getAllFields(entityClass)) {
                if (Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    
                    // Check if it's a @OneToMany with mappedBy attribute
                    if (field.isAnnotationPresent(jakarta.persistence.OneToMany.class)) {
                        jakarta.persistence.OneToMany annotation = 
                            field.getAnnotation(jakarta.persistence.OneToMany.class);
                        
                        String mappedByField = annotation.mappedBy();
                        if (mappedByField != null && !mappedByField.isEmpty()) {
                            // Get the type of elements in the collection
                            Class<?> elementType = getCollectionElementType(field);
                            if (elementType != null) {
                                // Create a JPQL query to fetch the related entities
                                String jpql = "SELECT e FROM " + elementType.getSimpleName() + 
                                       " e WHERE e." + mappedByField + ".id = :parentId";
                                List<?> items = entityManager.createQuery(jpql)
                                                .setParameter("parentId", entityId)
                                                .getResultList();
                                
                                // Replace or initialize the collection
                                Collection<Object> collection = (Collection<Object>) field.get(entity);
                                if (collection == null) {
                                    // Create appropriate collection implementation
                                    if (List.class.isAssignableFrom(field.getType())) {
                                        collection = new ArrayList<>();
                                    } else if (Set.class.isAssignableFrom(field.getType())) {
                                        collection = new HashSet<>();
                                    } else {
                                        // Default to ArrayList for other collection types
                                        collection = new ArrayList<>();
                                    }
                                    field.set(entity, collection);
                                }
                                
                                // Clear and add all items
                                collection.clear();
                                for (Object item : items) {
                                    collection.add(item);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading mapped-by collections: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the element type of a collection field.
     *
     * @param field The collection field
     * @return The element type class
     */
    private Class<?> getCollectionElementType(Field field) {
        try {
            if (field.getGenericType() instanceof java.lang.reflect.ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    return (Class<?>) typeArgs[0];
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting collection element type: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets all fields from a class hierarchy.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
    
    /**
     * Creates a new entity.
     *
     * @param modelPath The path segment for the model
     * @param requestData The entity data
     * @return The created entity
     */
    @Transactional
    public Object create(String modelPath, Object requestData) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert from Map to actual entity class if needed
        Object entity;
        if (requestData instanceof Map) {
            entity = convertToEntityObject(requestData, modelClass);
        } else if (modelClass.isInstance(requestData)) {
            entity = requestData;
        } else {
            throw new IllegalArgumentException("Invalid entity data type: " + requestData.getClass().getName());
        }
        
        // Process relationships
        entity = EntityRelationshipManager.processRelationshipFields(entity, requestData, entityManager);
        
        // Pre-process entity
        entity = eventPublisher.publishBeforeCreate(modelClass, entity);
        
        // Validate if enabled
        if (validationEnabledMap.getOrDefault(modelClass, true)) {
            Map<String, String> validationErrors = validationService.validate(entity);
            if (!validationErrors.isEmpty()) {
                throw new EntityValidationException("Validation failed for " + modelClass.getSimpleName(), 
                                                 validationErrors);
            }
        }
        
        // Save entity
        entityManager.persist(entity);
        entityManager.flush();
        
        // Post-process entity
        entity = RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH);
        eventPublisher.publishAfterCreate(modelClass, entity);
        
        return entity;
    }
    
    /**
     * Updates an existing entity.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     * @param requestData The updated entity data
     * @return The updated entity
     */
    @Transactional
    public Object update(String modelPath, Object id, Object requestData) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type
        Object typedId = TypeConverter.convertToAppropriateType(id, getIdType(modelClass));
        
        // Check if entity exists
        Object existingEntity = entityManager.find(modelClass, typedId);
        if (existingEntity == null) {
            throw new IllegalArgumentException("Entity not found with ID: " + id);
        }
        
        // Convert from Map to actual entity class if needed
        Object entity;
        if (requestData instanceof Map) {
            entity = convertToEntityObject(requestData, modelClass);
            // Ensure the ID is set correctly
            setEntityId(entity, typedId);
        } else if (modelClass.isInstance(requestData)) {
            entity = requestData;
            // Ensure the ID is set correctly
            setEntityId(entity, typedId);
        } else {
            throw new IllegalArgumentException("Invalid entity data type: " + requestData.getClass().getName());
        }
        
        // Process relationships
        entity = EntityRelationshipManager.processRelationshipFields(entity, requestData, entityManager);
        
        // Pre-process entity
        entity = eventPublisher.publishBeforeUpdate(modelClass, entity, typedId);
        
        // Validate if enabled
        if (validationEnabledMap.getOrDefault(modelClass, true)) {
            Map<String, String> validationErrors = validationService.validate(entity);
            if (!validationErrors.isEmpty()) {
                throw new EntityValidationException("Validation failed for " + modelClass.getSimpleName(), 
                                                 validationErrors);
            }
        }
        
        // Update entity
        Object updatedEntity = entityManager.merge(entity);
        entityManager.flush();
        
        // Post-process entity
        updatedEntity = RelationshipUtil.loadRelationships(updatedEntity, entityManager, DEFAULT_RELATIONSHIP_DEPTH);
        eventPublisher.publishAfterUpdate(modelClass, updatedEntity, typedId);
        
        return updatedEntity;
    }
    
    /**
     * Deletes an entity by its ID.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     */
    @Transactional
    public void deleteById(String modelPath, Object id) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type
        Object typedId = convertToAppropriateType(id, getIdType(modelClass));
        
        // Find the entity
        Object entity = entityManager.find(modelClass, typedId);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }
    
    /**
     * Finds related entities for a specific relationship.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     * @param relationship The relationship name
     * @return Optional containing the related entity/entities
     */
    @Transactional(readOnly = true)
    public Optional<Object> findRelated(String modelPath, Object id, String relationship) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type
        Object typedId = convertToAppropriateType(id, getIdType(modelClass));
        
        // Find the entity
        Object entity = entityManager.find(modelClass, typedId);
        if (entity == null) {
            return Optional.empty();
        }
        
        // Get the related field
        try {
            Field field = findField(modelClass, relationship);
            if (field == null) {
                return Optional.empty();
            }
            
            field.setAccessible(true);
            Object relatedValue = field.get(entity);
            
            // Load relationships for the related entity/entities
            if (relatedValue != null) {
                if (Collection.class.isAssignableFrom(relatedValue.getClass())) {
                    // Handle collection
                    Collection<?> collection = (Collection<?>) relatedValue;
                    List<Object> loadedItems = new ArrayList<>();
                    for (Object item : collection) {
                        loadedItems.add(RelationshipUtil.loadRelationships(item, entityManager, DEFAULT_RELATIONSHIP_DEPTH - 1));
                    }
                    return Optional.of(loadedItems);
                } else {
                    // Handle single entity
                    return Optional.of(RelationshipUtil.loadRelationships(relatedValue, entityManager, DEFAULT_RELATIONSHIP_DEPTH - 1));
                }
            }
            
            return Optional.of(relatedValue); // Could be null
        } catch (Exception e) {
            throw new RuntimeException("Error accessing relationship: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds an entity to a relationship.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     * @param relationship The relationship name
     * @param relatedEntity The entity to add
     * @return Optional containing the updated entity
     */
    @Transactional
    public Optional<Object> addToRelationship(String modelPath, Object id, String relationship, Object relatedEntity) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type
        Object typedId = convertToAppropriateType(id, getIdType(modelClass));
        
        // Find the entity
        Object entity = entityManager.find(modelClass, typedId);
        if (entity == null) {
            return Optional.empty();
        }
        
        // Get the related field
        try {
            Field field = findField(modelClass, relationship);
            if (field == null) {
                return Optional.empty();
            }
            
            field.setAccessible(true);
            
            // Handle based on field type
            if (Collection.class.isAssignableFrom(field.getType())) {
                // Handle collection relationship
                Collection<Object> collection = (Collection<Object>) field.get(entity);
                if (collection == null) {
                    // Create new collection if null
                    if (List.class.isAssignableFrom(field.getType())) {
                        collection = new ArrayList<>();
                    } else {
                        collection = new HashSet<>();
                    }
                    field.set(entity, collection);
                }
                
                // Convert related entity if needed
                Class<?> elementType = RelationshipUtil.getCollectionGenericType(field);
                Object relatedObject;
                if (relatedEntity instanceof Map) {
                    relatedObject = convertToEntityObject(relatedEntity, elementType);
                } else if (elementType.isInstance(relatedEntity)) {
                    relatedObject = relatedEntity;
                } else if (relatedEntity instanceof Number) {
                    // If just an ID was provided, load the entity
                    Object relatedId = convertToAppropriateType(relatedEntity, getIdType(elementType));
                    relatedObject = entityManager.find(elementType, relatedId);
                    if (relatedObject == null) {
                        throw new IllegalArgumentException("Related entity not found with ID: " + relatedEntity);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid related entity data");
                }
                
                // Add to collection
                collection.add(relatedObject);
                
                // Update the entity
                entityManager.merge(entity);
                entityManager.flush();
                
                return Optional.of(RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH));
            } else {
                // Handle single entity relationship
                Class<?> fieldType = field.getType();
                Object relatedObject;
                
                if (relatedEntity instanceof Map) {
                    relatedObject = convertToEntityObject(relatedEntity, fieldType);
                } else if (fieldType.isInstance(relatedEntity)) {
                    relatedObject = relatedEntity;
                } else if (relatedEntity instanceof Number) {
                    // If just an ID was provided, load the entity
                    Object relatedId = convertToAppropriateType(relatedEntity, getIdType(fieldType));
                    relatedObject = entityManager.find(fieldType, relatedId);
                    if (relatedObject == null) {
                        throw new IllegalArgumentException("Related entity not found with ID: " + relatedEntity);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid related entity data");
                }
                
                // Set the related entity
                field.set(entity, relatedObject);
                
                // Update the entity
                entityManager.merge(entity);
                entityManager.flush();
                
                return Optional.of(RelationshipUtil.loadRelationships(entity, entityManager, DEFAULT_RELATIONSHIP_DEPTH));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error modifying relationship: " + e.getMessage(), e);
        }
    }
    
    /**
     * Removes an entity from a relationship.
     *
     * @param modelPath The path segment for the model
     * @param id The entity ID
     * @param relationship The relationship name
     * @param relatedId The ID of the related entity to remove
     * @return true if successfully removed, false otherwise
     */
    @Transactional
    public boolean removeFromRelationship(String modelPath, Object id, String relationship, Object relatedId) {
        Class<?> modelClass = getModelClass(modelPath);
        
        // Convert id to the appropriate type
        Object typedId = convertToAppropriateType(id, getIdType(modelClass));
        
        // Find the entity
        Object entity = entityManager.find(modelClass, typedId);
        if (entity == null) {
            return false;
        }
        
        // Get the related field
        try {
            Field field = findField(modelClass, relationship);
            if (field == null) {
                return false;
            }
            
            field.setAccessible(true);
            
            // Handle based on field type
            if (Collection.class.isAssignableFrom(field.getType())) {
                // Handle collection relationship
                Collection<Object> collection = (Collection<Object>) field.get(entity);
                if (collection == null || collection.isEmpty()) {
                    return false;
                }
                
                // Get element type
                Class<?> elementType = RelationshipUtil.getCollectionGenericType(field);
                Object typedRelatedId = convertToAppropriateType(relatedId, getIdType(elementType));
                
                // Find and remove the matching element
                Iterator<Object> iterator = collection.iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    Object itemId = getEntityId(item);
                    if (itemId != null && itemId.equals(typedRelatedId)) {
                        iterator.remove();
                        
                        // Update the entity
                        entityManager.merge(entity);
                        entityManager.flush();
                        return true;
                    }
                }
                
                return false;
            } else {
                // Handle single entity relationship
                Object currentValue = field.get(entity);
                if (currentValue == null) {
                    return false;
                }
                
                Object currentId = getEntityId(currentValue);
                Object typedRelatedId = convertToAppropriateType(relatedId, getIdType(currentValue.getClass()));
                
                if (currentId != null && currentId.equals(typedRelatedId)) {
                    // Set the field to null (remove the relationship)
                    field.set(entity, null);
                    
                    // Update the entity
                    entityManager.merge(entity);
                    entityManager.flush();
                    return true;
                }
                
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error removing from relationship: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the class for a given model path.
     *
     * @param modelPath The path segment for the model
     * @return The class
     * @throws IllegalArgumentException if model not found
     */
    private Class<?> getModelClass(String modelPath) {
        Class<?> modelClass = modelMap.get(modelPath);
        if (modelClass == null) {
            throw new IllegalArgumentException("Model not found for path: " + modelPath);
        }
        return modelClass;
    }
    
    /**
     * Converts request data to the appropriate entity class.
     */
    private Object convertToEntityObject(Object data, Class<?> targetClass) {
        try {
            return objectMapper.convertValue(data, targetClass);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert data to " + targetClass.getName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a value to the appropriate type.
     */
    private Object convertToAppropriateType(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        
        try {
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                } else if (value instanceof String) {
                    return Long.parseLong((String) value);
                }
            } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                if (value instanceof Long) {
                    return ((Long) value).intValue();
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
            }
            
            // Use Jackson for more complex conversions
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert " + value + " to " + targetType.getName(), e);
        }
    }
    
    /**
     * Finds a field in a class by name.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Gets the ID field type for a class.
     */
    private Class<?> getIdType(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    return field.getType();
                }
            }
            current = current.getSuperclass();
        }
        return Long.class; // Default to Long if not found
    }
    
    /**
     * Sets the ID field value for an entity.
     */
    private void setEntityId(Object entity, Object idValue) {
        try {
            Class<?> entityClass = entity.getClass();
            Field idField = findIdField(entityClass);
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, idValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds the ID field for a class.
     */
    private Field findIdField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
    
    /**
     * Gets the ID value from an entity.
     */
    private Object getEntityId(Object entity) {
        try {
            Field idField = findIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                return idField.get(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entity ID: " + e.getMessage(), e);
        }
        return null;
    }
}