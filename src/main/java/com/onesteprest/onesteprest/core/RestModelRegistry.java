package com.onesteprest.core;

import com.onesteprest.annotations.RestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Dynamically registers REST models and connects them with the DynamicRestController.
 */
@Configuration
public class RestModelRegistry {

    @Autowired
    private RestModelScanner restModelScanner;

    /**
     * Registers all models annotated with @RestModel.
     */
    @PostConstruct
    public void registerModels() {
        String basePackage = "com.onesteprest.examples"; // Adjust this to your package structure
        Set<Class<?>> restModels = restModelScanner.scanForRestModels(basePackage);

        for (Class<?> modelClass : restModels) {
            RestModel restModel = modelClass.getAnnotation(RestModel.class);
            String basePath = restModel.path();

            // Log the registration for now
            System.out.println("Registering model: " + modelClass.getSimpleName() + " at path: " + basePath);

            // In future, connect these models dynamically to the DynamicRestController
        }
    }
}