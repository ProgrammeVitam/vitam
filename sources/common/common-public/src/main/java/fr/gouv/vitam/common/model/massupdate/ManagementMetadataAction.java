package fr.gouv.vitam.common.model.massupdate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManagementMetadataAction {

    // Non-Rule Management Metadata
    @JsonProperty("ArchiveUnitProfile")
    String archiveUnitProfile;

    public String getArchiveUnitProfile() {
        return archiveUnitProfile;
    }

    public ManagementMetadataAction setArchiveUnitProfile(String archiveUnitProfile) {
        this.archiveUnitProfile = archiveUnitProfile;
        return this;
    }
}
