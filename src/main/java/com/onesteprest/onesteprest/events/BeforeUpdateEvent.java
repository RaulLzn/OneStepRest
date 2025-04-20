package com.onesteprest.onesteprest.events;

/**
 * Event fired before an entity is updated.
 */
public interface BeforeUpdateEvent extends EntityEvent {
    /**
     * Allows modifying the entity before it's updated.
     * 
     * @param entity The entity to be modified
     */
    void setEntity(Object entity);
    
    /**
     * Gets the ID of the entity being updated.
     */
    Object getId();
}