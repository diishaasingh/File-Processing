package fileprocessor.fileoperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fileprocessor.fileoperations.exceptions.SourceFolderDoesNotExistException;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class FileProcessor {

	private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
	private static final String trgFileExtension = ".trg";
	private static final String csvFileExtension = ".csv";
	private static final String errorFileExtension = ".err";
	// the extension of data file can be anything except the above three

	public static void main(String[] args) {
		String srcFolder = "C:\\Users\\DishaSingh\\Documents\\source";
		String destinationFolder = "C:\\Users\\DishaSingh\\Documents\\destination";

		try {
			logger.info("Started processing files from source folder");
			processFiles(srcFolder, destinationFolder);
		} catch (SourceFolderDoesNotExistException e) {
			logger.error("Source folder does not exist or is empty: {}", e.getMessage());
		}
	}

	private static void processFiles(String sourceFolder, String destinationFolder)
			throws SourceFolderDoesNotExistException {
		File sourceDir = new File(sourceFolder);
		File[] files = sourceDir.listFiles();

		if (files != null) {
			// Map to store trg filename without extension as key and data file name with
			// extension as value
			Map<String, String> trgToDataFileMap = new HashMap<>();
			Set<String> csvFileSet = new HashSet<>();
			Set<String> errorFileSet = new HashSet<>();
			Set<String> dataFileSet = new HashSet<>();

			// traversing the list of files in the source folder and adding the file name in
			// their respective data structures
			for (File file : files) {
				if (file.getName().endsWith(trgFileExtension)) {
					String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
					trgToDataFileMap.put(baseName, null);
				} else if (file.getName().endsWith(csvFileExtension)) {
					csvFileSet.add(file.getName());
				} else if (file.getName().endsWith(errorFileExtension)) {
					errorFileSet.add(file.getName());
				} else {
					dataFileSet.add(file.getName());
				}
			}

			for (String trgFileName : trgToDataFileMap.keySet()) {
				String csvFileName = trgFileName + csvFileExtension;
				if (csvFileSet.contains(csvFileName)) {
					for (String dataFileName : dataFileSet) {
						String extension = dataFileName.substring(dataFileName.lastIndexOf('.'));
						if (!extension.equals(trgFileExtension) && !extension.equals(csvFileExtension)
								&& !extension.equals(errorFileExtension)) {
							String dataBaseName = dataFileName.substring(0, dataFileName.lastIndexOf('.'));
							if (dataBaseName.equals(trgFileName))
								trgToDataFileMap.put(trgFileName, dataFileName);
						}
					}
				}
			}

			int countOfTrgFilesWithoutCsvOrDataFiles = 0;

			for (Map.Entry<String, String> entry : trgToDataFileMap.entrySet()) {
				if (entry.getValue() == null) {
					countOfTrgFilesWithoutCsvOrDataFiles++;
				}
				System.out.println("Trg file: " + entry.getKey() + ", Data file: " + entry.getValue());
			}

			int totalFiles = trgToDataFileMap.size();
			int processedFiles = totalFiles - countOfTrgFilesWithoutCsvOrDataFiles;

			logger.info(
					"Processing {} files whose corresponding csv and data files are present. Skipping {} files as corresponding CSV or data file was absent.",
					processedFiles, countOfTrgFilesWithoutCsvOrDataFiles);

			// traversing again on the map and processing csv files
			for (Map.Entry<String, String> entry : trgToDataFileMap.entrySet()) {
				if (entry.getValue() != null) {
					String csvFileName = entry.getKey() + csvFileExtension;
					String dataFileName = entry.getValue();
					String csvFilePath = sourceFolder + File.separator + csvFileName;
					String dataFilePath = sourceFolder + File.separator + dataFileName;

					processCsvFile(csvFilePath, destinationFolder, dataFilePath);
				}
			}
		} else {
			throw new SourceFolderDoesNotExistException("Source folder does not exist or is empty.");
		}
	}

	private static void processCsvFile(String csvFilePath, String destinationFolder, String dataFilePath) {
		try (Scanner scanner = new Scanner(new File(csvFilePath))) {
			logger.info("Reading CSV file: {}", csvFilePath);

			if (scanner.hasNextLine()) {
				String header = scanner.nextLine();
				logger.info("CSV header: {}", header);
			}

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				logger.info("CSV line: {}", line);

				String[] parts = line.split(",");
				if (parts.length == 2) {
					String folder = parts[0].trim();
					String createRevisionStr = parts[1].trim();

					// Validating createRevision value
					boolean createRevision;
					if (!createRevisionStr.equalsIgnoreCase("true") && !createRevisionStr.equalsIgnoreCase("false")) {
						logger.error("Invalid createRevision value in CSV: {}", createRevisionStr);
						return;
					} else {
						createRevision = Boolean.parseBoolean(createRevisionStr);
					}

					// Validating if folder exists in the destination folder
					String destinationFolderPath = destinationFolder + File.separator + folder;
					File destinationFolderFile = new File(destinationFolderPath);
					if (!destinationFolderFile.exists()) {
						logger.error("Invalid folder or folder does not exist: {}", destinationFolderPath);
						return;
					}

					processDataFile(dataFilePath, destinationFolderPath, createRevision);
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("CSV file not found: {}", e.getMessage());
		}
	}

	private static void processDataFile(String dataFilePath, String destinationFolderPath, boolean createRevision) {
		File sourceFile = new File(dataFilePath);

		if (!sourceFile.exists()) {
			logger.error("Source file not found: {}", dataFilePath);
			return;
		}

		String baseName = sourceFile.getName();
		String extension = "";

		int lastDotIndex = baseName.lastIndexOf('.');
		if (lastDotIndex != -1) {
			baseName = baseName.substring(0, lastDotIndex);
			extension = sourceFile.getName().substring(lastDotIndex);
		}

		int revision = 1;
		File destinationFolder = new File(destinationFolderPath);

		// Create revision logic
		if (createRevision) {
			while (true) {
				String revisedName = "";
				if (revision == 1) {
					revisedName = baseName;
				} else {
					revisedName = baseName + "(" + revision + ")";
				}
				File destinationFile = new File(destinationFolder, revisedName + extension);

				if (!destinationFile.exists()) {
					baseName = revisedName;
					break;
				}

				revision++;
			}
		} else {
			File destinationFile = new File(destinationFolder, baseName + extension);

			if (destinationFile.exists()) {
				logger.info("File already exists in destination folder: {}", destinationFile.getName());
				return;
			}
		}

		// Construct the destination file path
		File destinationFile = new File(destinationFolder, baseName + extension);

		try (InputStream inputStream = new FileInputStream(sourceFile);
				OutputStream outputStream = new FileOutputStream(destinationFile)) {

			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			logger.info("Copied and processed data file: {}", destinationFile.getName());
		} catch (IOException e) {
			logger.error("Error processing data file: {}", e.getMessage());
			writeErrorToFile(e.getMessage(), destinationFolderPath, sourceFile.getName());
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
