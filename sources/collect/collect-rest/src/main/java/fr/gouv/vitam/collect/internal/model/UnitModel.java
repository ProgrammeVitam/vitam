/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.collect.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnitModel {

    @JsonProperty("Title")
    private String title;

    @JsonProperty("data")
    private String data;

    @JsonProperty("got")
    private String got;

    @JsonProperty("#id")
    private String id;

    @JsonProperty("#tenant")
    private String tenant;

    @JsonProperty("#unitups")
    private List<String> unitUps;

    @JsonProperty("#min")
    private Integer min;

    @JsonProperty("#max")
    private Integer max;

    @JsonProperty("#allunitups")
    private List<String> allUnitUps;

    @JsonProperty("#originating_agencies")
    private List<String> originatingAgencies;

    @JsonProperty("#version")
    private Integer version;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getGot() {
        return got;
    }

    public void setGot(String got) {
        this.got = got;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public List<String> getUnitUps() {
        return unitUps;
    }

    public void setUnitUps(List<String> unitUps) {
        this.unitUps = unitUps;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public List<String> getAllUnitUps() {
        return allUnitUps;
    }

    public void setAllUnitUps(List<String> allUnitUps) {
        this.allUnitUps = allUnitUps;
    }

    public List<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    public void setOriginatingAgencies(List<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UnitModel that = (UnitModel) o;
        return Objects.equals(got, that.got) && Objects.equals(id, that.id) &&
            Objects.equals(tenant, that.tenant) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(got, id, tenant, version);
    }

    @Override
    public String toString() {
        return "UnitModel{" +
            "title='" + title + '\'' +
            ", data='" + data + '\'' +
            ", got='" + got + '\'' +
            ", id='" + id + '\'' +
            ", tenant='" + tenant + '\'' +
            ", unitUps=" + unitUps +
            ", min=" + min +
            ", max=" + max +
            ", allUnitUps=" + allUnitUps +
            ", originatingAgencies=" + originatingAgencies +
            ", version=" + version +
            '}';
    }
}
