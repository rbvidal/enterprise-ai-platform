package com.cognitera.platform.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables AI provider configuration properties. */
@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiConfig {
}
