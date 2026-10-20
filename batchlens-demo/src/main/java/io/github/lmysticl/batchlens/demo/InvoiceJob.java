package io.github.lmysticl.batchlens.demo;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class InvoiceJob {
  @Bean
  @BatchTaskExecutor
  ThreadPoolTaskExecutor batchExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(4);
    executor.setThreadNamePrefix("invoice-import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    return executor;
  }

  @Bean
  @StepScope
  FlatFileItemReader<Invoice> invoiceReader() {
    return new FlatFileItemReaderBuilder<Invoice>()
        .name("invoiceReader")
        .resource(new ClassPathResource("invoices.csv"))
        .linesToSkip(1)
        .delimited()
        .names("id", "customer", "amount")
        .fieldSetMapper(
            fields ->
                new Invoice(
                    fields.readLong("id"),
                    fields.readString("customer"),
                    fields.readBigDecimal("amount")))
        .build();
  }

  @Bean
  @StepScope
  InvoiceProcessor invoiceProcessor(
      @Value("#{jobParameters['failAt'] ?: '137'}") String failAt,
      @Value("#{jobParameters['delayMs'] ?: '8'}") String delay,
      @Value("#{stepExecution}") StepExecution step,
      JobRepository repository) {
    boolean firstAttempt =
        repository.getJobExecutions(step.getJobExecution().getJobInstance()).size() == 1;
    return new InvoiceProcessor(Integer.parseInt(failAt), Integer.parseInt(delay), firstAttempt);
  }

  @Bean
  @StepScope
  JdbcBatchItemWriter<Invoice> invoiceWriter(
      DataSource source, @Value("#{jobParameters['run']}") String run) {
    if (run == null || run.isBlank() || run.length() > 64) {
      throw new IllegalArgumentException(
          "Supply a unique 'run' string parameter (1–64 characters)");
    }
    return new JdbcBatchItemWriterBuilder<Invoice>()
        .dataSource(source)
        .sql("INSERT INTO demo_invoice (run_key, invoice_id, customer, amount) VALUES (?, ?, ?, ?)")
        .itemPreparedStatementSetter(
            (invoice, statement) -> {
              statement.setString(1, run);
              statement.setLong(2, invoice.id());
              statement.setString(3, invoice.customer());
              statement.setBigDecimal(4, invoice.amount());
            })
        .build();
  }

  @Bean
  Step importInvoices(
      JobRepository repository,
      PlatformTransactionManager transactions,
      FlatFileItemReader<Invoice> invoiceReader,
      InvoiceProcessor invoiceProcessor,
      JdbcBatchItemWriter<Invoice> invoiceWriter) {
    return new StepBuilder("importInvoices", repository)
        .<Invoice, Invoice>chunk(20)
        .transactionManager(transactions)
        .reader(invoiceReader)
        .processor(invoiceProcessor)
        .writer(invoiceWriter)
        .build();
  }

  @Bean
  Step validateInput(
      JobRepository repository, PlatformTransactionManager transactions, JdbcTemplate jdbc) {
    return new StepBuilder("validateInput", repository)
        .tasklet(
            (contribution, context) -> {
              String run =
                  contribution
                      .getStepExecution()
                      .getJobExecution()
                      .getJobParameters()
                      .getString("run");
              if (run == null || run.isBlank() || run.length() > 64) {
                throw new IllegalArgumentException(
                    "Supply a unique 'run' string parameter (1–64 characters)");
              }
              if (jdbc.queryForObject(
                      "SELECT COUNT(*) FROM demo_invoice WHERE run_key=?", Long.class, run)
                  != 0) {
                throw new IllegalArgumentException(
                    "This demo run key already contains invoices. Choose a new run key.");
              }
              return RepeatStatus.FINISHED;
            })
        .transactionManager(transactions)
        .build();
  }

  @Bean
  Step reconcileInvoices(
      JobRepository repository, PlatformTransactionManager transactions, JdbcTemplate jdbc) {
    return new StepBuilder("reconcileInvoices", repository)
        .tasklet(
            (contribution, context) -> {
              String run =
                  contribution
                      .getStepExecution()
                      .getJobExecution()
                      .getJobParameters()
                      .getString("run");
              long count =
                  jdbc.queryForObject(
                      "SELECT COUNT(*) FROM demo_invoice WHERE run_key=?", Long.class, run);
              if (count != 300)
                throw new IllegalStateException("Expected 300 committed invoices; found " + count);
              return RepeatStatus.FINISHED;
            })
        .transactionManager(transactions)
        .build();
  }

  @Bean
  Job invoiceImport(
      JobRepository repository, Step validateInput, Step importInvoices, Step reconcileInvoices) {
    return new JobBuilder("invoiceImport", repository)
        .start(validateInput)
        .next(importInvoices)
        .next(reconcileInvoices)
        .build();
  }
}
