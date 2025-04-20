package com.onesteprest.onesteprest.service;

import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for validating entity objects using Jakarta Bean Validation.
 */
@Service
public class ValidationService {

    private final Validator validator;

    public ValidationService() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**
     * Validates an entity object.
     *
     * @param entity The entity to validate
     * @param <T> The type of the entity
     * @return A map containing validation errors, or empty if valid
     */
    public <T> Map<String, String> validate(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        Map<String, String> errors = new HashMap<>();
        
        for (ConstraintViolation<T> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        
        return errors;
    }
    
    /**
     * Checks if an entity is valid according to its constraints.
     *
     * @param entity The entity to validate
     * @param <T> The type of the entity
     * @return true if valid, false otherwise
     */
    public <T> boolean isValid(T entity) {
        return validator.validate(entity).isEmpty();
    }
}