package syno.fileduplicateremover;

import java.util.List;

public interface FileTerminator {
    void killThemAll(List<DuplicateFile> filesToDelete);
}
