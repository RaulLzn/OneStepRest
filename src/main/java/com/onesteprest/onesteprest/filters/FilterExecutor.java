package com.onesteprest.onesteprest.filters;

import com.onesteprest.onesteprest.utils.TypeConverter;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import java.util.*;

/**
 * Componente que aplica filtros a las consultas JPA.
 * Construye predicados basados en las especificaciones de filtro.
 */
@Component
public class FilterExecutor {
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Aplica los filtros a un query builder JPA.
     *
     * @param root Root de la consulta JPA
     * @param query La consulta CriteriaQuery
     * @param cb El constructor de criterios
     * @param filterSpec La especificación del filtro a aplicar
     * @return La consulta con el where aplicado
     */
    public <T> CriteriaQuery<T> applyFilter(Root<?> root, CriteriaQuery<T> query, CriteriaBuilder cb, 
                                           FilterSpecification filterSpec) {
        if (filterSpec == null || !filterSpec.hasFilters()) {
            return query;
        }

        Predicate predicate = createPredicate(root, cb, filterSpec);
        if (predicate != null) {
            query.where(predicate);
        }
        
        return query;
    }

    /**
     * Crea un predicado a partir de una especificación de filtro.
     */
    public Predicate createPredicate(Root<?> root, CriteriaBuilder cb, FilterSpecification filterSpec) {
        if (filterSpec == null || !filterSpec.hasFilters()) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();
        
        for (Filter filter : filterSpec.getFilters()) {
            Predicate singlePredicate = createPredicateForFilter(root, cb, filter);
            if (singlePredicate != null) {
                predicates.add(singlePredicate);
            }
        }
        
        if (predicates.isEmpty()) {
            return null;
        }
        
        // Combinar predicados según la lógica (AND/OR)
        if (filterSpec.getLogic() == FilterSpecification.FilterLogic.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            return cb.and(predicates.toArray(new Predicate[0]));
        }
    }

    /**
     * Crea un predicado para un filtro individual.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate createPredicateForFilter(Root<?> root, CriteriaBuilder cb, Filter filter) {
        if (filter.getField() == null || filter.getOperation() == null) {
            return null;
        }
        
        System.out.println("Creating predicate for filter: " + filter);

        // Manejar caminos con puntos para relaciones anidadas
        Path<?> path = getPath(root, filter.getField());
        Class<?> fieldType = path.getJavaType();
        
        System.out.println("Field type for " + filter.getField() + ": " + fieldType.getName());

        // Convertir valor al tipo correcto para la comparación
        Object value = convertValue(filter.getValue(), fieldType);
        
        switch (filter.getOperation()) {
            case EQUAL:
                return value == null ? cb.isNull(path) : cb.equal(path, value);
                
            case NOT_EQUAL:
                return value == null ? cb.isNotNull(path) : cb.notEqual(path, value);
                
            case GREATER_THAN:
                if (value == null) return null;
                System.out.println("Creating GREATER_THAN predicate: " + path + " > " + value);
                return cb.greaterThan((Expression<Comparable>) path, (Comparable) value);
                
            case GREATER_THAN_OR_EQUAL:
                if (value == null) return null;
                return cb.greaterThanOrEqualTo((Expression<Comparable>) path, (Comparable) value);
                
            case LESS_THAN:
                if (value == null) return null;
                return cb.lessThan((Expression<Comparable>) path, (Comparable) value);
                
            case LESS_THAN_OR_EQUAL:
                if (value == null) return null;
                return cb.lessThanOrEqualTo((Expression<Comparable>) path, (Comparable) value);
                
            case LIKE:
                if (value == null) return null;
                String likeValue = value.toString();
                // Agregar comodines si no están presentes
                if (!likeValue.contains("%")) {
                    likeValue = "%" + likeValue + "%";
                }
                return cb.like((Expression<String>) path, likeValue);
                
            case IN:
                if (value == null) return null;
                
                if (value instanceof Collection) {
                    Collection<?> valueList = (Collection<?>) value;
                    if (valueList.isEmpty()) {
                        // Una lista IN vacía siempre da falso en SQL
                        return cb.disjunction();
                    }
                    
                    // Convertir cada valor al tipo del campo
                    List<Object> convertedValues = new ArrayList<>();
                    for (Object item : valueList) {
                        convertedValues.add(convertValue(item, fieldType));
                    }
                    
                    return path.in(convertedValues);
                }
                return null;
                
            case BETWEEN:
                if (value == null || filter.getSecondValue() == null) return null;
                Object secondValue = convertValue(filter.getSecondValue(), fieldType);
                
                return cb.between((Expression<Comparable>) path, 
                                 (Comparable) value, 
                                 (Comparable) secondValue);
                
            default:
                return null;
        }
    }

    /**
     * Obtiene un camino para un campo, soportando notación de punto para relaciones anidadas.
     */
    private Path<?> getPath(Root<?> root, String fieldPath) {
        if (fieldPath.contains(".")) {
            String[] parts = fieldPath.split("\\.");
            Path<?> path = root;
            
            for (String part : parts) {
                path = path.get(part);
            }
            
            return path;
        } else {
            return root.get(fieldPath);
        }
    }

    /**
     * Convierte un valor al tipo apropiado para la comparación.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        try {
            System.out.println("Converting value: " + value + " (" + value.getClass().getName() + ") to " + targetType.getName());
            Object result = TypeConverter.convertToAppropriateType(value, targetType);
            System.out.println("Conversion result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Error converting value: " + value + " to " + targetType + " - " + e.getMessage());
            e.printStackTrace();
            return value; // Return original value on error
        }
    }
}
