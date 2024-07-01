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
import fr.gouv.vitam.common.model.objectgroup.StorageRacineModel;

import java.util.List;

/**
 * AuditObjectGroup
 */
public class AuditObjectGroup {

    @JsonProperty("id")
    private String id;

    @JsonProperty("opi")
    private String opi;

    @JsonProperty("sp")
    private String sp;

    @JsonProperty("unitUps")
    private List<String> unitUps;

    @JsonProperty("objects")
    private List<AuditObject> objects;

    @JsonProperty("storage")
    private StorageRacineModel storage;

    public AuditObjectGroup() {}

    public AuditObjectGroup(
        String id,
        String opi,
        String sp,
        List<String> unitUps,
        List<AuditObject> objects,
        StorageRacineModel storage
    ) {
        this.id = id;
        this.opi = opi;
        this.sp = sp;
        this.unitUps = unitUps;
        this.objects = objects;
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

    public String getSp() {
        return sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public List<String> getUnitUps() {
        return unitUps;
    }

    public void setUnitUps(List<String> unitUps) {
        this.unitUps = unitUps;
    }

    public List<AuditObject> getObjects() {
        return objects;
    }

    public void setObjects(List<AuditObject> objects) {
        this.objects = objects;
    }

    public StorageRacineModel getStorage() {
        return storage;
    }

    public void setStorage(StorageRacineModel storage) {
        this.storage = storage;
    }
}
