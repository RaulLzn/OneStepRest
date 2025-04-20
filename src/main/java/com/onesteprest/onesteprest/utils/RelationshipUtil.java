package com.onesteprest.onesteprest.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Utility class for handling entity relationships.
 */
public class RelationshipUtil {

    /**
     * Loads related entities for a given entity.
     * 
     * @param entity The entity to load relationships for
     * @param entityManager The entity manager
     * @param depth Maximum depth to load (to prevent infinite recursion)
     * @return The entity with loaded relationships
     */
    public static Object loadRelationships(Object entity, EntityManager entityManager, int depth) {
        if (entity == null || depth <= 0) {
            return entity;
        }
        
        try {
            Class<?> entityClass = entity.getClass();
            Set<Object> processed = new HashSet<>(); // To prevent circular loading
            return loadRelationshipsRecursive(entity, entityManager, depth, processed);
        } catch (Exception e) {
            System.err.println("Error loading relationships: " + e.getMessage());
            e.printStackTrace();
            return entity;
        }
    }
    
    /**
     * Recursive helper method to load relationships while avoiding circular references
     */
    private static Object loadRelationshipsRecursive(Object entity, EntityManager entityManager, int depth, Set<Object> processed) {
        if (entity == null || depth <= 0 || processed.contains(entity)) {
            return entity;
        }
        
        processed.add(entity);
        
        try {
            Class<?> entityClass = entity.getClass();
            Object entityId = getEntityId(entity);
            
            // If the entity has an ID, load it from the database to ensure all relationships are populated
            if (entityId != null) {
                Object managedEntity = entityManager.find(entityClass, entityId);
                if (managedEntity != null) {
                    entity = managedEntity;
                }
            }
            
            for (Field field : getAllFields(entityClass)) {
                field.setAccessible(true);
                
                // Skip primitive types and simple types
                if (field.getType().isPrimitive() || 
                    field.getType().equals(String.class) ||
                    Number.class.isAssignableFrom(field.getType()) ||
                    field.getType().equals(Boolean.class) ||
                    field.getType().equals(Character.class)) {
                    continue;
                }
                
                // Handle collections
                if (Collection.class.isAssignableFrom(field.getType())) {
                    Collection<?> collection = (Collection<?>) field.get(entity);
                    if (collection != null && !collection.isEmpty()) {
                        // For collections, we need to eagerly load them if they're lazy
                        Class<?> elementType = getCollectionGenericType(field);
                        if (elementType != null) {
                            // If it's a JPA managed relationship, load the collection properly
                            Object id = getEntityId(entity);
                            if (id != null) {
                                // Try to load the entire collection in one go if possible
                                String mappedByField = getMappedByField(entityClass, field);
                                if (mappedByField != null) {
                                    String jpql = "SELECT e FROM " + elementType.getSimpleName() + 
                                           " e WHERE e." + mappedByField + ".id = :parentId";
                                    Query query = entityManager.createQuery(jpql);
                                    query.setParameter("parentId", id);
                                    List<?> items = query.getResultList();
                                    
                                    // Replace the collection with loaded items
                                    Collection<Object> loadedItems = new ArrayList<>();
                                    for (Object item : items) {
                                        loadedItems.add(loadRelationshipsRecursive(item, entityManager, depth - 1, processed));
                                    }
                                    
                                    replaceCollection(entity, field, loadedItems);
                                }
                            }
                        }
                    }
                }
                // Handle entity reference (non-collection)
                else if (!Collection.class.isAssignableFrom(field.getType())) {
                    Object relatedEntity = field.get(entity);
                    if (relatedEntity != null) {
                        field.set(entity, loadRelationshipsRecursive(relatedEntity, entityManager, depth - 1, processed));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in loadRelationshipsRecursive: " + e.getMessage());
            e.printStackTrace();
        }
        
        return entity;
    }
    
    /**
     * Replace a collection with loaded items
     */
    private static void replaceCollection(Object entity, Field field, Collection<Object> loadedItems) throws Exception {
        if (field.getType().isAssignableFrom(ArrayList.class)) {
            field.set(entity, new ArrayList<>(loadedItems));
        } else if (field.getType().isAssignableFrom(HashSet.class)) {
            field.set(entity, new HashSet<>(loadedItems));
        } else if (field.getType().isAssignableFrom(List.class)) {
            field.set(entity, new ArrayList<>(loadedItems));
        } else if (field.getType().isAssignableFrom(Set.class)) {
            field.set(entity, new HashSet<>(loadedItems));
        }
    }
    
    /**
     * Gets the mappedBy attribute from @OneToMany annotation if present
     */
    private static String getMappedByField(Class<?> entityClass, Field field) {
        try {
            if (field.isAnnotationPresent(jakarta.persistence.OneToMany.class)) {
                jakarta.persistence.OneToMany annotation = field.getAnnotation(jakarta.persistence.OneToMany.class);
                return annotation.mappedBy();
            }
        } catch (Exception e) {
            System.err.println("Error getting mappedBy: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets all fields from a class hierarchy.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
    
    /**
     * Gets the ID of an entity using reflection.
     * 
     * @param entity The entity
     * @return The ID value
     */
    public static Object getEntityId(Object entity) {
        try {
            if (entity == null) {
                return null;
            }
            
            Class<?> entityClass = entity.getClass();
            while (entityClass != null && entityClass != Object.class) {
                for (Field field : entityClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        return field.get(entity);
                    }
                }
                entityClass = entityClass.getSuperclass();
            }
        } catch (Exception e) {
            System.err.println("Error getting entity ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Gets the generic type of a collection field.
     * 
     * @param field The collection field
     * @return The generic type class
     */
    public static Class<?> getCollectionGenericType(Field field) {
        try {
            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                return (Class<?>) typeArgs[0];
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}