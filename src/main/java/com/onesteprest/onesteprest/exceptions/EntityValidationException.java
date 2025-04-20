package com.onesteprest.onesteprest.exceptions;

import java.util.Map;

/**
 * Exception thrown when entity validation fails.
 */
public class EntityValidationException extends RuntimeException {
    
    private final Map<String, String> errors;
    
    public EntityValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}