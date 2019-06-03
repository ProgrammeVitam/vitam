package fr.gouv.vitam.common.model.tape;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FileInTape {

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileSegments")
    private List<TarEntryDescription> fileSegments;

    public FileInTape() {
        // Empty constructor for deserialization
    }

    public FileInTape(String fileName, List<TarEntryDescription> fileSegments) {
        this.fileName = fileName;
        this.fileSegments = fileSegments;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<TarEntryDescription> getFileSegments() {
        return fileSegments;
    }

    public void setFileSegments(List<TarEntryDescription> fileSegments) {
        this.fileSegments = fileSegments;
    }
}
