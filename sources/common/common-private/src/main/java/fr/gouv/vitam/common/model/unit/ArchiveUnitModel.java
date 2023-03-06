/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.ArrayList;
import java.util.List;

/**
 * ArchiveUnit external model (#id, #management...)
 */
public class ArchiveUnitModel {

    @JsonProperty("#id")
    private String id;

    @JsonProperty("#object")
    private String og;

    @JsonProperty("ArchiveUnitProfile")
    private String archiveUnitProfile;

    @JsonProperty("#management")
    private ManagementModel management;

    @JsonUnwrapped
    private DescriptiveMetadataModel descriptiveMetadataModel;

    @JsonProperty("#history")
    private List<ArchiveUnitHistoryModel> history = new ArrayList<>();

    @JsonProperty("#sedaVersion")
    private String sedaVersion;

    @JsonProperty("#implementationVersion")
    private String implementationVersion;

    @JsonProperty("#unitType")
    private String unitType;

    @JsonProperty("#opi")
    private String opi;

    @JsonProperty("#operations")
    private List<String> ops;

    @JsonProperty("#opts")
    private List<String> opts;

    @JsonProperty("#originating_agency")
    private String originatingAgency;

    @JsonProperty("#originating_agencies")
    private List<String> originatingAgencies;

    @JsonProperty("#unitups")
    private List<String> unitups;

    @JsonProperty("#allunitups")
    private List<String> allunitups;

    /**
     * Constructor
     */
    public ArchiveUnitModel() {
        management = new ManagementModel();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArchiveUnitProfile() {
        return archiveUnitProfile;
    }

    public void setArchiveUnitProfile(String archiveUnitProfile) {
        this.archiveUnitProfile = archiveUnitProfile;
    }

    public DescriptiveMetadataModel getDescriptiveMetadataModel() {
        return descriptiveMetadataModel;
    }

    public void setDescriptiveMetadataModel(DescriptiveMetadataModel descriptiveMetadataModel) {
        this.descriptiveMetadataModel = descriptiveMetadataModel;
    }

    public String getOg() {
        return og;
    }

    public void setOg(String og) {
        this.og = og;
    }

    public ManagementModel getManagement() {
        return management;
    }

    public void setManagement(ManagementModel management) {
        this.management = management;
    }


    public List<ArchiveUnitHistoryModel> getHistory() {
        return history;
    }

    public void setHistory(List<ArchiveUnitHistoryModel> history) {
        this.history = history;
    }

    public String getSedaVersion() {
        return sedaVersion;
    }

    public void setSedaVersion(String sedaVersion) {
        this.sedaVersion = sedaVersion;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public void setImplementationVersion(String implementationVersion) {
        this.implementationVersion = implementationVersion;
    }

    public String getUnitType() {
        return unitType;
    }

    public ArchiveUnitModel setUnitType(String unitType) {
        this.unitType = unitType;
        return this;
    }

    public String getOpi() {
        return opi;
    }

    public ArchiveUnitModel setOpi(String opi) {
        this.opi = opi;
        return this;
    }

    public List<String> getOps() {
        return ops;
    }

    public ArchiveUnitModel setOps(List<String> ops) {
        this.ops = ops;
        return this;
    }



    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public ArchiveUnitModel setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
        return this;
    }

    public List<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    public ArchiveUnitModel setOriginatingAgencies(List<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
        return this;
    }

    public List<String> getUnitups() {
        return unitups;
    }

    public void setUnitups(List<String> unitups) {
        this.unitups = unitups;
    }

    public List<String> getAllunitups() {
        return allunitups;
    }

    public void setAllunitups(List<String> allunitups) {
        this.allunitups = allunitups;
    }

    public List<String> getOpts() {
        return this.opts;
    }

    public void setOpts(List<String> opts) {
        this.opts = opts;
    }
}
