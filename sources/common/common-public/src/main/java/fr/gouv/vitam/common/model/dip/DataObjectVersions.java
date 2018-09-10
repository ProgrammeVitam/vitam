package fr.gouv.vitam.common.model.dip;

import java.util.Collections;
import java.util.Set;

public class DataObjectVersions {
    private Set<String> dataObjectVersions;

    public DataObjectVersions() {}

    public DataObjectVersions(Set<String> dataObjectVersionToExport) {
        this.dataObjectVersions = dataObjectVersionToExport;
    }

    public Set<String> getDataObjectVersions() {
        if(dataObjectVersions == null) {
            dataObjectVersions = Collections.emptySet();
        }
        return dataObjectVersions;
    }

    public void setDataObjectVersions(Set<String> dataObjectVersions) {
        this.dataObjectVersions = dataObjectVersions;
    }
}
