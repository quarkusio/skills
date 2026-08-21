package org.eclipse.cargotracker.interfaces.handling.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.cargotracker.application.util.DateConverter;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("EventItemReader")
@Scope("prototype")
public class EventItemReader implements ItemStreamReader<HandlingEventRegistrationAttempt> {

  @Value("${batch.upload.directory}")
  private String uploadDirectoryPath;

  private Logger logger;

  private EventFilesCheckpoint checkpoint;
  private RandomAccessFile currentFile;

  public EventItemReader(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void open(ExecutionContext executionContext) {
    File uploadDirectory = new File(uploadDirectoryPath);

    if (executionContext.containsKey("checkpoint")) {
      // not exactly the same
      this.checkpoint = (EventFilesCheckpoint) executionContext.get("checkpoint");
      logger.log(Level.INFO, "Starting from previous checkpoint");
    } else {
      this.checkpoint = new EventFilesCheckpoint();
      logger.log(Level.INFO, "Scanning upload directory: {0}", uploadDirectory);

      if (!uploadDirectory.exists()) {
        logger.log(Level.INFO, "Upload directory does not exist, creating it");
        uploadDirectory.mkdirs();
      } else {
        File[] files = uploadDirectory.listFiles();
        if (files != null) {
          checkpoint.setFiles(Arrays.asList(files));
        }
      }
    }

    File file = this.checkpoint.currentFile();

    if (file == null) {
      logger.log(Level.INFO, "No files to process");
      currentFile = null;
    } else {
      try {
        currentFile = new RandomAccessFile(file, "r");
        logger.log(Level.INFO, "Processing file: {0}", file);
        currentFile.seek(this.checkpoint.getFilePointer());
      } catch (IOException e) {
        throw new ItemStreamException(e);
      }
    }
  }

  @Override
  public HandlingEventRegistrationAttempt read() throws Exception {
    if (currentFile == null)
      return null;

    String line = currentFile.readLine();

    if (line != null) {
      checkpoint.setFilePointer(currentFile.getFilePointer());
      return parseLine(line);
    } else {
      logger.log(
          Level.INFO, "Finished processing file, deleting: {0}", checkpoint.currentFile());
      currentFile.close();
      checkpoint.currentFile().delete();
      File nextFile = checkpoint.nextFile();

      if (nextFile == null) {
        logger.log(Level.INFO, "No more files to process");
        return null;
      } else {
        currentFile = new RandomAccessFile(nextFile, "r");
        logger.log(Level.INFO, "Processing file: {0}", nextFile);
        return read();
      }
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    executionContext.put("checkpoint", this.checkpoint);
  }

  @Override
  public void close() {
    if (currentFile != null) {
      try {
        currentFile.close();
      } catch (IOException e) {
        throw new ItemStreamException(e);
      }
    }
  }

  private HandlingEventRegistrationAttempt parseLine(String line) throws EventLineParseException {
    String[] result = line.split(",");

    if (result.length != 5) {
      throw new EventLineParseException("Wrong number of data elements", line);
    }

    LocalDateTime completionTime = null;

    try {
      completionTime = DateConverter.toDateTime(result[0]);
    } catch (DateTimeParseException e) {
      throw new EventLineParseException("Cannot parse completion time", e, line);
    }

    TrackingId trackingId = null;

    try {
      trackingId = new TrackingId(result[1]);
    } catch (NullPointerException e) {
      throw new EventLineParseException("Cannot parse tracking ID", e, line);
    }

    VoyageNumber voyageNumber = null;

    try {
      if (!result[2].isEmpty()) {
        voyageNumber = new VoyageNumber(result[2]);
      }
    } catch (NullPointerException e) {
      throw new EventLineParseException("Cannot parse voyage number", e, line);
    }

    UnLocode unLocode = null;

    try {
      unLocode = new UnLocode(result[3]);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new EventLineParseException("Cannot parse UN location code", e, line);
    }

    HandlingEvent.Type eventType = null;

    try {
      eventType = HandlingEvent.Type.valueOf(result[4]);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new EventLineParseException("Cannot parse event type", e, line);
    }

    HandlingEventRegistrationAttempt attempt =
        new HandlingEventRegistrationAttempt(
            LocalDateTime.now(), completionTime, trackingId, voyageNumber, eventType, unLocode);

    return attempt;
  }

}
