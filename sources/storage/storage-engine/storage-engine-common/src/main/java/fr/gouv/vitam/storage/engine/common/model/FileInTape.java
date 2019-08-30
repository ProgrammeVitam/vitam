package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FileInTape {

    public static final String FILE_NAME = "fileName";
    public static final String STORAGE_ID = "storageId";
    public static final String FILE_SEGMENTS = "fileSegments";

    @JsonProperty(FILE_NAME)
    private String fileName;

    @JsonProperty(STORAGE_ID)
    private String storageId;


    @JsonProperty(FILE_SEGMENTS)
    private List<TarEntryDescription> fileSegments;

    public FileInTape() {
        // Empty constructor for deserialization
    }


    public FileInTape(String fileName, String storageId,
        List<TarEntryDescription> fileSegments) {
        this.fileName = fileName;
        this.storageId = storageId;
        this.fileSegments = fileSegments;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public List<TarEntryDescription> getFileSegments() {
        return fileSegments;
    }

    public void setFileSegments(List<TarEntryDescription> fileSegments) {
        this.fileSegments = fileSegments;
    }
}
