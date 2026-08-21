package org.eclipse.cargotracker.interfaces.handling.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.application.util.DateConverter;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("EventItemWriter")
@Scope("prototype")
public class EventItemWriter implements ItemStreamWriter<HandlingEventRegistrationAttempt> {

  @Value("${batch.archive.directory}")
  private String archiveDirectoryPath;

  private StepExecution stepExecution;
  private ApplicationEvents applicationEvents;

  public EventItemWriter(ApplicationEvents applicationEvents) {
    this.applicationEvents = applicationEvents;
  }

  @BeforeStep
  public void captureStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public void open(ExecutionContext executionContext) {
    File archiveDir = new File(archiveDirectoryPath);
    if (!archiveDir.exists()) {
      boolean created = archiveDir.mkdirs();
      if (!created) {
        throw new RuntimeException("Failed to create archive directory: " + archiveDirectoryPath);
      }
    }
  }

  @Override
  public void write(Chunk<? extends HandlingEventRegistrationAttempt> items) throws Exception {
    String archiveFile = archiveDirectoryPath + "/archive_" +
        stepExecution.getJobExecution().getJobInstance().getJobName() + "_" +
        stepExecution.getJobExecution().getId() + ".csv";
    try (PrintWriter archive =
        new PrintWriter(new BufferedWriter(new FileWriter(archiveFile, true)))) {
      for (HandlingEventRegistrationAttempt attempt : items) {
        applicationEvents.receivedHandlingEventRegistrationAttempt(attempt);

        archive.println(
            DateConverter.toString(attempt.getRegistrationTime()) + "," +
                DateConverter.toString(attempt.getCompletionTime()) + "," +
                attempt.getTrackingId() + "," +
                attempt.getVoyageNumber() + "," +
                attempt.getUnLocode() + "," +
                attempt.getType());
      }
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    // Optional: write archive file path or counters if needed
  }

  @Override
  public void close() {
    // No-op unless you need to clean up
  }
}
