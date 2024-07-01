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
package fr.gouv.vitam.worker.core.plugin.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.objectgroup.StorageJson;

/**
 * AuditObject
 */
public class AuditObject {

    @JsonProperty("id")
    private String id;

    @JsonProperty("opi")
    private String opi;

    @JsonProperty("qualifier")
    private String qualifier;

    @JsonProperty("version")
    private String version;

    @JsonProperty("MessageDigest")
    private String messageDigest;

    @JsonProperty("Algorithm")
    private String algorithm;

    @JsonProperty("storage")
    private StorageJson storage;

    public AuditObject() {}

    public AuditObject(
        String id,
        String opi,
        String qualifier,
        String version,
        String messageDigest,
        String algorithm,
        StorageJson storage
    ) {
        super();
        this.id = id;
        this.opi = opi;
        this.qualifier = qualifier;
        this.version = version;
        this.messageDigest = messageDigest;
        this.algorithm = algorithm;
        this.storage = storage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessageDigest() {
        return messageDigest;
    }

    public void setMessageDigest(String messageDigest) {
        this.messageDigest = messageDigest;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public StorageJson getStorage() {
        return storage;
    }

    public void setStorage(StorageJson storage) {
        this.storage = storage;
    }
}
