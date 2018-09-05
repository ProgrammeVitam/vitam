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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Probative value Parameters
 */
public class ProbativeParameter {

    private String id;
    private Map<String, ProbativeUsageParameter> usageParameters;

    private List<ProbativeCheckReport> reports;

    private String message;

    private EvidenceStatus evidenceStatus;

    private String evIdAppSession;

    private String agIdApp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    ProbativeParameter( ) {
        reports = new ArrayList<>();
        usageParameters = new HashMap<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }


    public List<ProbativeCheckReport> getReports() {
        return reports;
    }

    public void setReports(List<ProbativeCheckReport> reports) {
        this.reports = reports;
    }

    public String getEvIdAppSession() {
        return evIdAppSession;
    }

    public void setEvIdAppSession(String evIdAppSession) {
        this.evIdAppSession = evIdAppSession;
    }

    public String getAgIdApp() {
        return agIdApp;
    }

    public void setAgIdApp(String agIdApp) {
        this.agIdApp = agIdApp;
    }

    public Map<String, ProbativeUsageParameter> getUsageParameters() {
        return usageParameters;
    }

    public void setUsageParameters(
        Map<String, ProbativeUsageParameter> usageParameters) {
        this.usageParameters = usageParameters;
    }
}
