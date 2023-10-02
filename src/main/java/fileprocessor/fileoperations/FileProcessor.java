package fileprocessor.fileoperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fileprocessor.fileoperations.exceptions.SourceFolderDoesNotExistException;

import java.io.*;
import java.util.Scanner;

public class FileProcessor {

	private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

	public static void main(String[] args) {
		String sourceFolder = "C:\\Users\\DishaSingh\\Documents\\source";
		String destinationFolder = "C:\\Users\\DishaSingh\\Documents\\destination";

		try {
			processFiles(sourceFolder, destinationFolder);
		} catch (FileNotFoundException e) {
			logger.error("Error processing files: {}", e.getMessage());
		} catch (SourceFolderDoesNotExistException e) {
			logger.error("Source folder does not exist or is empty: {}", e.getMessage());
		}
	}

	private static void processFiles(String sourceFolder, String destinationFolder)
			throws FileNotFoundException, SourceFolderDoesNotExistException {
		File sourceDir = new File(sourceFolder);
		File[] files = sourceDir.listFiles();

		if (files != null) {
			boolean csvOrTxtFileFound = false;

			for (File file : files) {
				if (file.getName().endsWith(".trg")) {
					String baseFileName = file.getName().replace(".trg", "");
					File csvFile = new File(sourceFolder, baseFileName + ".csv");
					File txtFile = new File(sourceFolder, baseFileName + ".txt");

					if (csvFile.exists()) {
						csvOrTxtFileFound = true;
						processCsvFile(csvFile, destinationFolder);
					} else if (txtFile.exists()) {
						csvOrTxtFileFound = true;
						processDataFile(txtFile, destinationFolder);
					}
				}
			}

			if (!csvOrTxtFileFound) {
				throw new FileNotFoundException(
						"No .csv or .txt file found for existing .trg files in the source folder.");
			}
		} else {
			throw new SourceFolderDoesNotExistException("Source folder does not exist or is empty.");
		}
	}

	private static void processCsvFile(File csvFile, String destinationFolder) {
		try (Scanner scanner = new Scanner(csvFile)) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			}

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split(",");

				if (parts.length == 2) {
					String folder = parts[0].trim();
					boolean createRevision = Boolean.parseBoolean(parts[1].trim());

					logger.info("Processing data files for folder: {}", folder);

					if (createRevision) {
						logger.info("Creating revisions for files in folder: {}", folder);
					} else {
						logger.info("Copying files to folder: {}", folder);
					}
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("CSV file not found: {}", e.getMessage());
		}
	}

	private static void processDataFile(File dataFile, String destinationFolder) {
		String sourceFileName = dataFile.getName();
		String destinationFileName = sourceFileName;

		if (sourceFileName.matches(".*\\.(txt|other_extension)")) {
			int revision = 1;
			File destinationFile = new File(destinationFolder, destinationFileName);
			while (destinationFile.exists()) {
				revision++;
				destinationFileName = sourceFileName.replaceFirst("$", "(" + revision + ").$0");
				destinationFile = new File(destinationFolder, destinationFileName);
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(dataFile));
					FileWriter writer = new FileWriter(destinationFile)) {

				String line;
				while ((line = reader.readLine()) != null) {
					writer.write(line);
					writer.write("\n");
				}

				logger.info("Copied and processed data file: {}", sourceFileName);
			} catch (IOException e) {
				logger.error("Error processing data file: {}", e.getMessage());
				writeErrorToFile(e.getMessage(), destinationFolder, sourceFileName);
			}
		} else {
			logger.info("Skipping unsupported data file: {}", sourceFileName);
		}
	}

	private static void writeErrorToFile(String errorMessage, String destinationFolder, String fileName) {
		File errFile = new File(destinationFolder, fileName.replaceFirst("[.][^.]+$", ".err"));

		try (FileWriter writer = new FileWriter(errFile, true)) {
			writer.write(errorMessage);
			writer.write("\n");
		} catch (IOException e) {
			logger.error("Error writing to error file: {}", e.getMessage());
		}
	}
}
