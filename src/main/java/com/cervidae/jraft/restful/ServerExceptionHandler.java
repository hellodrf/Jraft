package com.cervidae.jraft.restful;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.logging.Logger;

/**
 * @author AaronDu
 */
@RestControllerAdvice
public class ServerExceptionHandler {

    /* AOP exception handlers
    /* these handlers will only intercept exceptions thrown in @RestController.
    /* instead of crashing the application, an error message will be returned. */

    /**
     * All Other Exceptions: return "Internal Error"
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(Exception.class)
    public Response<?> allExceptionHandler(Exception e) {
        Logger.getGlobal().severe("Unexpected exception caught: " + e.getClass().getName());
        return Response.fail("1002", e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Response<?> UnauthenticatedExceptionHandler(Exception e) {
        return Response.fail("3003");
    }

    /**
     * IllegalArgumentException: thrown in case of "illegal (logically)" arguments provided by the request
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<?> invalidParameterHandler(Exception e) {
        return Response.fail(e.getMessage());
    }

    /**
     * UnsatisfiedServletRequestParameterException: incorrect (number/type) arguments were provided by the request
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(UnsatisfiedServletRequestParameterException.class)
    public Response<?> unsatisfiedServletRequestParameterExceptionHandler(Exception e) {
        return Response.fail("1006");
    }

    /**
     * NoHandlerFoundException: thrown in case of 404 errors
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Response<?> noHandlerFoundExceptionHandler(Exception e) {
        return Response.fail("1404");
    }

}
