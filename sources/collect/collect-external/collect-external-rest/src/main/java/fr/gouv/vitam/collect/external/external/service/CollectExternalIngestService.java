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

package fr.gouv.vitam.collect.external.external.service;

import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalInvalidRequestException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalNotFoundException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalServerSideException;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientInvalidRequestException;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;

import java.io.IOException;
import java.io.InputStream;

import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;

public class CollectExternalIngestService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectExternalIngestService.class);

    /**
     * Generate transaction SIP (if not already generated), mark it as SENDING, and download SIP for ingest to Vitam.
     * Download the generated SIP of the transaction
     *
     * @throws VitamClientException exception occurs when parse operation failed
     */
    public String generateSipForIngest(
        CollectInternalClient collectInternalClient,
        IngestExternalClient ingestExternalClient,
        String transactionId
    ) throws CollectExternalException {
        try {
            LOGGER.info("Waiting for SIP availability for transaction '" + transactionId + "'");
            collectInternalClient.awaitTransactionValidation(transactionId);

            LOGGER.info("Uploading transaction SIP");
            collectInternalClient.changeTransactionStatus(transactionId, TransactionStatus.SENDING);
            String ingestOperationId;
            try (InputStream sipInputStream = collectInternalClient.downloadSIP(transactionId)) {
                RequestResponse<Void> response = ingestExternalClient.ingest(
                    new VitamContext(ParameterHelper.getTenantParameter()),
                    sipInputStream,
                    DEFAULT_WORKFLOW.name(),
                    RESUME.name()
                );
                ingestOperationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            }
            LOGGER.info("Ingest uploaded to Vitam with id: " + ingestOperationId);
            collectInternalClient.attachVitamOperationId(transactionId, ingestOperationId);
            collectInternalClient.changeTransactionStatus(transactionId, TransactionStatus.SENT);
            LOGGER.info("SIP sent with success ");
            return ingestOperationId;
        } catch (CollectInternalClientNotFoundException e) {
            throw new CollectExternalNotFoundException(
                "Cannot ingest SIP transaction " + transactionId + ". Bad Request",
                e
            );
        } catch (CollectInternalClientInvalidRequestException e) {
            throw new CollectExternalInvalidRequestException(
                "Cannot ingest SIP transaction " + transactionId + ". Bad Request",
                e
            );
        } catch (VitamClientException | IOException | IngestExternalException e) {
            throw new CollectExternalServerSideException("An error occurred during transaction upload", e);
        }
    }
}
