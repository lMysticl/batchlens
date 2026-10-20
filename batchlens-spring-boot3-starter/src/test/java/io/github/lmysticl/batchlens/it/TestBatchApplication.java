package io.github.lmysticl.batchlens.it;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
class TestBatchApplication {
  /** The flaky job fails once and succeeds after a restart. */
  static final AtomicBoolean FAIL_NEXT = new AtomicBoolean(true);

  @Bean
  Job flaky(JobRepository repository, PlatformTransactionManager transactions) {
    var prepare =
        new StepBuilder("prepare", repository)
            .tasklet((contribution, context) -> RepeatStatus.FINISHED, transactions)
            .build();
    var load =
        new StepBuilder("load", repository)
            .tasklet(
                (contribution, context) -> {
                  if (FAIL_NEXT.getAndSet(false)) {
                    throw new IllegalStateException("Simulated failure in load");
                  }
                  return RepeatStatus.FINISHED;
                },
                transactions)
            .build();
    return new JobBuilder("flaky", repository).start(prepare).next(load).build();
  }
}
