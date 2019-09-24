package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CompressInformation POJO containing information on files to be compressed
 */
public class CompressInformation {

    @JsonProperty("files")
    private List<String> files = new ArrayList<>();
    @JsonProperty("outputFile")
    private String outputFile;
    @JsonProperty("outputContainer")
    private String outputContainer;

    /**
     * Default constructor
     */
    public CompressInformation() {
        // empty
    }

    /**
     * Constructor
     * 
     * @param files list of files to be compressed
     * @param outputFile output file
     */
    public CompressInformation(List<String> files, String outputFile, String outputContainer) {
        this.files = files;
        this.outputFile = outputFile;
        this.outputContainer = outputContainer;
    }

    /**
     * get list of files to be compressed
     * 
     * @return list of files as a list of string
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * Set list of files
     * 
     * @param files list of files as string
     */
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * Get Output File
     * 
     * @return outpuFile
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Set Output file
     * 
     * @param outputFile
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputContainer() {
        return outputContainer;
    }

    public CompressInformation setOutputContainer(String outputContainer) {
        this.outputContainer = outputContainer;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CompressInformation that = (CompressInformation) o;
        return Objects.equals(files, that.files) &&
            Objects.equals(outputFile, that.outputFile) &&
            Objects.equals(outputContainer, that.outputContainer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, outputFile, outputContainer);
    }
}
