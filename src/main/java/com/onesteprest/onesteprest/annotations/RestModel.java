package com.onesteprest.onesteprest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a REST model for a class.
 * 
 * This annotation is used to specify the base path for REST endpoints
 * associated with the annotated class. It is intended to be applied
 * only to classes and is retained at runtime for reflection purposes.
 * 
 * Example usage:
 * <pre>
 * &#64;RestModel(path = "/productos", enableValidation = true)
 * public class Producto {
 *     &#64;NotNull
 *     private String nombre;
 *     
 *     &#64;Min(0)
 *     private Double precio;
 *     
 *     // Class implementation
 * }
 * </pre>
 * 
 * Attributes:
 * - {@code path}: Specifies the base path for the REST model. This is a required attribute.
 * - {@code enableValidation}: Enables or disables validation for this model. Default is true.
 */
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface RestModel {

    String path();
    
    /**
     * Whether to enable validation for this model.
     * When enabled, validation will be performed on create and update operations.
     * Default is true.
     */
    boolean enableValidation() default true;
}