package com.cognitera.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Web MVC configuration that sets up locale resolution and a {@code CurrentUriAdvice} for internationalization.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Provides the session-based locale resolver with English as default.
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * Registers a locale change interceptor using the {@code lang} query parameter.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        registry.addInterceptor(lci);
    }

    /**
     * Controller advice that exposes the current request URI as a model attribute for language switching links.
     */
    @ControllerAdvice
    static class CurrentUriAdvice {

        /**
         * Returns the current request URI with a bare query string ready for lang parameter appending.
         */
        @ModelAttribute("currentUri")
        String currentUri(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String qs = request.getQueryString();
            if (qs != null && !qs.isBlank()) {
                // Strip existing lang param to avoid doubling
                qs = qs.replaceAll("&?lang=[^&]*", "").replaceAll("^&", "");
                if (!qs.isBlank()) {
                    uri = uri + "?" + qs + "&";
                } else {
                    uri = uri + "?";
                }
            } else {
                uri = uri + "?";
            }
            return uri;
        }
    }
}
