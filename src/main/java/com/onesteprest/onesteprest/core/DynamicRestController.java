package com.onesteprest.core;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic controller to handle CRUD operations dynamically.
 */
@RestController
@RequestMapping("/api")
public class DynamicRestController {

    // In-memory storage for demonstration purposes (replace with database integration)
    private final Map<String, Map<Long, Object>> storage = new HashMap<>();

    /**
     * Handles retrieving all entities of a given model type.
     *
     * @param model The model type (e.g., "productos").
     * @return A map of all entities of the given model type.
     */
    @GetMapping("/{model}")
    public Map<Long, Object> getAll(@PathVariable String model) {
        return storage.getOrDefault(model, new HashMap<>());
    }

    /**
     * Handles retrieving a single entity by its ID.
     *
     * @param model The model type (e.g., "productos").
     * @param id The ID of the entity.
     * @return The entity, or null if not found.
     */
    @GetMapping("/{model}/{id}")
    public Object getById(@PathVariable String model, @PathVariable Long id) {
        return storage.getOrDefault(model, new HashMap<>()).get(id);
    }

    /**
     * Handles creating a new entity.
     *
     * @param model The model type (e.g., "productos").
     * @param id The ID of the new entity.
     * @param entity The entity data.
     * @return A confirmation message.
     */
    @PostMapping("/{model}/{id}")
    public String create(@PathVariable String model, @PathVariable Long id, @RequestBody Object entity) {
        storage.computeIfAbsent(model, k -> new HashMap<>()).put(id, entity);
        return "Entity created successfully!";
    }

    /**
     * Handles updating an existing entity.
     *
     * @param model The model type (e.g., "productos").
     * @param id The ID of the entity.
     * @param entity The updated entity data.
     * @return A confirmation message.
     */
    @PutMapping("/{model}/{id}")
    public String update(@PathVariable String model, @PathVariable Long id, @RequestBody Object entity) {
        Map<Long, Object> entities = storage.getOrDefault(model, new HashMap<>());
        if (!entities.containsKey(id)) {
            return "Entity not found!";
        }
        entities.put(id, entity);
        return "Entity updated successfully!";
    }

    /**
     * Handles deleting an entity by its ID.
     *
     * @param model The model type (e.g., "productos").
     * @param id The ID of the entity.
     * @return A confirmation message.
     */
    @DeleteMapping("/{model}/{id}")
    public String delete(@PathVariable String model, @PathVariable Long id) {
        Map<Long, Object> entities = storage.getOrDefault(model, new HashMap<>());
        if (entities.remove(id) == null) {
            return "Entity not found!";
        }
        return "Entity deleted successfully!";
    }
}