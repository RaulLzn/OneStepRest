package com.onesteprest.onesteprest.service;

import com.onesteprest.onesteprest.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service for publishing entity lifecycle events.
 */
@Service
public class EventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    /**
     * Publishes a before-create event.
     * 
     * @param modelClass The model class
     * @param entity The entity being created
     * @return The potentially modified entity
     */
    public Object publishBeforeCreate(Class<?> modelClass, Object entity) {
        BeforeCreateEventImpl event = new BeforeCreateEventImpl(modelClass, entity);
        applicationEventPublisher.publishEvent(event);
        return event.getEntity();
    }
    
    /**
     * Publishes an after-create event.
     * 
     * @param modelClass The model class
     * @param entity The created entity
     */
    public void publishAfterCreate(Class<?> modelClass, Object entity) {
        applicationEventPublisher.publishEvent(
            new AfterCreateEventImpl(modelClass, entity));
    }
    
    /**
     * Publishes a before-update event.
     * 
     * @param modelClass The model class
     * @param entity The entity being updated
     * @param id The entity ID
     * @return The potentially modified entity
     */
    public Object publishBeforeUpdate(Class<?> modelClass, Object entity, Object id) {
        BeforeUpdateEventImpl event = new BeforeUpdateEventImpl(modelClass, entity, id);
        applicationEventPublisher.publishEvent(event);
        return event.getEntity();
    }
    
    /**
     * Publishes an after-update event.
     * 
     * @param modelClass The model class
     * @param entity The updated entity
     * @param id The entity ID
     */
    public void publishAfterUpdate(Class<?> modelClass, Object entity, Object id) {
        applicationEventPublisher.publishEvent(
            new AfterUpdateEventImpl(modelClass, entity, id));
    }
    
    // Internal implementations of the event interfaces
    
    public static class BeforeCreateEventImpl implements BeforeCreateEvent {
        private final Class<?> modelClass;
        private Object entity;
        
        public BeforeCreateEventImpl(Class<?> modelClass, Object entity) {
            this.modelClass = modelClass;
            this.entity = entity;
        }
        
        @Override
        public Class<?> getModelClass() {
            return modelClass;
        }
        
        @Override
        public Object getEntity() {
            return entity;
        }
        
        @Override
        public void setEntity(Object entity) {
            this.entity = entity;
        }
    }
    
    public static class AfterCreateEventImpl implements AfterCreateEvent {
        private final Class<?> modelClass;
        private final Object entity;
        
        public AfterCreateEventImpl(Class<?> modelClass, Object entity) {
            this.modelClass = modelClass;
            this.entity = entity;
        }
        
        @Override
        public Class<?> getModelClass() {
            return modelClass;
        }
        
        @Override
        public Object getEntity() {
            return entity;
        }
    }
    
    public static class BeforeUpdateEventImpl implements BeforeUpdateEvent {
        private final Class<?> modelClass;
        private Object entity;
        private final Object id;
        
        public BeforeUpdateEventImpl(Class<?> modelClass, Object entity, Object id) {
            this.modelClass = modelClass;
            this.entity = entity;
            this.id = id;
        }
        
        @Override
        public Class<?> getModelClass() {
            return modelClass;
        }
        
        @Override
        public Object getEntity() {
            return entity;
        }
        
        @Override
        public void setEntity(Object entity) {
            this.entity = entity;
        }
        
        @Override
        public Object getId() {
            return id;
        }
    }
    
    public static class AfterUpdateEventImpl implements AfterUpdateEvent {
        private final Class<?> modelClass;
        private final Object entity;
        private final Object id;
        
        public AfterUpdateEventImpl(Class<?> modelClass, Object entity, Object id) {
            this.modelClass = modelClass;
            this.entity = entity;
            this.id = id;
        }
        
        @Override
        public Class<?> getModelClass() {
            return modelClass;
        }
        
        @Override
        public Object getEntity() {
            return entity;
        }
        
        @Override
        public Object getId() {
            return id;
        }
    }
}