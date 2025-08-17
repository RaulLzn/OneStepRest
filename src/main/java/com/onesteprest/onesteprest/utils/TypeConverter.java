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
    
    // Date format patterns
    private static final String[] DATE_FORMATS = {
        "yyyy-MM-dd", 
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy"
    };

    private static final String[] DATETIME_FORMATS = {
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    };
    
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
                    String strValue = ((String) value).trim();
                    if (strValue.isEmpty()) return null;
                    return Long.parseLong(strValue);
                } else if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                if (value instanceof Long) {
                    return ((Long) value).intValue();
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    if (strValue.isEmpty()) return null;
                    return Integer.parseInt(strValue);
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                if (value instanceof Integer) {
                    return ((Integer) value).doubleValue();
                } else if (value instanceof Long) {
                    return ((Long) value).doubleValue();
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    if (strValue.isEmpty()) return null;
                    return Double.parseDouble(strValue);
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    if (strValue.isEmpty()) return null;
                    return Float.parseFloat(strValue);
                }
            } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                if (value instanceof String) {
                    String stringValue = ((String) value).toLowerCase().trim();
                    return stringValue.equals("true") || 
                           stringValue.equals("yes") || 
                           stringValue.equals("1") || 
                           stringValue.equals("on");
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
            } else if (targetType.equals(String.class)) {
                return value.toString();
            } else if (targetType == java.util.Date.class && value instanceof String) {
                // Try parsing date strings in various formats
                return parseDate((String) value);
            } else if (targetType == java.time.LocalDate.class && value instanceof String) {
                // Try parsing LocalDate
                return parseLocalDate((String) value);
            } else if (targetType == java.time.LocalDateTime.class && value instanceof String) {
                // Try parsing LocalDateTime
                return parseLocalDateTime((String) value);
            }
            
            // Use Jackson for more complex conversions if needed
            if (objectMapper != null) {
                try {
                    return objectMapper.convertValue(value, targetType);
                } catch (Exception e) {
                    System.err.println("Failed to convert using Jackson: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting " + value + " to " + targetType + ": " + e.getMessage());
        }
        
        return value; // Return original value if conversion failed
    }
    
    /**
     * Attempts to parse a string as a Date by trying various formats.
     */
    private static java.util.Date parseDate(String value) throws java.text.ParseException {
        if (value == null || value.isEmpty()) {
            return null;
        }

        java.text.ParseException lastException = null;
        for (String format : DATE_FORMATS) {
            try {
                return new java.text.SimpleDateFormat(format).parse(value);
            } catch (java.text.ParseException e) {
                lastException = e;
            }
        }

        for (String format : DATETIME_FORMATS) {
            try {
                return new java.text.SimpleDateFormat(format).parse(value);
            } catch (java.text.ParseException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        
        return null;
    }

    /**
     * Attempts to parse a string as a LocalDate by trying various formats.
     */
    private static java.time.LocalDate parseLocalDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        for (String format : DATE_FORMATS) {
            try {
                return java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern(format));
            } catch (Exception ignored) {
                // Try next format
            }
        }

        // Last attempt with default parser
        try {
            return java.time.LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse date: " + value, e);
        }
    }

    /**
     * Attempts to parse a string as a LocalDateTime by trying various formats.
     */
    private static java.time.LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        for (String format : DATETIME_FORMATS) {
            try {
                return java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern(format));
            } catch (Exception ignored) {
                // Try next format
            }
        }

        // Last attempt with default parser
        try {
            return java.time.LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse datetime: " + value, e);
        }
    }
}