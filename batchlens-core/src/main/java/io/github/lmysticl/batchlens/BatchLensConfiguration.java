package io.github.lmysticl.batchlens;

import io.github.lmysticl.batchlens.catalog.BatchCatalog;
import io.github.lmysticl.batchlens.operations.BatchCommands;
import io.github.lmysticl.batchlens.spi.SpringBatchAdapter;
import io.github.lmysticl.batchlens.web.BatchApiAuthentication;
import io.github.lmysticl.batchlens.web.BatchApiErrors;
import io.github.lmysticl.batchlens.web.BatchController;
import io.github.lmysticl.batchlens.web.BatchPage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The beans every starter shares. A starter imports this configuration from its opt-in
 * auto-configuration and contributes the {@link SpringBatchAdapter} for its Spring Batch line.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BatchLensProperties.class)
public class BatchLensConfiguration {
  @Bean
  WebMvcConfigurer batchLensAuthentication(BatchLensProperties properties) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BatchApiAuthentication(properties));
      }
    };
  }

  @Bean
  BatchCatalog<?> batchCatalog(SpringBatchAdapter<?> batch) {
    return new BatchCatalog<>(batch);
  }

  @Bean
  BatchCommands<?> batchCommands(SpringBatchAdapter<?> batch, BatchLensProperties properties) {
    return new BatchCommands<>(batch, properties);
  }

  @Bean
  BatchController batchController(
      BatchCatalog<?> catalog,
      BatchCommands<?> commands,
      BatchLensProperties properties,
      Environment environment) {
    return new BatchController(catalog, commands, properties, environment);
  }

  @Bean
  BatchPage batchPage() {
    return new BatchPage();
  }

  @Bean
  BatchApiErrors batchApiErrors() {
    return new BatchApiErrors();
  }
}
