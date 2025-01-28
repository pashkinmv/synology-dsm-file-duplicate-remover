package syno.fileduplicateremover;

class DuplicateFile {
    private int group;
    private String fileLocation;
    private long fileSizeBytes;

    void setGroup(int group) {
        this.group = group;
    }

    int getGroup() {
        return group;
    }

    void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    String getFileLocation() {
        return fileLocation;
    }

    void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    long getFileSizeBytes() {
        return fileSizeBytes;
    }
}
