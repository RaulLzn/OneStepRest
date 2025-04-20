package com.onesteprest.onesteprest.events;

/**
 * Event fired after an entity is updated.
 */
public interface AfterUpdateEvent extends EntityEvent {
    /**
     * Gets the ID of the entity that was updated.
     */
    Object getId();
}