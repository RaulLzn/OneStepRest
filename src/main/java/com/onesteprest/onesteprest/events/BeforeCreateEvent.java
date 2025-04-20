package com.onesteprest.onesteprest.events;

/**
 * Event fired before an entity is created.
 */
public interface BeforeCreateEvent extends EntityEvent {
    /**
     * Allows modifying the entity before it's created.
     * 
     * @param entity The entity to be modified
     */
    void setEntity(Object entity);
}