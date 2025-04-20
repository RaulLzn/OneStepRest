package com.onesteprest.onesteprest.core;

import com.onesteprest.onesteprest.annotations.RestModel;
import com.onesteprest.onesteprest.config.OneStepRestConfig;
import com.onesteprest.onesteprest.service.DynamicEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Set;

/**
 * Dynamically registers REST models and connects them with the DynamicRestController.
 */
@Configuration
public class RestModelRegistry {

    @Autowired
    private RestModelScanner restModelScanner;
    
    @Autowired
    private DynamicEntityService entityService;
    
    @Autowired
    private OneStepRestConfig config;

    /**
     * Registers all models annotated with @RestModel.
     */
    @PostConstruct
    public void registerModels() {
        String basePackage = config.getDefaultModelPackage();
        try {
            Set<Class<?>> restModels = restModelScanner.scanForRestModels(basePackage);

            if (restModels.isEmpty()) {
                System.err.println("No models found annotated with @RestModel in package: " + basePackage);
                return;
            }

            for (Class<?> modelClass : restModels) {
                RestModel restModel = modelClass.getAnnotation(RestModel.class);
                String basePath = restModel.path();

                // Log the registration of the model
                System.out.println("Registering model: " + modelClass.getSimpleName() + " at path: " + basePath);

                // Register the model with the service
                entityService.registerModel(modelClass);
            }
        } catch (Exception e) {
            System.err.println("Error occurred while registering models: " + e.getMessage());
            e.printStackTrace();
        }
    }
}