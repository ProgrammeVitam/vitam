package fr.gouv.vitam.workspace.common;

import java.util.ArrayList;
import java.util.List;

public class CompressInformation {

    private List<String> files = new ArrayList<>();

    private String outputFile;

    public CompressInformation() {
    }

    public CompressInformation(List<String> files, String outputFile) {
        this.files = files;
        this.outputFile = outputFile;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CompressInformation that = (CompressInformation) o;

        if (files != null ? !files.equals(that.files) : that.files != null)
            return false;
        return outputFile != null ? outputFile.equals(that.outputFile) : that.outputFile == null;
    }

    @Override public int hashCode() {
        int result = files != null ? files.hashCode() : 0;
        result = 31 * result + (outputFile != null ? outputFile.hashCode() : 0);
        return result;
    }

}
