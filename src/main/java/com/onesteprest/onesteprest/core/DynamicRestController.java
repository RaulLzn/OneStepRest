package com.onesteprest.onesteprest.core;

import com.onesteprest.onesteprest.config.OneStepRestConfig;
import com.onesteprest.onesteprest.filters.Filter;
import com.onesteprest.onesteprest.filters.FilterOperation;
import com.onesteprest.onesteprest.filters.FilterParser;
import com.onesteprest.onesteprest.filters.FilterSpecification;
import com.onesteprest.onesteprest.service.DynamicEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * A generic controller to handle CRUD operations dynamically.
 */
@RestController
@RequestMapping("${onesteprest.api-base-path:/api}")  // Default to /api if not configured
@Tag(name = "Dynamic Entity API", description = "REST API for dynamic entity management")
public class DynamicRestController {

    @Autowired
    private DynamicEntityService entityService;
    
    @Autowired
    private OneStepRestConfig config;
    
    @Autowired
    private FilterParser filterParser;
    
    /**
     * Get all entities of a specific model with optional pagination.
     */
    @GetMapping("/{model}") 
    @Operation(
        summary = "Get all entities of a specific model",
        description = "Retrieves all entities of the specified model type with optional pagination"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved entities"),
        @ApiResponse(responseCode = "404", description = "Model not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<?> getAll(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(required = false, defaultValue = "-1") int page,
            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "20") int size,
            @Parameter(description = "Sort direction (asc,desc)")
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @Parameter(description = "Sort by field")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "JSON filter specification")
            @RequestParam(required = false) String filter,
            @RequestParam Map<String, String[]> allParams) {
        
        try {
            System.out.println("Received request for model: " + model + " with parameters: " + allParams);
            
            // Parse filter specification
            FilterSpecification filterSpec = null;
            if (filter != null && !filter.isEmpty()) {
                // JSON format filter
                filterSpec = filterParser.parseFromJson(filter);
            } else {
                // Query parameter format filter
                filterSpec = filterParser.parseFromQueryParams(allParams);
            }
            
            if (filterSpec != null && filterSpec.hasFilters()) {
                System.out.println("Parsed " + filterSpec.getFilters().size() + 
                                   " filters with logic " + filterSpec.getLogic());
            } else {
                System.out.println("No filters parsed from request");
            }
            
            // Check if pagination is requested
            if (page >= 0) {
                Sort sort = Sort.unsorted();
                if (sortBy != null && !sortBy.isEmpty()) {
                    sort = direction.equalsIgnoreCase("desc") ? 
                        Sort.by(sortBy).descending() : 
                        Sort.by(sortBy).ascending();
                }
                
                Pageable pageable = PageRequest.of(page, size, sort);
                
                // Use filtered query if filters are provided, otherwise use standard query
                Page<Object> pageResult;
                if (filterSpec != null && filterSpec.hasFilters()) {
                    pageResult = entityService.findAllWithFilter(model, filterSpec, pageable);
                } else {
                    pageResult = entityService.findAll(model, pageable);
                }
                
                return ResponseEntity.ok(pageResult);
            } else {
                // For non-paginated requests
                if (filterSpec != null && filterSpec.hasFilters()) {
                    System.out.println("Applying filters to non-paginated request");
                    // Apply filters with a high page size
                    Pageable pageable = PageRequest.of(0, 10000);
                    Page<Object> pageResult = entityService.findAllWithFilter(model, filterSpec, pageable);
                    return ResponseEntity.ok(pageResult.getContent());
                } else {
                    // Return all items without pagination or filtering
                    List<Object> entities = entityService.findAll(model);
                    return ResponseEntity.ok(entities);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Get a specific entity by ID.
     */
    @GetMapping("/{model}/{id}")  // Simplified mapping
    @Operation(
        summary = "Get entity by ID",
        description = "Retrieves a specific entity by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the entity"),
        @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Object> getById(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model, 
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id) {
        
        Optional<Object> entity = entityService.findById(model, id);
        return entity.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new entity.
     */
    @PostMapping("/{model}")  // Simplified mapping
    @Operation(
        summary = "Create a new entity",
        description = "Creates a new entity of the specified model type"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully created the entity"),
        @ApiResponse(responseCode = "400", description = "Invalid input or validation failed", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Object> create(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model, 
            @Parameter(description = "The entity to create", required = true) 
            @RequestBody Object entity) {
        
        Object createdEntity = entityService.create(model, entity);
        return ResponseEntity.ok(createdEntity);
    }

    /**
     * Update an existing entity.
     */
    @PutMapping("/{model}/{id}")
    @Operation(
        summary = "Update an existing entity",
        description = "Updates an existing entity with the specified ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully updated the entity"),
        @ApiResponse(responseCode = "400", description = "Invalid input or validation failed", content = @Content),
        @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Object> update(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model, 
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id, 
            @Parameter(description = "The updated entity", required = true) 
            @RequestBody Object entity) {
        
        if (!entityService.findById(model, id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Object updatedEntity = entityService.update(model, id, entity);
        return ResponseEntity.ok(updatedEntity);
    }

    /**
     * Delete an existing entity.
     */
    @DeleteMapping("/{model}/{id}")
    @Operation(
        summary = "Delete an entity",
        description = "Deletes an entity with the specified ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Successfully deleted the entity"),
        @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model, 
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id) {
        
        if (!entityService.findById(model, id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        entityService.deleteById(model, id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get related entities for a specific relationship.
     */
    @GetMapping("/{model}/{id}/{relationship}")
    @Operation(
        summary = "Get related entities",
        description = "Retrieves related entities for a specific relationship"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved related entities"),
        @ApiResponse(responseCode = "404", description = "Entity or relationship not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Object> getRelated(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model,
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id,
            @Parameter(description = "The relationship name", required = true) 
            @PathVariable String relationship) {
        
        Optional<Object> related = entityService.findRelated(model, id, relationship);
        return related.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Add an entity to a relationship.
     */
    @PostMapping("/{model}/{id}/{relationship}")
    @Operation(
        summary = "Add to relationship",
        description = "Adds an entity to a relationship"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully added to relationship"),
        @ApiResponse(responseCode = "404", description = "Entity or relationship not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Object> addToRelationship(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model,
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id,
            @Parameter(description = "The relationship name", required = true) 
            @PathVariable String relationship,
            @Parameter(description = "The entity or ID to add", required = true) 
            @RequestBody Object relatedEntity) {
        
        Optional<Object> updated = entityService.addToRelationship(model, id, relationship, relatedEntity);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Remove an entity from a relationship.
     */
    @DeleteMapping("/{model}/{id}/{relationship}/{relatedId}")
    @Operation(
        summary = "Remove from relationship",
        description = "Removes an entity from a relationship"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Successfully removed from relationship"),
        @ApiResponse(responseCode = "404", description = "Entity, related entity, or relationship not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    public ResponseEntity<Void> removeFromRelationship(
            @Parameter(description = "The model name", required = true) 
            @PathVariable String model,
            @Parameter(description = "The entity ID", required = true) 
            @PathVariable Object id,
            @Parameter(description = "The relationship name", required = true) 
            @PathVariable String relationship,
            @Parameter(description = "The ID of the related entity to remove", required = true) 
            @PathVariable Object relatedId) {
        
        boolean removed = entityService.removeFromRelationship(model, id, relationship, relatedId);
        if (removed) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}