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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Logbook operation model
 */
public class LogbookOperation extends LogbookEventOperation {

    @JsonIgnore
    public static LogbookOperation emptyWithEvDetData(String evDetData) {
        LogbookOperation operation = new LogbookOperation();
        operation.setEvDetData(evDetData);
        return operation;
    }

    /**
     * ID Tag
     */
    public static final String TAG_ID = "id";

    /**
     * Tenant Tag
     */
    public static final String TAG_TENANT = "tenant";

    /**
     * Hash Tag
     */
    public static final String HASH = "#";

    /**
     * Underscore tag
     */
    public static final String UNDERSCORE = "_";

    /**
     * Events tag
     */
    public static final String EVENTS = "events";

    @JsonProperty(HASH + TAG_ID)
    @JsonAlias(UNDERSCORE + TAG_ID)
    private String id;

    @JsonProperty(HASH + TAG_TENANT)
    @JsonAlias(UNDERSCORE + TAG_TENANT)
    private Integer tenant;

    @JsonProperty("agIdApp")
    private String agIdApp;

    @JsonProperty("evIdAppSession")
    private String evIdAppSession;

    @JsonProperty("events")
    private List<LogbookEventOperation> events;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * @param tenant the tenant to set
     */
    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    /**
     * @return the events
     */
    public List<LogbookEventOperation> getEvents() {
        return events;
    }

    /**
     * @param events the events to set
     */
    public void setEvents(List<LogbookEventOperation> events) {
        this.events = events;
    }

    /**
     * @return the agIdApp
     */
    public String getAgIdApp() {
        return agIdApp;
    }

    /**
     * @param agIdApp the agIdApp to set
     */
    public void setAgIdApp(String agIdApp) {
        this.agIdApp = agIdApp;
    }

    /**
     * @return the evIdAppSession
     */
    public String getEvIdAppSession() {
        return evIdAppSession;
    }

    /**
     * @param evIdAppSession the evIdAppSession to set
     */
    public void setEvIdAppSession(String evIdAppSession) {
        this.evIdAppSession = evIdAppSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogbookOperation operation = (LogbookOperation) o;
        return (
            Objects.equals(id, operation.id) &&
            Objects.equals(tenant, operation.tenant) &&
            Objects.equals(agIdApp, operation.agIdApp) &&
            Objects.equals(evIdAppSession, operation.evIdAppSession) &&
            Objects.equals(events, operation.events)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenant, agIdApp, evIdAppSession, events);
    }
}
