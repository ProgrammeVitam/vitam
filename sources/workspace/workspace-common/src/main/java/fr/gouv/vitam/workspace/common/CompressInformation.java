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
}
