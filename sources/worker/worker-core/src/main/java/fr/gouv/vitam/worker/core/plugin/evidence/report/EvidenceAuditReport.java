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
package fr.gouv.vitam.worker.core.plugin.evidence.report;

import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;

import java.util.List;

/**
 * EvidenceAuditReport class
 */
public class EvidenceAuditReport {


    private EvidenceStatus evidenceStatus;
    private List<EvidenceAuditReportLine> warningObjects;
    private List<EvidenceAuditReportLine> okObjects;
    private List<EvidenceAuditReportLine> koObjects;
    private MetadataType metadataType;
    private String message ;

    /**
     * getter for evidenceStatus
     **/
    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    /**
     * setter for evidenceStatus
     **/
    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    /**
     * getter for warningObjects
     **/
    public List<EvidenceAuditReportLine> getWarningObjects() {
        return warningObjects;
    }

    /**
     * setter for warningObjects
     **/
    public void setWarningObjects(
        List<EvidenceAuditReportLine> warningObjects) {
        this.warningObjects = warningObjects;
    }

    /**
     * getter for okObjects
     **/
    public List<EvidenceAuditReportLine> getOkObjects() {
        return okObjects;
    }

    /**
     * setter for okObjects
     **/
    public void setOkObjects(List<EvidenceAuditReportLine> okObjects) {
        this.okObjects = okObjects;
    }

    /**
     * getter for koObjects
     **/
    public List<EvidenceAuditReportLine> getKoObjects() {
        return koObjects;
    }

    /**
     * setter for koObjects
     **/
    public void setKoObjects(List<EvidenceAuditReportLine> koObjects) {
        this.koObjects = koObjects;
    }

    /**
     * getter for metadataType
     **/
    public MetadataType getMetadataType() {
        return metadataType;
    }

    /**
     * setter for metadataType
     **/
    public void setMetadataType(MetadataType metadataType) {
        this.metadataType = metadataType;
    }

    /**
     * getter for message
     **/
    public String getMessage() {
        return message;
    }

    /**
     * setter for message
     **/
    public void setMessage(String message) {
        this.message = message;
    }
}
