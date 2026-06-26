package com.cognitera.platform.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error page controller that extracts servlet error attributes and renders the error view.
 */
@Controller
public class ErrorPageController implements ErrorController {

    /**
     * Handles servlet errors by extracting status, message, exception, and request URI into the model.
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = status instanceof Integer ? (Integer) status : 500;
        model.addAttribute("status", statusCode);
        model.addAttribute("message", message != null ? message.toString() : "Unknown error");
        model.addAttribute("error", statusCode >= 500 ? "Internal Server Error" : "Bad Request");
        model.addAttribute("stacktrace", exception != null ? exception.toString() : null);
        model.addAttribute("requestUri", requestUri != null ? requestUri.toString() : null);

        return "error";
    }
}
