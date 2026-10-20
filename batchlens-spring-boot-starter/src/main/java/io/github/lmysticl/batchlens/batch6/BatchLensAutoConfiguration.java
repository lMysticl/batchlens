package io.github.lmysticl.batchlens.batch6;

import io.github.lmysticl.batchlens.BatchLensConfiguration;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Opt-in BatchLens for Spring Boot 4.1 and Spring Batch 6.0. */
@AutoConfiguration(
    afterName = {
      "org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration",
      "org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration"
    })
@ConditionalOnProperty(name = "batchlens.enabled", havingValue = "true")
@Import(BatchLensConfiguration.class)
public class BatchLensAutoConfiguration {
  @Bean
  SpringBatch6Adapter batchLensSpringBatch(
      JobRepository repository, JobOperator operator, ObjectProvider<Job> jobs) {
    return new SpringBatch6Adapter(repository, operator, jobs);
  }
}
