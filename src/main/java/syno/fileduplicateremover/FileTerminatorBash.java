package syno.fileduplicateremover;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
public class FileTerminatorBash implements FileTerminator {
    private final AppCmdConfig appCmdConfig;

    @Override
    public void killThemAll(List<DuplicateFile> filesToDelete) {
        if (appCmdConfig.isDryRun()) {
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
}
