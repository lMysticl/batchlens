package io.github.lmysticl.batchlens;

import io.github.lmysticl.batchlens.catalog.BatchCatalog;
import io.github.lmysticl.batchlens.operations.BatchCommands;
import io.github.lmysticl.batchlens.web.BatchApiAuthentication;
import io.github.lmysticl.batchlens.web.BatchApiErrors;
import io.github.lmysticl.batchlens.web.BatchController;
import io.github.lmysticl.batchlens.web.BatchPage;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(
    afterName = "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration")
@ConditionalOnProperty(name = "batchlens.enabled", havingValue = "true")
@EnableConfigurationProperties(BatchLensProperties.class)
public class BatchLensAutoConfiguration {
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
  BatchCatalog batchCatalog(JobExplorer explorer, ObjectProvider<Job> jobs) {
    return new BatchCatalog(explorer, jobs);
  }

  @Bean
  BatchCommands batchCommands(
      BatchCatalog catalog,
      JobLauncher launcher,
      JobRepository repository,
      BatchLensProperties properties) {
    return new BatchCommands(catalog, launcher, repository, properties);
  }

  @Bean
  BatchController batchController(
      BatchCatalog catalog,
      BatchCommands commands,
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
