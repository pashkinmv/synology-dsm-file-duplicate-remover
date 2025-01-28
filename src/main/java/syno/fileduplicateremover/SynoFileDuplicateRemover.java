package syno.fileduplicateremover;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SynoFileDuplicateRemover {

    public static void main(String[] args) {
        AppCmdConfig appCmdConfig = AppCmdConfig.fromArgs(args);

        execute(appCmdConfig.getFile(), appCmdConfig.isDryRun(), appCmdConfig.getEncoding());
    }

    static void execute(String synoDuplicateListCsvFile, boolean dryRun, String encoding) {
        System.out.println("Parse csv file: " + new File(synoDuplicateListCsvFile).getAbsolutePath());

        Metrics metrics = new Metrics();

        Map<Integer, List<DuplicateFile>> csvFileContent = parseFile(synoDuplicateListCsvFile, encoding, metrics);
        List<DuplicateFile> filesToDelete = createDeleteFileList(csvFileContent, metrics);
        updateMetrics(filesToDelete, metrics);
        saveListToFile(filesToDelete, encoding);
        deleteFiles(dryRun,filesToDelete);

        metrics.printSummary();
    }

    static Map<Integer, List<DuplicateFile>> parseFile(String synoDuplicateListCsvFile, String encoding, Metrics metrics) {
        Map<Integer, List<DuplicateFile>> csvFileContent = new HashMap<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(new File(synoDuplicateListCsvFile).getAbsolutePath())), encoding))) {
            skipHeader(fileReader);

            String line;
            while ((line = fileReader.readLine()) != null) {
                if (isEmpty(line)) {
                    continue;
                }

                String[] fileLineTokens = line.split("\t"); // wrong format exit
                DuplicateFile duplicateFile = new DuplicateFile();
                duplicateFile.setGroup(Integer.parseInt(fileLineTokens[0].replaceAll("\"|\0", "")));
                duplicateFile.setFileLocation(fileLineTokens[2].replace("\"", "").replace("\0", ""));
                duplicateFile.setFileSizeBytes(Long.parseLong(fileLineTokens[3].replace("\"", "").replace("\0", "")));

                csvFileContent
                        .computeIfAbsent(duplicateFile.getGroup(), i -> new ArrayList<>())
                        .add(duplicateFile);

                metrics.incrementTotalFiles();
            }
        } catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }

        metrics.setGroupCount(csvFileContent.size());

        return csvFileContent;
    }

    static List<DuplicateFile> createDeleteFileList(Map<Integer, List<DuplicateFile>> csvFileContent, Metrics metrics) {
        List<DuplicateFile> filesToDelete = new ArrayList<>();

        csvFileContent.values().forEach(fileDuplicates -> {
            if (fileDuplicates.size() == 1) {
                System.out.println("No duplicates found for group: " + fileDuplicates.get(0).getGroup());
            } else {
                filesToDelete.addAll(fileDuplicates.subList(1, fileDuplicates.size()));
            }
        });

        return filesToDelete;
    }

    static void updateMetrics(List<DuplicateFile> filesToDelete, Metrics metrics) {
        metrics.setRemovedFilesBytes(filesToDelete.stream()
                .map(DuplicateFile::getFileSizeBytes)
                .reduce(0L, Long::sum));

        metrics.setRemovedFiles(filesToDelete.size());
    }

    static void saveListToFile(List<DuplicateFile> filesToDelete, String encoding) {
        try (BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("deleted-files.csv"), encoding))) {
            for (int i = 0; i < filesToDelete.size(); i++) {
                if (i > 0) {
                    fileWriter.newLine();
                }

                fileWriter.write(filesToDelete.get(i).getFileLocation());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFiles(boolean dryRun, List<DuplicateFile> filesToDelete) {
        if (dryRun) {
            System.out.println("Dry more is on. Files won't be deleted.");
            return;
        }

        for (DuplicateFile fileToDelete : filesToDelete) {
            try {
                Files.delete(Paths.get(fileToDelete.getFileLocation().replaceAll("\"|\0|\\s", "")));
                System.out.println("Deleted file: " + fileToDelete.getFileLocation());
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }


    private static void skipHeader(BufferedReader fileReader) throws IOException {
        fileReader.readLine();
    }

    private static boolean isEmpty(String line) {
        return line.replace("\0", "").isEmpty();
    }
}
