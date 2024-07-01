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

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Data Transfer Object Model of access contract (DTO).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessContractModel extends AbstractContractModel {

    /**
     * OriginatingAgencies
     */
    public static final String ORIGINATING_AGENCIES = "OriginatingAgencies";

    /**
     * Root units
     */
    public static final String ROOT_UNITS = "RootUnits";

    /**
     * Excluded root units
     */
    public static final String EXCLUDED_ROOT_UNITS = "ExcludedRootUnits";

    /**
     * DataObjectVersion
     */
    public static final String DATA_OBJECT_VERSION = "DataObjectVersion";

    /**
     * Work for all data object version
     */
    public static final String EVERY_DATA_OBJECT_VERSION = "EveryDataObjectVersion";

    /**
     * Work for all originating agencies
     */
    public static final String EVERY_ORIGINATINGAGENCY = "EveryOriginatingAgency";

    /**
     * Work for access log
     */
    private static final String ACCESS_LOG = "AccessLog";
    public static final String RULE_CATEGORY_TO_FILTER = "RuleCategoryToFilter";

    @JsonProperty(DATA_OBJECT_VERSION)
    private Set<String> dataObjectVersion;

    @JsonProperty(ORIGINATING_AGENCIES)
    private Set<String> originatingAgencies;

    @JsonProperty("WritingPermission")
    private Boolean writingPermission;

    @JsonProperty("WritingRestrictedDesc")
    private Boolean writingRestrictedDesc;

    @JsonProperty(EVERY_ORIGINATINGAGENCY)
    private Boolean everyOriginatingAgency;

    @JsonProperty(EVERY_DATA_OBJECT_VERSION)
    private Boolean everyDataObjectVersion;

    @JsonProperty(ROOT_UNITS)
    private Set<String> rootUnits;

    @JsonProperty(EXCLUDED_ROOT_UNITS)
    private Set<String> excludedRootUnits;

    @JsonProperty(ACCESS_LOG)
    private ActivationStatus accessLog;

    @JsonProperty(RULE_CATEGORY_TO_FILTER)
    private Set<RuleType> ruleCategoryToFilter;

    /**
     * Constructor without fields
     * use for jackson
     */
    public AccessContractModel() {
        super();
    }

    /**
     * Get the collection of originating agency
     *
     * @return originatingAgencies collection
     */
    public Set<String> getOriginatingAgencies() {
        if (originatingAgencies == null) {
            originatingAgencies = new HashSet<>();
        }
        return originatingAgencies;
    }

    /**
     * Set the collection of originating agency
     *
     * @param originatingAgencies
     */
    public AccessContractModel setOriginatingAgencies(Set<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
        return this;
    }

    /**
     * @return dataObjectVersion
     */
    public Set<String> getDataObjectVersion() {
        if (dataObjectVersion == null) {
            dataObjectVersion = new HashSet<>();
        }
        return dataObjectVersion;
    }

    /**
     * @param dataObjectVersion
     * @return AccessContractModel
     */
    public AccessContractModel setDataObjectVersion(Set<String> dataObjectVersion) {
        this.dataObjectVersion = dataObjectVersion;
        return this;
    }

    /**
     * @return writingPermission
     */
    public Boolean getWritingPermission() {
        return writingPermission;
    }

    /**
     * @param writingPermission
     * @return AccessContractModel
     */
    public AccessContractModel setWritingPermission(Boolean writingPermission) {
        this.writingPermission = writingPermission;
        return this;
    }

    /**
     * @return writingRestrictedDesc
     */
    public Boolean getWritingRestrictedDesc() {
        return writingRestrictedDesc;
    }

    /**
     * @param writingRestrictedDesc
     * @return AccessContractModel
     */
    AccessContractModel setWritingRestrictedDesc(Boolean writingRestrictedDesc) {
        this.writingRestrictedDesc = writingRestrictedDesc;
        return this;
    }

    /**
     * @return true if all originatingAgencies are enabled for this contract
     */
    public Boolean getEveryOriginatingAgency() {
        return everyOriginatingAgency;
    }

    /**
     * Set the 'everyOriginatingAgency' flag on the contract.
     *
     * @param everyOriginatingAgency If true, all originatingAgencies are enabled for this contract
     * @return the contract
     */
    public AccessContractModel setEveryOriginatingAgency(Boolean everyOriginatingAgency) {
        this.everyOriginatingAgency = everyOriginatingAgency;
        return this;
    }

    /**
     * @return true if all data object version are enabled for this contract
     */
    public Boolean isEveryDataObjectVersion() {
        return everyDataObjectVersion;
    }

    /**
     * Set the 'everyDataObjectVersion' flag on the contract.
     *
     * @param everyDataObjectVersion if true, all data object version are enabled for this contract
     * @return this
     */
    public AccessContractModel setEveryDataObjectVersion(Boolean everyDataObjectVersion) {
        this.everyDataObjectVersion = everyDataObjectVersion;
        return this;
    }

    /**
     * @return the root units
     */
    public Set<String> getRootUnits() {
        if (rootUnits == null) {
            rootUnits = new HashSet<>();
        }
        return rootUnits;
    }

    /**
     * Collection of GUID of archive units. If not empty, access is restricted only to the given rootUnits
     * and there childs. Access not permitted to parent units of the rootUnits
     * Access not permitted to parent units of the rootUnits
     *
     * @param rootUnits collection of guid of units (can be empty)
     * @return this
     */
    public AccessContractModel setRootUnits(Set<String> rootUnits) {
        this.rootUnits = rootUnits;
        return this;
    }

    /**
     * @return the excluded root units
     */
    public Set<String> getExcludedRootUnits() {
        if (excludedRootUnits == null) {
            excludedRootUnits = new HashSet<>();
        }
        return excludedRootUnits;
    }

    /**
     * Collection of archive units' GUIDs. If not empty then access is forbidden to given unit and its children.
     *
     * @param excludedRootUnits collection of guid of units (can be empty)
     * @return this
     */
    public AccessContractModel setExcludedRootUnits(Set<String> excludedRootUnits) {
        this.excludedRootUnits = excludedRootUnits;
        return this;
    }

    public ActivationStatus getAccessLog() {
        return accessLog;
    }

    public AccessContractModel setAccessLog(ActivationStatus accessLog) {
        this.accessLog = accessLog;
        return this;
    }

    public Set<RuleType> getRuleCategoryToFilter() {
        if (ruleCategoryToFilter == null) {
            ruleCategoryToFilter = new HashSet<>();
        }
        return ruleCategoryToFilter;
    }

    void setRuleCategoryToFilter(Set<RuleType> ruleCategoryToFilter) {
        this.ruleCategoryToFilter = ruleCategoryToFilter;
    }

    public void initializeDefaultValue() {
        writingPermission = firstNonNull(writingPermission, false);
        writingRestrictedDesc = firstNonNull(writingRestrictedDesc, false);
        everyOriginatingAgency = firstNonNull(everyOriginatingAgency, false);
        everyDataObjectVersion = firstNonNull(everyDataObjectVersion, false);
        accessLog = firstNonNull(accessLog, ActivationStatus.INACTIVE);
    }
}
