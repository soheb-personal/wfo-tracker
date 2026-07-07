package com.wfotracker.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TEMPLATE_ERROR_400 = "error/400";
    private static final String TEMPLATE_ERROR_500 = "error/500";
    private static final String ATTR_ERROR = "error";

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(NoResourceFoundException e, Model model) {
        log.debug("Static resource not found: {}", e.getResourcePath());
        model.addAttribute(ATTR_ERROR, "The requested static asset could not be found.");
        return TEMPLATE_ERROR_400;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, Model model) {
        log.warn("Illegal argument exception: {}", e.getMessage());
        model.addAttribute(ATTR_ERROR, e.getMessage());
        return TEMPLATE_ERROR_400;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public void handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception e, Model model) {
        log.error("Unexpected error occurred", e);
        model.addAttribute(ATTR_ERROR, "An unexpected error occurred. Please contact support.");
        return TEMPLATE_ERROR_500;
    }
}
