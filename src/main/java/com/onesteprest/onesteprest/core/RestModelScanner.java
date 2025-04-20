package com.onesteprest.onesteprest.core;

import com.onesteprest.onesteprest.annotations.RestModel;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
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
        // Create properly configured Reflections instance
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .forPackage(basePackage)
            .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes));
            
        // Find all classes annotated with @RestModel
        return reflections.getTypesAnnotatedWith(RestModel.class);
    }
}