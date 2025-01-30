package syno.fileduplicateremover;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.concurrent.TimeUnit;

public class Metrics {
    int totalFiles;
    int removedFiles;
    long removedFilesBytes;
    long startTimeMillis = System.currentTimeMillis();
    int groupCount;

    void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    void setRemovedFiles(int removedFiles) {
        this.removedFiles = removedFiles;
    }

    void decreaseRemovedFiles() {
        removedFiles--;
    }

    void setRemovedFilesBytes(long removedFilesBytes) {
        this.removedFilesBytes = removedFilesBytes;
    }

    void decreaseRemovedFilesBytes(long notRemovedFilesBytes) {
        removedFilesBytes -= notRemovedFilesBytes;
    }

    void incrementTotalFiles() {
        totalFiles++;
    }

    void printSummary() {
        System.out.println("\nSummary:");
        System.out.println("\tVisited files: " + totalFiles);
        System.out.println("\tGroup count:  " + groupCount);
        System.out.println("\tRemoved files: " + removedFiles + " freed up space: " + humanReadableByteCountBin(removedFilesBytes));
        System.out.println("\tFiles remaining: " + (totalFiles - removedFiles));
        System.out.println("\tTime taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimeMillis) + " s");
    }

    private String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
}
