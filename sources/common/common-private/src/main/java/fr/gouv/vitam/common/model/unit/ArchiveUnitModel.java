/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.ArrayList;
import java.util.List;

/**
 * ArchiveUnitModel class
 */
public class ArchiveUnitModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("_og")
    private String og;

    @JsonProperty("_opts")
    private List<String> opts;

    private String archiveUnitProfile;

    private ManagementModel management;


    @JsonUnwrapped
    @JsonProperty("Content")
    private DescriptiveMetadataModel descriptiveMetadataModel;

    private DataObjectReference dataObjectReference;

    @JsonProperty("_history")
    private List<ArchiveUnitHistoryModel> history = new ArrayList<>();

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

    public DataObjectReference getDataObjectReference() {
        return dataObjectReference;
    }

    public void setDataObjectReference(DataObjectReference dataObjectReference) {
        this.dataObjectReference = dataObjectReference;
    }

    public String getOg() {
        return og;
    }

    public void setOg(String og) {
        this.og = og;
    }

    @JsonGetter("#management")
    public ManagementModel getManagement() {
        return management;
    }

    @JsonSetter("#management")
    public void setManagement(ManagementModel management) {
        this.management = management;
    }


    @JsonSetter("#id")
    public void set_Id(String id) {
        this.id = id;
    }

    @JsonSetter("#object")
    public void set_Og(String og) {
        this.og = og;
    }

    @JsonSetter("_mgt")
    public void setMgt(ManagementModel management) {
        this.management = management;
    }

    @JsonSetter("Management")
    public void setMgtManagement(ManagementModel management) {
        this.management = management;
    }

    @JsonGetter("Management")
    public ManagementModel getMgtManagement() {
        return management;
    }

    @JsonGetter("_mgt")
    public ManagementModel getMgt(ManagementModel management) {
        return this.management;
    }

    @JsonGetter("#history")
    public List<ArchiveUnitHistoryModel> getHistory() {
        return history;
    }

    @JsonSetter("#history")
    public void setHistory(List<ArchiveUnitHistoryModel> history) {
        this.history = history;
    }

    @JsonGetter("#opts")
    public List<String> getOpts() {
        if (this.opts == null) {
            return new ArrayList<>();
        }
        return this.opts;
    }

    @JsonSetter("#opts")
    public void setOpts(List<String> opts) {
        this.opts = opts;
    }
}
