package com.onesteprest.onesteprest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Utility for converting between different data types.
 */
@Component
public class TypeConverter {
    
    private static ObjectMapper objectMapper;
    
    @Autowired
    private ObjectMapper autowiredMapper;
    
    @PostConstruct
    public void init() {
        TypeConverter.objectMapper = autowiredMapper;
    }
    
    /**
     * Converts a value to the appropriate type.
     *
     * @param value The value to convert
     * @param targetType The target type
     * @return The converted value
     */
    public static Object convertToAppropriateType(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        
        try {
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                } else if (value instanceof String) {
                    return Long.parseLong((String) value);
                } else if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                if (value instanceof Long) {
                    return ((Long) value).intValue();
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                if (value instanceof Integer) {
                    return ((Integer) value).doubleValue();
                } else if (value instanceof String) {
                    return Double.parseDouble((String) value);
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
            } else if (targetType.equals(String.class)) {
                return value.toString();
            }
            
            // Use Jackson for more complex conversions if needed
            if (objectMapper != null) {
                return objectMapper.convertValue(value, targetType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert " + value + " to " + targetType.getName(), e);
        }
        
        throw new IllegalArgumentException("Cannot convert " + value + " to " + targetType.getName());
    }
}