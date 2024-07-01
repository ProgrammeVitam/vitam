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
package fr.gouv.vitam.common.model.logbook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Logbook operation event model
 */
public class LogbookEventOperation extends LogbookEvent {

    @JsonProperty("evIdReq")
    private String evIdReq;

    @JsonProperty("agIdExt")
    private String agIdExt;

    @JsonProperty("obIdReq")
    private String obIdReq;

    @JsonProperty("obIdIn")
    private String obIdIn;

    /**
     * @return the evIdReq
     */
    public String getEvIdReq() {
        return evIdReq;
    }

    /**
     * @param evIdReq the evIdReq to set
     */
    public void setEvIdReq(String evIdReq) {
        this.evIdReq = evIdReq;
    }

    /**
     * @return the agIdExt
     */
    public String getAgIdExt() {
        return agIdExt;
    }

    /**
     * @param agIdExt the agIdExt to set
     */
    public void setAgIdExt(String agIdExt) {
        this.agIdExt = agIdExt;
    }

    /**
     * @return the obIdReq
     */
    public String getObIdReq() {
        return obIdReq;
    }

    /**
     * @param obIdReq the obIdReq to set
     */
    public void setObIdReq(String obIdReq) {
        this.obIdReq = obIdReq;
    }

    /**
     * @return the obIdIn
     */
    public String getObIdIn() {
        return obIdIn;
    }

    /**
     * @param obIdIn the obIdIn to set
     */
    public void setObIdIn(String obIdIn) {
        this.obIdIn = obIdIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogbookEventOperation that = (LogbookEventOperation) o;
        return (
            Objects.equals(evIdReq, that.evIdReq) &&
            Objects.equals(agIdExt, that.agIdExt) &&
            Objects.equals(obIdReq, that.obIdReq) &&
            Objects.equals(obIdIn, that.obIdIn)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(evIdReq, agIdExt, obIdReq, obIdIn);
    }
}
