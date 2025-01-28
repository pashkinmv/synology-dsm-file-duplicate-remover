package syno.fileduplicateremover;

import lombok.Data;

@Data
public class DuplicateFile {
    private int group;
    private String fileLocation;
    private long fileSizeBytes;
}
