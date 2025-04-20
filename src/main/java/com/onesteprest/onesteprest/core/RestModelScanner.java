package com.onesteprest.core;

import com.onesteprest.annotations.RestModel;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * Scans the project for classes annotated with @RestModel
 * and retrieves their base paths for REST endpoint registration.
 */
@Component
public class RestModelScanner {

    /**
     * Scans the given base package for classes annotated with @RestModel.
     *
     * @param basePackage the base package to scan
     * @return a set of classes annotated with @RestModel
     */
    public Set<Class<?>> scanForRestModels(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        // Find all classes annotated with @RestModel
        return reflections.getTypesAnnotatedWith(RestModel.class);
    }
}