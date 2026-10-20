package io.github.lmysticl.batchlens.batch5;

import io.github.lmysticl.batchlens.BatchLensConfiguration;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Opt-in BatchLens for Spring Boot 3.5 and Spring Batch 5.2. */
@AutoConfiguration(
    afterName = "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration")
@ConditionalOnProperty(name = "batchlens.enabled", havingValue = "true")
@Import(BatchLensConfiguration.class)
public class BatchLensAutoConfiguration {
  @Bean
  SpringBatch5Adapter batchLensSpringBatch(
      JobExplorer explorer,
      JobRepository repository,
      JobLauncher launcher,
      ObjectProvider<Job> jobs) {
    return new SpringBatch5Adapter(explorer, repository, launcher, jobs);
  }
}
