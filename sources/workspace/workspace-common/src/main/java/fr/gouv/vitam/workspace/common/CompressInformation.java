package fr.gouv.vitam.workspace.common;

import java.util.ArrayList;
import java.util.List;

/**
 * CompressInformation POJO containing information on files to be compressed
 */
public class CompressInformation {

    private List<String> files = new ArrayList<>();

    private String outputFile;

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
    public CompressInformation(List<String> files, String outputFile) {
        this.files = files;
        this.outputFile = outputFile;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CompressInformation that = (CompressInformation) o;

        if (files != null ? !files.equals(that.files) : that.files != null)
            return false;
        return outputFile != null ? outputFile.equals(that.outputFile) : that.outputFile == null;
    }

    @Override
    public int hashCode() {
        int result = files != null ? files.hashCode() : 0;
        result = 31 * result + (outputFile != null ? outputFile.hashCode() : 0);
        return result;
    }

}
