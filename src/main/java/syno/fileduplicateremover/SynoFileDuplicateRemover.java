package syno.fileduplicateremover;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SynoFileDuplicateRemover {

    public static void main(String[] args) {
        AppCmdConfig appCmdConfig;

        if (new File("app.properties").exists()) {
            appCmdConfig = AppCmdConfig.fromPropertiesFile("app.properties");
        } else {
            appCmdConfig = AppCmdConfig.fromArgs(args);;
        }

        System.out.println("Parse csv file: " + new File(appCmdConfig.getFile()).getAbsolutePath());

        Metrics metrics = new Metrics();

        Map<Integer, List<DuplicateFile>> csvFileContent = parseFile(appCmdConfig.getFile(), appCmdConfig.getEncoding(), metrics);
        List<DuplicateFile> filesToDelete = createDeleteFileList(csvFileContent, appCmdConfig);
        updateMetrics(filesToDelete, metrics);
        saveListToFile(filesToDelete, appCmdConfig.getEncoding());

        if (appCmdConfig.getSshHost() != null) {
            new FileTerminatorSsh(appCmdConfig, metrics).killThemAll(filesToDelete);
        } else {
            new FileTerminatorBash(appCmdConfig).killThemAll(filesToDelete);
        }

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

    static List<DuplicateFile> createDeleteFileList(Map<Integer, List<DuplicateFile>> csvFileContent, AppCmdConfig appCmdConfig) {
        final List<DuplicateFile> filesToDelete = new ArrayList<>();
        final List<String> dirsDeleteFrom = new ArrayList<>();
        final List<String> dirsDoNotDeleteFrom = new ArrayList<>();
        
        if (appCmdConfig.getDirsDeleteFrom() != null) {
            dirsDeleteFrom.addAll(Arrays.stream(appCmdConfig.getDirsDeleteFrom().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(string -> !string.isBlank())
                    .toList());
        }
        
        if (appCmdConfig.getDirsDoNotDeleteFrom() != null) {
            dirsDoNotDeleteFrom.addAll(Arrays.stream(appCmdConfig.getDirsDoNotDeleteFrom().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(string -> !string.isBlank())
                    .toList());
        }

        csvFileContent.values().forEach(fileDuplicates -> {
            if (fileDuplicates.size() == 1) {
                System.out.println("No duplicates found for group: " + fileDuplicates.get(0).getGroup());
            } else {
                Optional<DuplicateFile> preferredNotToBeDeleted = fileDuplicates.stream()
                        .filter(fileDuplicate -> dirsDoNotDeleteFrom.stream().filter(dirDoNotDeleteFrom -> fileDuplicate.getFileLocation().toLowerCase().contains(dirDoNotDeleteFrom)).findAny().isPresent())
                                .findAny();
                List<DuplicateFile> preferredToBeDeleted = fileDuplicates.stream()
                        .filter(fileDuplicate -> dirsDeleteFrom.stream().filter(dirDeleteFrom -> fileDuplicate.getFileLocation().toLowerCase().contains(dirDeleteFrom)).findAny().isPresent())
                        .toList();

                final List<DuplicateFile> fileCopy = new ArrayList<>(fileDuplicates);

                if (preferredNotToBeDeleted.isPresent()) {
                    fileCopy.remove(preferredNotToBeDeleted.get());
                } else if (preferredToBeDeleted.isEmpty() || preferredToBeDeleted.size() == fileCopy.size()) {
                    fileCopy.remove(0);
                } else {
                    fileCopy.remove(fileCopy.stream()
                            .filter(file -> !preferredToBeDeleted.contains(file))
                            .findAny().get());
                }

                filesToDelete.addAll(fileCopy);
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

    private static void skipHeader(BufferedReader fileReader) throws IOException {
        fileReader.readLine();
    }

    private static boolean isEmpty(String line) {
        return line.replace("\0", "").isEmpty();
    }
}
