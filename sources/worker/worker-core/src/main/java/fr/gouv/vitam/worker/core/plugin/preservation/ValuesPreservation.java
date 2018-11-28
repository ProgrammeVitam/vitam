package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValuesPreservation {
    @JsonProperty("extension")
    private String extension;
    @JsonProperty("args")
    private List<String> args;
    @JsonProperty("dataToExtract")
    private Map<String, String> dataToExtract;

    public ValuesPreservation() {
    }

    public ValuesPreservation(String extension, List<String> args) {
        this.extension = extension;
        this.args = args;
    }

    public ValuesPreservation(Map<String, String> dataToExtract) {
        this.dataToExtract = dataToExtract;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public List<String> getArgs() {
        if (args == null) {
            return Collections.emptyList();
        }
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Map<String, String> getDataToExtract() {
        return dataToExtract;
    }

    public void setDataToExtract(Map<String, String> dataToExtract) {
        this.dataToExtract = dataToExtract;
    }

    @Override
    public String toString() {
        return "Values{" +
            "extension='" + extension + '\'' +
            ", args=" + args +
            ", dataToExtract=" + dataToExtract +
            '}';
    }
}
