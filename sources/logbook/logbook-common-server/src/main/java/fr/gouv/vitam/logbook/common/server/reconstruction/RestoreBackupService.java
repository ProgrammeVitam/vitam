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
package fr.gouv.vitam.logbook.common.server.reconstruction;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.storage.engine.client.OfferLogHelper;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Service used to recover a Backup copy of logbook operation Vitam collection.<br/>
 */

public class RestoreBackupService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RestoreBackupService.class);

    private StorageClientFactory storageClientFactory;

    /**
     * Constructor
     */
    public RestoreBackupService() {
        this(StorageClientFactory.getInstance());
    }

    /**
     * Constructor for tests
     *
     * @param storageClientFactory storage client factory
     */
    @VisibleForTesting
    public RestoreBackupService(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * Retrieve list of offer log defining objects to reconstruct from offer log
     *
     * @param strategy storage strategy
     * @param offset offset
     * @param limit limit
     * @return list of offer log by bulk
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    public Iterator<List<OfferLog>> getListing(String strategy, long offset, int limit)
        throws StorageServerClientException, StorageNotFoundClientException {
        LOGGER.info(
            String.format(
                "[Reconstruction]: Retrieve listing of {%s} Collection on {%s} Vitam strategy from {%s} offset with {%s} limit",
                DataCategory.BACKUP_OPERATION.name(),
                strategy,
                offset,
                limit
            )
        );

        return Iterators.partition(
            OfferLogHelper.getListing(
                storageClientFactory,
                strategy,
                storageClientFactory.getClient().getReferentOffer(strategy),
                DataCategory.BACKUP_OPERATION,
                offset,
                Order.ASC,
                VitamConfiguration.getBatchSize(),
                limit
            ),
            VitamConfiguration.getBatchSize()
        );
    }

    /**
     * Load data from storage
     *
     * @param strategy storage strategy
     * @param filename name of file to load
     * @param offset offset
     * @return data
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    public LogbookBackupModel loadData(String strategy, String filename, long offset) throws StorageNotFoundException {
        LOGGER.info(
            String.format(
                "[Reconstruction]: Retrieve file {%s} from storage of {%s} Collection on {%s} Vitam strategy",
                filename,
                DataCategory.BACKUP_OPERATION.name(),
                strategy
            )
        );
        InputStream inputStream = null;
        Response response = null;
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            DataCategory type = DataCategory.BACKUP_OPERATION;
            String referentOfferForStrategy = storageClient.getReferentOffer(strategy);
            response = storageClient.getContainerAsync(
                strategy,
                referentOfferForStrategy,
                filename,
                type,
                AccessLogUtils.getNoLogAccessLog()
            );
            inputStream = response.readEntity(InputStream.class);
            LogbookOperation logbookOperationDocument = new LogbookOperation(
                JsonHandler.getFromInputStream(inputStream, JsonNode.class)
            );
            LogbookBackupModel logbookBackupModel = new LogbookBackupModel();
            logbookBackupModel.setLogbookOperation(logbookOperationDocument);
            logbookBackupModel.setLogbookId(logbookOperationDocument.getId());
            logbookBackupModel.setOffset(offset);
            return logbookBackupModel;
        } catch (
            StorageServerClientException
            | InvalidParseOperationException
            | StorageNotFoundClientException
            | StorageException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            throw new VitamRuntimeException("ERROR: Exception has been thrown when using storage service:", e);
        } finally {
            StreamUtils.closeSilently(inputStream);
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }
}
