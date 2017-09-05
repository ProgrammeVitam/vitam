/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.model.logbook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Logbook operation event model
 */
public class LogbookEventOperation extends LogbookEvent {
    @JsonProperty("agIdApp")
    private String agIdApp;

    @JsonProperty("agIdAppSession")
    private String agIdAppSession;

    @JsonProperty("evIdReq")
    private String evIdReq;

    @JsonProperty("agIdSubm")
    private String agIdSubm;

    @JsonProperty("agIdOrig")
    private String agIdOrig;

    @JsonProperty("obIdReq")
    private String obIdReq;

    @JsonProperty("obIdIn")
    private String obIdIn;

    /**
     * @return the agIdApp
     */
    public String getAgIdApp() {
        return agIdApp;
    }

    /**
     * @param agIdApp the agIdApp to set
     *
     * @return this
     */
    public void setAgIdApp(String agIdApp) {
        this.agIdApp = agIdApp;
    }

    /**
     * @return the agIdAppSession
     */
    public String getAgIdAppSession() {
        return agIdAppSession;
    }

    /**
     * @param agIdAppSession the agIdAppSession to set
     *
     * @return this
     */
    public void setAgIdAppSession(String agIdAppSession) {
        this.agIdAppSession = agIdAppSession;
    }

    /**
     * @return the evIdReq
     */
    public String getEvIdReq() {
        return evIdReq;
    }

    /**
     * @param evIdReq the evIdReq to set
     *
     * @return this
     */
    public void setEvIdReq(String evIdReq) {
        this.evIdReq = evIdReq;
    }

    /**
     * @return the agIdSubm
     */
    public String getAgIdSubm() {
        return agIdSubm;
    }

    /**
     * @param agIdSubm the agIdSubm to set
     *
     * @return this
     */
    public void setAgIdSubm(String agIdSubm) {
        this.agIdSubm = agIdSubm;
    }

    /**
     * @return the agIdOrig
     */
    public String getAgIdOrig() {
        return agIdOrig;
    }

    /**
     * @param agIdOrig the agIdOrig to set
     *
     * @return this
     */
    public void setAgIdOrig(String agIdOrig) {
        this.agIdOrig = agIdOrig;
    }

    /**
     * @return the obIdReq
     */
    public String getObIdReq() {
        return obIdReq;
    }

    /**
     * @param obIdReq the obIdReq to set
     *
     * @return this
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
     *
     * @return this
     */
    public void setObIdIn(String obIdIn) {
        this.obIdIn = obIdIn;
    }



}
