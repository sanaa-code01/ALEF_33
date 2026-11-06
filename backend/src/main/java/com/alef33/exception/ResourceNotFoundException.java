package com.alef33.exception;

// Thrown when something is not found in the database (e.g. quote id 999)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " not found: " + id);
    }
}