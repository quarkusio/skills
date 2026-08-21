package org.eclipse.cargotracker.interfaces.handling.file;

import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfig {

  @Bean
  public Job eventFilesProcessorJob(JobRepository jobRepository, Step processFilesStep,
      FileProcessorJobListener listener) {
    return new JobBuilder("EventFilesProcessorJob", jobRepository)
        .start(processFilesStep)
        .listener(listener)
        .build();
  }

  @Bean
  public Step processFilesStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager, EventItemReader reader,
      EventItemWriter writer,
      LineParseExceptionListener listener) {
    return new StepBuilder("ProcessEventFiles", jobRepository)
        .<HandlingEventRegistrationAttempt, HandlingEventRegistrationAttempt>chunk(12,
            transactionManager)
        .reader(reader)
        .writer(writer)
        .stream(writer)
        .faultTolerant()
        .skip(EventLineParseException.class)
        .listener(listener)
        .build();
  }

}
