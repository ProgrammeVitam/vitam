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
package fr.gouv.vitam.common.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.configuration.ClassificationLevel;

import java.util.List;
import java.util.Map;

public class PublicConfiguration {

    @JsonProperty("tenants")
    private List<Integer> tenants;

    @JsonProperty("adminTenant")
    private int adminTenant;

    @JsonProperty("indexInheritedRulesWithAPIV2OutputByTenant")
    private List<Integer> indexInheritedRulesWithAPIV2OutputByTenant;

    @JsonProperty("indexInheritedRulesWithRulesIdByTenant")
    private List<Integer> indexInheritedRulesWithRulesIdByTenant;

    @JsonProperty("externalReferentialIdentifiersByTenant")
    private Map<Integer, List<String>> externalReferentialIdentifiersByTenant;

    @JsonProperty("distributionThreshold")
    private long distributionThreshold;

    @JsonProperty("eliminationAnalysisThreshold")
    private long eliminationAnalysisThreshold;

    @JsonProperty("eliminationActionThreshold")
    private long eliminationActionThreshold;

    @JsonProperty("computedInheritedRulesThreshold")
    private long computedInheritedRulesThreshold;

    @JsonProperty("classificationLevel")
    private ClassificationLevel classificationLevel;

    @JsonProperty("virtualPathsConfigurationByTenant")
    private Map<Integer, List<String>> virtualPathsConfigurationByTenant;

    public PublicConfiguration() {
        // Empty constructor for serialization
    }

    public List<Integer> getTenants() {
        return tenants;
    }

    public PublicConfiguration setTenants(List<Integer> tenants) {
        this.tenants = tenants;
        return this;
    }

    public int getAdminTenant() {
        return adminTenant;
    }

    public PublicConfiguration setAdminTenant(int adminTenant) {
        this.adminTenant = adminTenant;
        return this;
    }

    public List<Integer> getIndexInheritedRulesWithAPIV2OutputByTenant() {
        return indexInheritedRulesWithAPIV2OutputByTenant;
    }

    public PublicConfiguration setIndexInheritedRulesWithAPIV2OutputByTenant(
        List<Integer> indexInheritedRulesWithAPIV2OutputByTenant
    ) {
        this.indexInheritedRulesWithAPIV2OutputByTenant = indexInheritedRulesWithAPIV2OutputByTenant;
        return this;
    }

    public List<Integer> getIndexInheritedRulesWithRulesIdByTenant() {
        return indexInheritedRulesWithRulesIdByTenant;
    }

    public PublicConfiguration setIndexInheritedRulesWithRulesIdByTenant(
        List<Integer> indexInheritedRulesWithRulesIdByTenant
    ) {
        this.indexInheritedRulesWithRulesIdByTenant = indexInheritedRulesWithRulesIdByTenant;
        return this;
    }

    public Map<Integer, List<String>> getExternalReferentialIdentifiersByTenant() {
        return externalReferentialIdentifiersByTenant;
    }

    public PublicConfiguration setExternalReferentialIdentifiersByTenant(
        Map<Integer, List<String>> externalReferentialIdentifiersByTenant
    ) {
        this.externalReferentialIdentifiersByTenant = externalReferentialIdentifiersByTenant;
        return this;
    }

    public long getDistributionThreshold() {
        return distributionThreshold;
    }

    public PublicConfiguration setDistributionThreshold(long distributionThreshold) {
        this.distributionThreshold = distributionThreshold;
        return this;
    }

    public long getEliminationAnalysisThreshold() {
        return eliminationAnalysisThreshold;
    }

    public PublicConfiguration setEliminationAnalysisThreshold(long eliminationAnalysisThreshold) {
        this.eliminationAnalysisThreshold = eliminationAnalysisThreshold;
        return this;
    }

    public long getEliminationActionThreshold() {
        return eliminationActionThreshold;
    }

    public PublicConfiguration setEliminationActionThreshold(long eliminationActionThreshold) {
        this.eliminationActionThreshold = eliminationActionThreshold;
        return this;
    }

    public long getComputedInheritedRulesThreshold() {
        return computedInheritedRulesThreshold;
    }

    public PublicConfiguration setComputedInheritedRulesThreshold(long computedInheritedRulesThreshold) {
        this.computedInheritedRulesThreshold = computedInheritedRulesThreshold;
        return this;
    }

    public ClassificationLevel getClassificationLevel() {
        return classificationLevel;
    }

    public PublicConfiguration setClassificationLevel(ClassificationLevel classificationLevel) {
        this.classificationLevel = classificationLevel;
        return this;
    }

    public Map<Integer, List<String>> getVirtualPathsConfigurationByTenant() {
        return virtualPathsConfigurationByTenant;
    }

    public PublicConfiguration setVirtualPathsConfigurationByTenant(
        Map<Integer, List<String>> virtualPathsConfigurationByTenant
    ) {
        this.virtualPathsConfigurationByTenant = virtualPathsConfigurationByTenant;
        return this;
    }
}
