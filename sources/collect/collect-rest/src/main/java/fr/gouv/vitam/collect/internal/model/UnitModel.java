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
import fr.gouv.vitam.common.model.unit.ManagementModel;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnitModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("DescriptionLevel")
    private String descriptionLevel;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("TransactedDate")
    private String transactedDate;

    @JsonProperty("_og")
    private String og;

    @JsonProperty("_mgt")
    private ManagementModel management;

    @JsonProperty("_sedaVersion")
    private String sedaVersion;

    @JsonProperty("_unitType")
    private String unitType;

    @JsonProperty("_opi")
    private String opi;

    @JsonProperty("_ops")
    private Set<String> ops;

    // Storage

    @JsonProperty("_sps")
    private Set<String> sps;

    @JsonProperty("_sp")
    private String sp;

    @JsonProperty("_up")
    private Set<String> up;

    @JsonProperty("_us")
    private Set<String> us;

    @JsonProperty("_graph")
    private Set<String> graph;

    // uds

    @JsonProperty("_max")
    private Integer max;

    @JsonProperty("_min")
    private Integer min;

    @JsonProperty("_glpd")
    private String glpd;

    @JsonProperty("_fuzzyCD")
    private String fuzzyCD;

    @JsonProperty("_fuzzyUD")
    private String fuzzyUD;

    @JsonProperty("_tenant")
    private String tenant;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescriptionLevel() {
        return descriptionLevel;
    }

    public void setDescriptionLevel(String descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactedDate() {
        return transactedDate;
    }

    public void setTransactedDate(String transactedDate) {
        this.transactedDate = transactedDate;
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

    public String getSedaVersion() {
        return sedaVersion;
    }

    public void setSedaVersion(String sedaVersion) {
        this.sedaVersion = sedaVersion;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    public Set<String> getOps() {
        return ops;
    }

    public void setOps(Set<String> ops) {
        this.ops = ops;
    }

    public Set<String> getSps() {
        return sps;
    }

    public void setSps(Set<String> sps) {
        this.sps = sps;
    }

    public String getSp() {
        return sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public Set<String> getUp() {
        return up;
    }

    public void setUp(Set<String> up) {
        this.up = up;
    }

    public Set<String> getUs() {
        return us;
    }

    public void setUs(Set<String> us) {
        this.us = us;
    }

    public Set<String> getGraph() {
        return graph;
    }

    public void setGraph(Set<String> graph) {
        this.graph = graph;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public String getGlpd() {
        return glpd;
    }

    public void setGlpd(String glpd) {
        this.glpd = glpd;
    }

    public String getFuzzyCD() {
        return fuzzyCD;
    }

    public void setFuzzyCD(String fuzzyCD) {
        this.fuzzyCD = fuzzyCD;
    }

    public String getFuzzyUD() {
        return fuzzyUD;
    }

    public void setFuzzyUD(String fuzzyUD) {
        this.fuzzyUD = fuzzyUD;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public String toString() {
        return "UnitModel{" +
            "id='" + id + '\'' +
            ", descriptionLevel='" + descriptionLevel + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", transactedDate='" + transactedDate + '\'' +
            ", og='" + og + '\'' +
            ", management=" + management +
            ", sedaVersion='" + sedaVersion + '\'' +
            ", unitType='" + unitType + '\'' +
            ", opi='" + opi + '\'' +
            ", ops=" + ops +
            ", sps=" + sps +
            ", sp='" + sp + '\'' +
            ", up=" + up +
            ", us=" + us +
            ", graph=" + graph +
            ", max=" + max +
            ", min=" + min +
            ", glpd='" + glpd + '\'' +
            ", fuzzyCD='" + fuzzyCD + '\'' +
            ", fuzzyUD='" + fuzzyUD + '\'' +
            ", tenant='" + tenant + '\'' +
            '}';
    }
}
