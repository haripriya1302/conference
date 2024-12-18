package com.example.conferencebooking.service.validator;

public interface Validator<T> {
    void validate(T entity);
} 