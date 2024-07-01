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

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessionRegisterSummaryModel {

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    /**
     * originating agency (aggregation key for {@link AccessionRegisterDetailModel})
     */
    @JsonProperty("OriginatingAgency")
    private String originatingAgency;

    /**
     * number of objects containing all archives for a specific originating agency
     */
    @JsonProperty("TotalObjects")
    private RegisterValueDetailModel totalObjects;

    /**
     * number of objects groups containing all archives for a specific originating agency
     */
    @JsonProperty("TotalObjectGroups")
    private RegisterValueDetailModel totalObjectsGroups;

    /**
     * number of archive units containing all archives for a specific originating agency
     */

    @JsonProperty("TotalUnits")
    private RegisterValueDetailModel totalUnits;

    /**
     * number of objects size
     */
    @JsonProperty("ObjectSize")
    private RegisterValueDetailModel ObjectSize;

    /**
     * creation date
     */
    @JsonProperty("CreationDate")
    private String creationDate;

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * @param tenant the working tenant to set
     * @return this
     */
    public AccessionRegisterSummaryModel setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * @return version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @return originatingAgency
     */
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    /**
     * @param originatingAgency value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
        return this;
    }

    /**
     * @return totalObjects
     */
    public RegisterValueDetailModel getTotalObjects() {
        return totalObjects;
    }

    /**
     * @param totalObjects value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setTotalObjects(RegisterValueDetailModel totalObjects) {
        this.totalObjects = totalObjects;
        return this;
    }

    /**
     * @return totalObjectsGroups
     */
    public RegisterValueDetailModel getTotalObjectsGroups() {
        return totalObjectsGroups;
    }

    /**
     * @param totalObjectsGroups value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setTotalObjectsGroups(RegisterValueDetailModel totalObjectsGroups) {
        this.totalObjectsGroups = totalObjectsGroups;
        return this;
    }

    /**
     * @return totalUnits
     */
    public RegisterValueDetailModel getTotalUnits() {
        return totalUnits;
    }

    /**
     * @param totalUnits value to set
     * @return AccessionRegisterSummaryModel
     */
    public AccessionRegisterSummaryModel setTotalUnits(RegisterValueDetailModel totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    /**
     * @return ObjectSize
     */
    public RegisterValueDetailModel getObjectSize() {
        return ObjectSize;
    }

    /**
     * @param objectSize value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setObjectSize(RegisterValueDetailModel objectSize) {
        ObjectSize = objectSize;
        return this;
    }

    /**
     * @return creationDate
     */
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate value to set
     * @return this
     */
    public AccessionRegisterSummaryModel setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }
}
