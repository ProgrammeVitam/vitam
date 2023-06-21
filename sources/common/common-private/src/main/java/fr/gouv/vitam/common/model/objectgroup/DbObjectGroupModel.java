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
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * ObjectGroupInternalModel
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DbObjectGroupModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("_tenant")
    private int tenant;

    @JsonProperty("FileInfo")
    private DbFileInfoModel fileInfo;

    @JsonProperty("_qualifiers")
    private List<DbQualifiersModel> qualifiers;

    @JsonProperty("_up")
    private List<String> up;

    @JsonProperty("_nbc")
    private int nbc;

    @JsonProperty("_ops")
    private List<String> ops;

    @JsonProperty("_opi")
    private String opi;

    @JsonProperty("_sp")
    private String originatingAgency;

    @JsonProperty("_sps")
    private List<String> originatingAgencies;

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return tenant
     */
    public int getTenant() {
        return tenant;
    }

    /**
     * @param tenant
     */
    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    /**
     * @return up
     */
    public List<String> getUp() {
        return up;
    }

    /**
     * @param up
     */
    public void setUp(List<String> up) {
        if (this.up == null) {
            this.up = new ArrayList<>();
        }
        this.up = up;
    }

    /**
     * @return nbc
     */
    public int getNbc() {
        return nbc;
    }

    /**
     * @param nbc
     */
    public void setNbc(int nbc) {
        this.nbc = nbc;
    }

    /**
     * @return ops
     */
    public List<String> getOps() {
        return ops;
    }

    /**
     * @param ops
     */
    public void setOps(List<String> ops) {
        if (this.ops == null) {
            this.ops = new ArrayList<>();
        }
        this.ops = ops;
    }

    /**
     * @return opi
     */
    public String getOpi() {
        return opi;
    }

    /**
     * @param opi
     */
    public void setOpi(String opi) {
        this.opi = opi;
    }

    /**
     * @return originatingAgency
     */
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    /**
     * @param originatingAgency
     */
    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public List<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    public void setOriginatingAgencies(List<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
    }

    /**
     * @return fileInfo
     */
    public DbFileInfoModel getFileInfo() {
        return fileInfo;
    }

    /**
     * @param fileInfo
     */
    public void setFileInfo(DbFileInfoModel fileInfo) {
        this.fileInfo = fileInfo;
    }

    public List<DbQualifiersModel> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<DbQualifiersModel> qualifiers) {
        this.qualifiers = qualifiers;
    }
}
