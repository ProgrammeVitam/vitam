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
package fr.gouv.vitam.collect.internal.core.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;

import java.util.List;
import java.util.Objects;

/**
 * project model
 */
public class ProjectModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Context")
    private ManifestContext manifestContext;

    @JsonProperty("Status")
    private ProjectStatus status;

    @JsonProperty("CreationDate")
    private String creationDate;

    @JsonProperty("LastUpdate")
    private String lastUpdate;

    @JsonProperty("UnitUp")
    private String unitUp;

    @JsonProperty("UnitUps")
    private List<MetadataUnitUp> unitUps;

    @JsonProperty("_tenant")
    private Integer tenant;

    public ProjectModel() {}

    public ProjectModel(
        String id,
        String name,
        ManifestContext manifestContext,
        ProjectStatus status,
        String creationDate,
        String lastUpdate,
        String unitUp,
        List<MetadataUnitUp> unitUps,
        Integer tenant
    ) {
        this.id = id;
        this.name = name;
        this.manifestContext = manifestContext;
        this.status = status;
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.unitUps = unitUps;
        this.unitUp = unitUp;
        this.tenant = tenant;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ManifestContext getManifestContext() {
        return manifestContext;
    }

    public void setManifestContext(ManifestContext manifestContext) {
        this.manifestContext = manifestContext;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUnitUp() {
        return unitUp;
    }

    public void setUnitUp(String unitUp) {
        this.unitUp = unitUp;
    }

    public List<MetadataUnitUp> getUnitUps() {
        return unitUps;
    }

    public void setUnitUps(List<MetadataUnitUp> unitUps) {
        this.unitUps = unitUps;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectModel that = (ProjectModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
