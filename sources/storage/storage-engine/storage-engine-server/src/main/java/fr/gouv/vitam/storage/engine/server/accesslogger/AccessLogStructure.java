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
package fr.gouv.vitam.storage.engine.server.accesslogger;

import java.util.Date;

public class AccessLogStructure {

    private final String applicativeContextId;
    private final String accessContractId;
    private final String xRequestId;/* Useless because writable in log ? */
    private final String ingestId;/* Kezako ? */
    private final String objectId;
    private final String archiveUnitId;/* is it needed ? Is it mandatory ? */
    private final String objectType;/* container ? */
    private final Long objectSize;
    private final Date responseDate;

    public AccessLogStructure(
        String applicativeContextId,
        String accessContractId,
        String xRequestId,
        String objectId,
        Long objectSize,
        Date responseDate,
        String objectType,
        String ingestId,
        String archiveUnitId
    ) {
        this.applicativeContextId = applicativeContextId;
        this.accessContractId = accessContractId;
        this.xRequestId = xRequestId;
        this.ingestId = ingestId;
        this.objectId = objectId;
        this.objectType = objectType;
        this.objectSize = objectSize;
        this.responseDate = responseDate;
        this.archiveUnitId = archiveUnitId;
    }

    public String getApplicativeContextId() {
        return applicativeContextId;
    }

    public String getAccessContractId() {
        return accessContractId;
    }

    public String getxRequestId() {
        return xRequestId;
    }

    public String getIngestId() {
        return ingestId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public Long getObjectSize() {
        return objectSize;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public String getArchiveUnitId() {
        return archiveUnitId;
    }
}
