package com.example.mergab.Advice;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

@ControllerAdvice
public class AdviceController {

    // Our Exception
    @ExceptionHandler(value = ApiException.class)
    public ResponseEntity<?> ApiException(ApiException e) {
        String message = e.getMessage();
        return ResponseEntity.status(400).body(new ApiResponse(message));
    }

    // Server Validation Exception
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400).body(new ApiResponse(e.getFieldError().getDefaultMessage()));
    }

    // Server Validation Exception
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> ConstraintViolationException(ConstraintViolationException e) {

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // SQL Constraint Ex:(Duplicate) Exception
    @ExceptionHandler(value = SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ApiResponse> SQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // wrong write SQL in @column Exception
    @ExceptionHandler(value = InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ApiResponse> InvalidDataAccessResourceUsageException(InvalidDataAccessResourceUsageException e) {
        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // Database Constraint Exception
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> DataIntegrityViolationException(DataIntegrityViolationException e) {

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // Method not allowed Exception
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> HttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // Json parse Exception
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> HttpMessageNotReadableException(HttpMessageNotReadableException e) {

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // TypesMisMatch Exception
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> MethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse> RuntimeException(RuntimeException e) {
        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }


    // wrong path
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<?> NoResourceFoundException(NoResourceFoundException e){
        String message = e.getMessage();
        return ResponseEntity.status(400).body(message);
    }

    // using text instead of json
    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> HttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e){
        String message = e.getMessage();
        return ResponseEntity.status(400).body(message);
    }

    // forger to add the variable in the path
    @ExceptionHandler(value = MissingPathVariableException.class)
    public ResponseEntity<?> MissingPathVariableException(MissingPathVariableException e){
        String message = e.getMessage();
        return ResponseEntity.status(400).body(message);
    }
}
