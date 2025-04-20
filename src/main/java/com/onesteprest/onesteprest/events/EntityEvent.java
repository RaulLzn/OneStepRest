package com.onesteprest.onesteprest.events;

/**
 * Base interface for entity events.
 */
public interface EntityEvent {
    /**
     * Gets the model class associated with this event.
     */
    Class<?> getModelClass();
    
    /**
     * Gets the entity involved in this event.
     */
    Object getEntity();
}