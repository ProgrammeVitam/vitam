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

package fr.gouv.vitam.metadata.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;

public class GroupedTenantConfiguration {

    @JsonProperty("tenants")
    private String tenants;
    @JsonProperty("name")
    private String name;
    @JsonProperty("unit")
    private CollectionConfiguration unit;
    @JsonProperty("objectgroup")
    private CollectionConfiguration objectgroup;

    public GroupedTenantConfiguration() {
        // Default constructor for deserialization
    }

    public GroupedTenantConfiguration(String tenants, String name,
        CollectionConfiguration unit, CollectionConfiguration objectgroup) {
        this.tenants = tenants;
        this.name = name;
        this.unit = unit;
        this.objectgroup = objectgroup;
    }

    public String getTenants() {
        return tenants;
    }

    public GroupedTenantConfiguration setTenants(String tenants) {
        this.tenants = tenants;
        return this;
    }

    public String getName() {
        return name;
    }

    public GroupedTenantConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public CollectionConfiguration getUnit() {
        return unit;
    }

    public GroupedTenantConfiguration setUnit(CollectionConfiguration unit) {
        this.unit = unit;
        return this;
    }

    public CollectionConfiguration getObjectgroup() {
        return objectgroup;
    }

    public GroupedTenantConfiguration setObjectgroup(
        CollectionConfiguration objectgroup) {
        this.objectgroup = objectgroup;
        return this;
    }

    @Override
    public String toString() {
        return "GroupedTenantConfiguration{" +
            "tenants='" + tenants + '\'' +
            ", name='" + name + '\'' +
            ", unit=" + unit +
            ", objectgroup=" + objectgroup +
            '}';
    }
}
