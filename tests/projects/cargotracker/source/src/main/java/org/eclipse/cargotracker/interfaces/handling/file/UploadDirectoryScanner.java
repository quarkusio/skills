package org.eclipse.cargotracker.interfaces.handling.file;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically scans a certain directory for files and attempts to parse handling event
 * registrations from the contents by calling a batch job.
 *
 * <p>
 * Files that fail to parse are moved into a separate directory, successful files are deleted.
 */
// @PermitAll = no restrictions
// Spring Batch manages its own transactions per step ?
@Component
public class UploadDirectoryScanner {

  private JobLauncher jobLauncher;
  private Job eventFilesProcessorJob;
  private Logger logger;

  public UploadDirectoryScanner(JobLauncher jobLauncher, Job eventFilesProcessorJob,
      Logger logger) {
    this.jobLauncher = jobLauncher;
    this.eventFilesProcessorJob = eventFilesProcessorJob;
    this.logger = logger;
  }

  // Run every 2 minutes (same as "minute=*/2,hour=*")
  @Scheduled(cron = "0 */2 * * * *") // In production, run every fifteen minutes
  public void processFiles() {
    try {
      logger.log(Level.INFO, "Trying to launch batch.");
      JobParameters params = new JobParametersBuilder()
          .addString("JobID", String.valueOf(System.currentTimeMillis()))
          .toJobParameters();
      jobLauncher.run(eventFilesProcessorJob, params);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Job failed:", e);
    }
  }
}
