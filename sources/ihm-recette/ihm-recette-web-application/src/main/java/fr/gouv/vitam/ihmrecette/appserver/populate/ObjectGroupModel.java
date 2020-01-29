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
package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;

public class ObjectGroupModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("FileInfo")
    private FileInfoModel fileInfoModel;

    @JsonProperty("_qualifiers")
    private List<ObjectGroupQualifiersModel> qualifiers;

    @JsonProperty("_up")
    private Set<String> up = new HashSet<>();

    @JsonProperty("_sp")
    private String sp;

    @JsonProperty("_sps")
    private Set<String> sps = new HashSet<>();

    @JsonProperty("_ops")
    private List<String> operationIds = new ArrayList<>();

    @JsonProperty("_opi")
    private String operationOriginId;

    @JsonProperty("_glpd")
    private String graphLastPersistedDate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

    @JsonProperty("_storage")
    private StorageModel storageModel;

    @JsonProperty("_tenant")
    private int tenant;

    @JsonProperty("_v")
    private int version = 0;

    /*
     * getter and setter
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileInfoModel getFileInfoModel() {
        return fileInfoModel;
    }

    public void setFileInfoModel(FileInfoModel fileInfoModel) {
        this.fileInfoModel = fileInfoModel;
    }

    public List<ObjectGroupQualifiersModel> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<ObjectGroupQualifiersModel> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public Set<String> getUp() {
        return up;
    }

    public void setUp(Set<String> up) {
        this.up = up;
    }

    public String getSp() {
        return sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public Set<String> getSps() {
        return sps;
    }

    public void setSps(Set<String> sps) {
        this.sps = sps;
    }

    public int getTenant() {
        return tenant;
    }

    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    public List<String> getOperationIds() {
        return operationIds;
    }

    public void setOperationIds(List<String> operationIds) {
        this.operationIds = operationIds;
    }

    public String getOperationOriginId() {
        return operationOriginId;
    }

    public void setOperationOriginId(String operationOriginId) {
        this.operationOriginId = operationOriginId;
    }

    public String getGraphLastPersistedDate() {
        return graphLastPersistedDate;
    }

    public void setGraphLastPersistedDate(String graphLastPersistedDate) {
        this.graphLastPersistedDate = graphLastPersistedDate;
    }

    public StorageModel getStorageModel() {
        return storageModel;
    }

    public void setStorageModel(StorageModel storageModel) {
        this.storageModel = storageModel;
    }

    public int getVersion() {
        return version;
    }

    public ObjectGroupModel setVersion(int version) {
        this.version = version;
        return this;
    }
}
