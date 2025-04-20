package com.onesteprest.onesteprest.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class that handles entity relationships automatically.
 * This allows for generic handling of entity relationships without
 * requiring model-specific code.
 */
public class EntityRelationshipManager {

    /**
     * Processes all relationship fields in the entity based on the request data.
     * This method looks for fields annotated with relationship annotations
     * and handles them appropriately.
     *
     * @param entity The entity object
     * @param requestData The raw request data (can be a Map or the entity itself)
     * @param entityManager The entity manager
     * @param <T> Type of entity
     * @return The processed entity
     */
    public static <T> T processRelationshipFields(T entity, Object requestData, EntityManager entityManager) {
        if (entity == null || requestData == null) {
            return entity;
        }

        try {
            Class<?> entityClass = entity.getClass();
            
            // If requestData is a Map, extract relationship IDs
            if (requestData instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) requestData;
                processMapRelationships(entity, dataMap, entityManager);
            }
            
            // Process existing relationships to ensure bidirectional consistency
            ensureBidirectionalRelationships(entity, entityManager);
            
            return entity;
        } catch (Exception e) {
            System.err.println("Error processing relationship fields: " + e.getMessage());
            e.printStackTrace();
            return entity;
        }
    }
    
    /**
     * Processes relationship IDs from a request data map.
     * Looks for fields ending with "Id" and maps them to their relationship fields.
     */
    private static <T> void processMapRelationships(T entity, Map<String, Object> dataMap, EntityManager entityManager) throws Exception {
        Class<?> entityClass = entity.getClass();
        
        // First pass: look for direct relationship objects
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            
            // Skip non-entity relationship fields
            if (!isEntityRelationship(field)) {
                continue;
            }
            
            String fieldName = field.getName();
            
            // Check if this relationship object is in the map
            if (dataMap.containsKey(fieldName) && dataMap.get(fieldName) != null) {
                Object relationshipData = dataMap.get(fieldName);
                
                // If it's a Map with an ID, load the entity
                if (relationshipData instanceof Map) {
                    Map<String, Object> relMap = (Map<String, Object>) relationshipData;
                    if (relMap.containsKey("id")) {
                        Object relatedId = relMap.get("id");
                        setRelationshipById(entity, field, relatedId, entityManager);
                    }
                }
            }
        }
        
        // Second pass: look for ID fields (fieldNameId)
        for (String key : dataMap.keySet()) {
            // Check if this is a relationship ID field (ends with "Id")
            if (key.endsWith("Id") && dataMap.get(key) != null) {
                String relationshipFieldName = key.substring(0, key.length() - 2); // Remove "Id"
                
                // Try to find the actual relationship field
                try {
                    Field relationshipField = entityClass.getDeclaredField(relationshipFieldName);
                    if (isEntityRelationship(relationshipField)) {
                        Object relatedId = dataMap.get(key);
                        setRelationshipById(entity, relationshipField, relatedId, entityManager);
                    }
                } catch (NoSuchFieldException e) {
                    // Field doesn't exist, skip
                    continue;
                }
            }
        }
    }
    
    /**
     * Sets a relationship field using an ID value.
     */
    private static <T> void setRelationshipById(T entity, Field relationshipField, Object idValue, EntityManager entityManager) throws Exception {
        relationshipField.setAccessible(true);
        
        // Get the type of related entity
        Class<?> relatedType = relationshipField.getType();
        
        // Convert ID to appropriate type if needed
        idValue = TypeConverter.convertToAppropriateType(idValue, getIdType(relatedType));
        
        // Skip if ID is null
        if (idValue == null) {
            relationshipField.set(entity, null);
            return;
        }
        
        // Load the related entity
        Object relatedEntity = entityManager.find(relatedType, idValue);
        
        // Set the relationship
        if (relatedEntity != null) {
            relationshipField.set(entity, relatedEntity);
        }
    }
    
    /**
     * Ensures that bidirectional relationships are properly maintained.
     */
    private static <T> void ensureBidirectionalRelationships(T entity, EntityManager entityManager) throws Exception {
        Class<?> entityClass = entity.getClass();
        
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            
            // Skip non-entity relationship fields or null values
            if (!isEntityRelationship(field)) {
                continue;
            }
            
            Object relatedEntity = field.get(entity);
            if (relatedEntity == null) {
                continue;
            }
            
            // Handle ManyToOne relationship
            if (field.isAnnotationPresent(ManyToOne.class)) {
                updateOneToManyBackReference(entity, relatedEntity, field);
            }
            
            // Handle OneToMany relationship
            else if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                String mappedBy = oneToMany.mappedBy();
                
                if (!mappedBy.isEmpty() && field.get(entity) instanceof Collection) {
                    Collection<?> collection = (Collection<?>) field.get(entity);
                    for (Object item : collection) {
                        setBackReference(item, mappedBy, entity);
                    }
                }
            }
            
            // Handle OneToOne relationship
            else if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                String mappedBy = oneToOne.mappedBy();
                
                if (!mappedBy.isEmpty()) {
                    setBackReference(relatedEntity, mappedBy, entity);
                } else if (field.isAnnotationPresent(JoinColumn.class)) {
                    // This is the owning side, try to find mapped by on the other side
                    updateOneToOneBackReference(entity, relatedEntity, field);
                }
            }
        }
    }
    
    /**
     * Updates the back-reference for a ManyToOne relationship.
     */
    private static void updateOneToManyBackReference(Object entity, Object relatedEntity, Field manyToOneField) throws Exception {
        // Find OneToMany field in the related entity that points back to this entity
        for (Field field : relatedEntity.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }
            
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            String mappedBy = oneToMany.mappedBy();
            
            // Check if this OneToMany maps back to our ManyToOne
            if (!mappedBy.isEmpty() && mappedBy.equals(manyToOneField.getName())) {
                field.setAccessible(true);
                Collection collection = (Collection) field.get(relatedEntity);
                
                // Add to collection if not already there
                if (collection != null && !collection.contains(entity)) {
                    collection.add(entity);
                }
                
                break;
            }
        }
    }
    
    /**
     * Updates the back-reference for a OneToOne relationship.
     */
    private static void updateOneToOneBackReference(Object entity, Object relatedEntity, Field oneToOneField) throws Exception {
        // Find OneToOne field in the related entity that points back to this entity
        for (Field field : relatedEntity.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(OneToOne.class)) {
                continue;
            }
            
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            String mappedBy = oneToOne.mappedBy();
            
            // Check if this OneToOne maps back to our OneToOne
            if (!mappedBy.isEmpty() && mappedBy.equals(oneToOneField.getName())) {
                field.setAccessible(true);
                field.set(relatedEntity, entity);
                break;
            }
        }
    }
    
    /**
     * Sets a back reference on a related entity.
     */
    private static void setBackReference(Object relatedEntity, String fieldName, Object entity) throws Exception {
        Field field = findField(relatedEntity.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(relatedEntity, entity);
        }
    }
    
    /**
     * Checks if a field is an entity relationship field.
     */
    private static boolean isEntityRelationship(Field field) {
        return field.isAnnotationPresent(ManyToOne.class) ||
               field.isAnnotationPresent(OneToMany.class) ||
               field.isAnnotationPresent(OneToOne.class);
    }
    
    /**
     * Gets the ID field type for a class.
     */
    private static Class<?> getIdType(Class<?> clazz) {
        Field idField = findIdField(clazz);
        return idField != null ? idField.getType() : Long.class; // Default to Long
    }
    
    /**
     * Finds the ID field for a class.
     */
    private static Field findIdField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
    
    /**
     * Finds a field in a class or its superclasses.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
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
}