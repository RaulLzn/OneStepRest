package com.onesteprest.onesteprest.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                // If it's a List (for ManyToMany or OneToMany)
                else if (relationshipData instanceof List && Collection.class.isAssignableFrom(field.getType())) {
                    processCollectionRelationship(entity, field, (List<?>) relationshipData, entityManager);
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
            // Check if this is a relationship IDs field (ends with "Ids")
            else if (key.endsWith("Ids") && dataMap.get(key) != null && dataMap.get(key) instanceof List) {
                String relationshipFieldName = key.substring(0, key.length() - 3); // Remove "Ids"
                
                // Try to find the actual relationship field
                try {
                    Field relationshipField = entityClass.getDeclaredField(relationshipFieldName);
                    if (isCollectionRelationship(relationshipField)) {
                        List<?> relatedIds = (List<?>) dataMap.get(key);
                        setCollectionRelationshipByIds(entity, relationshipField, relatedIds, entityManager);
                    }
                } catch (NoSuchFieldException e) {
                    // Field doesn't exist, skip
                    continue;
                }
            }
        }
    }
    
    /**
     * Processes a collection relationship value from request data.
     */
    private static <T> void processCollectionRelationship(T entity, Field field, List<?> items, EntityManager entityManager) throws Exception {
        Class<?> elementType = RelationshipUtil.getCollectionGenericType(field);
        if (elementType == null) {
            return;
        }
        
        // Create new collection if needed
        Collection<Object> collection;
        if (field.get(entity) == null) {
            if (List.class.isAssignableFrom(field.getType())) {
                collection = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(field.getType())) {
                collection = new HashSet<>();
            } else {
                return; // Unsupported collection type
            }
            field.set(entity, collection);
        } else {
            collection = (Collection<Object>) field.get(entity);
            collection.clear();  // Clear existing items
        }
        
        // Process each item in the list
        for (Object item : items) {
            if (item instanceof Map) {
                Map<String, Object> itemMap = (Map<String, Object>) item;
                if (itemMap.containsKey("id")) {
                    Object id = itemMap.get("id");
                    Object relatedEntity = entityManager.find(elementType, TypeConverter.convertToAppropriateType(id, getIdType(elementType)));
                    if (relatedEntity != null) {
                        collection.add(relatedEntity);
                    }
                }
            } else if (item instanceof Number) {
                // If it's just an ID
                Object relatedEntity = entityManager.find(elementType, TypeConverter.convertToAppropriateType(item, getIdType(elementType)));
                if (relatedEntity != null) {
                    collection.add(relatedEntity);
                }
            }
        }

        // Add code to maintain bidirectional integrity
        if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            // If this is the owning side (no mappedBy)
            if (manyToMany.mappedBy().isEmpty()) {
                for (Object relatedEntity : collection) {
                    // Find the inverse side field in the related entity
                    updateManyToManyInverseReference(entity, relatedEntity, field);
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
     * Sets a collection relationship field using a list of IDs.
     */
    private static <T> void setCollectionRelationshipByIds(T entity, Field relationshipField, List<?> idValues, EntityManager entityManager) throws Exception {
        relationshipField.setAccessible(true);
        
        // Skip if not a collection
        if (!Collection.class.isAssignableFrom(relationshipField.getType())) {
            return;
        }
        
        // Get element type
        Class<?> elementType = RelationshipUtil.getCollectionGenericType(relationshipField);
        if (elementType == null) {
            return;
        }
        
        // Create new collection if needed
        Collection<Object> collection;
        if (relationshipField.get(entity) == null) {
            if (List.class.isAssignableFrom(relationshipField.getType())) {
                collection = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(relationshipField.getType())) {
                collection = new HashSet<>();
            } else {
                return; // Unsupported collection type
            }
            relationshipField.set(entity, collection);
        } else {
            collection = (Collection<Object>) relationshipField.get(entity);
            collection.clear();  // Clear existing items
        }
        
        // Get ID type for the element
        Class<?> idType = getIdType(elementType);
        
        // Find and add each related entity by ID
        for (Object idValue : idValues) {
            Object typedId = TypeConverter.convertToAppropriateType(idValue, idType);
            if (typedId != null) {
                Object relatedEntity = entityManager.find(elementType, typedId);
                if (relatedEntity != null) {
                    collection.add(relatedEntity);
                }
            }
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
            
            Object relatedValue = field.get(entity);
            if (relatedValue == null) {
                continue;
            }
            
            // Handle ManyToOne relationship
            if (field.isAnnotationPresent(ManyToOne.class)) {
                updateOneToManyBackReference(entity, relatedValue, field);
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
            
            // Handle ManyToMany relationship
            else if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                String mappedBy = manyToMany.mappedBy();
                
                if (!mappedBy.isEmpty() && field.get(entity) instanceof Collection) {
                    // This is the inverse side, update the owning side
                    Collection<?> collection = (Collection<?>) field.get(entity);
                    for (Object item : collection) {
                        updateManyToManyOwningReference(entity, item, mappedBy);
                    }
                } else if (field.get(entity) instanceof Collection) {
                    // This is the owning side, update the inverse side if it exists
                    Collection<?> collection = (Collection<?>) field.get(entity);
                    for (Object item : collection) {
                        updateManyToManyInverseReference(entity, item, field);
                    }
                }
            }
            
            // Handle OneToOne relationship
            else if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                String mappedBy = oneToOne.mappedBy();
                
                if (!mappedBy.isEmpty()) {
                    setBackReference(relatedValue, mappedBy, entity);
                } else if (field.isAnnotationPresent(JoinColumn.class)) {
                    // This is the owning side, try to find mapped by on the other side
                    updateOneToOneBackReference(entity, relatedValue, field);
                }
            }
        }
    }
    
    /**
     * Updates the owning side of a ManyToMany relationship.
     */
    private static void updateManyToManyOwningReference(Object inverseEntity, Object owningEntity, String mappedByField) throws Exception {
        Field field = findField(owningEntity.getClass(), mappedByField);
        if (field == null || !field.isAnnotationPresent(ManyToMany.class)) {
            return;
        }
        
        field.setAccessible(true);
        Collection<Object> owningCollection = getOrCreateCollection(owningEntity, field);
        
        // Add the inverse entity if it doesn't already exist
        if (!owningCollection.contains(inverseEntity)) {
            owningCollection.add(inverseEntity);
        }
    }
    
    /**
     * Updates the inverse side of a ManyToMany relationship.
     */
    private static void updateManyToManyInverseReference(Object owningEntity, Object inverseEntity, Field owningField) throws Exception {
        // Find the inverse field (with mappedBy pointing to this field)
        for (Field field : inverseEntity.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ManyToMany.class)) {
                continue;
            }
            
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            String mappedBy = manyToMany.mappedBy();
            
            if (!mappedBy.isEmpty() && mappedBy.equals(owningField.getName())) {
                field.setAccessible(true);
                Collection<Object> inverseCollection = getOrCreateCollection(inverseEntity, field);
                
                // Add the owning entity if it doesn't already exist
                if (!inverseCollection.contains(owningEntity)) {
                    inverseCollection.add(owningEntity);
                }
                
                break;
            }
        }
    }
    
    /**
     * Gets or creates a collection for an entity's field.
     */
    private static Collection<Object> getOrCreateCollection(Object entity, Field field) throws Exception {
        field.setAccessible(true);
        Collection<Object> collection = (Collection<Object>) field.get(entity);
        
        if (collection == null) {
            if (List.class.isAssignableFrom(field.getType())) {
                collection = new ArrayList<>();
            } else {
                collection = new HashSet<>();
            }
            field.set(entity, collection);
        }
        
        return collection;
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
               field.isAnnotationPresent(ManyToMany.class) ||
               field.isAnnotationPresent(OneToOne.class);
    }
    
    /**
     * Checks if a field is a collection relationship field.
     */
    private static boolean isCollectionRelationship(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) && 
              (field.isAnnotationPresent(OneToMany.class) || 
               field.isAnnotationPresent(ManyToMany.class));
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