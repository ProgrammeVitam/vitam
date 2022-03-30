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
package fr.gouv.vitam.storage.offers.tape.dto;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;

public class TapeResponse {

    private Object entity;
    private StatusCode status;
    private ReadWriteErrorCode errorCode;

    public TapeResponse(StatusCode status) {
        this.status = status;
    }

    public TapeResponse(Object entity, StatusCode status) {
        this.entity = entity;
        this.status = status;
    }

    public TapeResponse(ReadWriteErrorCode errorCode, StatusCode status) {
        this.errorCode = errorCode;
        this.status = status;
    }

    public TapeResponse(Object entity, ReadWriteErrorCode errorCode, StatusCode status) {
        this.entity = entity;
        this.errorCode = errorCode;
        this.status = status;
    }

    public <T> T getEntity(Class<T> entityType) {
        if (!(entity instanceof String) && entityType.isAssignableFrom(String.class)) {
            return entityType.cast(JsonHandler.unprettyPrint(entity));
        }
        return entityType.cast(entity);
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public boolean hasEntity() {
        return entity != null;
    }

    public StatusCode getStatus() {
        return status;
    }

    public void setStatus(StatusCode status) {
        this.status = status;
    }

    public boolean isOK() {
        return StatusCode.OK.equals(getStatus());
    }

    public boolean isWarn() {
        return StatusCode.WARNING.equals(getStatus());
    }

    public ReadWriteErrorCode getErrorCode() {
        return errorCode;
    }
}
