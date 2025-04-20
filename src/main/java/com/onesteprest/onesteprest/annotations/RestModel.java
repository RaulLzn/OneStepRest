package com.onesteprest.annotations;

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
 * &#64;RestModel(path = "/productos")
 * public class Producto {
 *     // Class implementation
 * }
 * </pre>
 * 
 * The above example will generate REST endpoints such as "/productos/{id}".
 * 
 * Attributes:
 * - {@code path}: Specifies the base path for the REST model. This is a required attribute.
 */
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface RestModel {

    String path();
}