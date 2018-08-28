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
package fr.gouv.vitam.worker.core.plugin.certification;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CertificationRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * EvidenceAuditDatabaseCheck class
 */
public class EvidenceCertificateObjectGroupCheck extends ActionHandler {
    private static final String EVIDENCE_CERTIFICATE_CHECK_OBJECT_GROUP = "EVIDENCE_CERTIFICATE_CHECK_OBJECT_GROUP";

    private static final String DATA = "data";
    private CertificateService certificateService;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceCertificateObjectGroupCheck.class);


    @VisibleForTesting
    EvidenceCertificateObjectGroupCheck(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    public EvidenceCertificateObjectGroupCheck() {
        this(new CertificateService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_CERTIFICATE_CHECK_OBJECT_GROUP);

        String objectToAuditId = param.getObjectName();

        try {

            CertificationRequest certificationRequest = JsonHandler
                .getFromInputStream(handlerIO.getInputStreamFromWorkspace("request"), CertificationRequest.class);

            CertificateParameters parameters =
                certificateService.evidenceCertificateChecks(objectToAuditId, certificationRequest.getUsage(),
                    certificationRequest.getVersion());

            File newLocalFile = handlerIO.getNewLocalFile(objectToAuditId);
            JsonHandler.writeAsFile(parameters, newLocalFile);

            handlerIO.transferFileToWorkspace(DATA + File.separator + objectToAuditId,
                newLocalFile, true, false);


        } catch (VitamException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(EVIDENCE_CERTIFICATE_CHECK_OBJECT_GROUP)
            .setItemsStatus(EVIDENCE_CERTIFICATE_CHECK_OBJECT_GROUP, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }
}
