/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessionRegisterSymbolicModel {

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @JsonProperty("OriginatingAgency")
    private String originatingAgency;

    @JsonProperty("CreationDate")
    private String creationDate;

    @JsonProperty("BinaryObjectSize")
    private double binaryObjectSize;

    @JsonProperty("ArchiveUnit")
    private long archiveUnit;

    @JsonProperty("ObjectGroup")
    private long objectGroup;

    @JsonProperty("BinaryObject")
    private long binaryObject;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public double getBinaryObjectSize() {
        return binaryObjectSize;
    }

    public void setBinaryObjectSize(double binaryObjectSize) {
        this.binaryObjectSize = binaryObjectSize;
    }

    public long getArchiveUnit() {
        return archiveUnit;
    }

    public void setArchiveUnit(long archiveUnit) {
        this.archiveUnit = archiveUnit;
    }

    public long getObjectGroup() {
        return objectGroup;
    }

    public void setObjectGroup(long objectGroup) {
        this.objectGroup = objectGroup;
    }

    public long getBinaryObject() {
        return binaryObject;
    }

    public void setBinaryObject(long binaryObject) {
        this.binaryObject = binaryObject;
    }
}
