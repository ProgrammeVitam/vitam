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
package fr.gouv.vitam.functional.administration.core.reconstruction;

import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterBackupModel;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.OfferLogHelper;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.apache.commons.io.IOUtils;
import org.bson.Document;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Service used to recover a Backup copy of the given Vitam collection.<br/>
 */

public class RestoreBackupServiceImpl implements RestoreBackupService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RestoreBackupServiceImpl.class);

    private static final String OBJECT_ID_TAG = "objectId";
    private static final String EXTENSION_JSON = ".json";

    private final StorageClientFactory storageClientFactory;

    /**
     * Constructor
     */
    public RestoreBackupServiceImpl() {
        this.storageClientFactory = StorageClientFactory.getInstance();
    }

    @Override
    public Optional<String> getLatestSavedFileName(
        String strategy,
        String offerId,
        DataCategory type,
        FunctionalAdminCollections collection
    ) {
        try (
            final StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            CloseableIterator<ObjectEntry> listing = storageClient.listContainer(strategy, offerId, type)
        ) {
            // recover an intact backup copy for the reconstruction.
            Iterable<ObjectEntry> iterable = () -> listing;
            Stream<ObjectEntry> stream = StreamSupport.stream(iterable.spliterator(), false);
            // regex -> filter on json and the sequence's version.
            String regex = "\\d+_+(\\w+)_+(\\d+)?" + EXTENSION_JSON + "$";
            Pattern pattern = Pattern.compile(regex);

            Optional<Integer> result = stream
                .map(ObjectEntry::getObjectId)
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .filter(matcher -> collection.getName().equals(matcher.group(1)))
                .map(matcher -> Integer.valueOf(matcher.group(2)))
                .max(Comparator.naturalOrder());

            // get the last version of the json backup files.
            if (result.isPresent()) {
                return Optional.of(
                    FunctionalBackupService.getBackupFileName(
                        collection,
                        ParameterHelper.getTenantParameter(),
                        result.get()
                    )
                );
            }
        } catch (StorageServerClientException e) {
            LOGGER.error("ERROR: Exception has been thrown when using storage service:", e);
        } catch (StorageNotFoundClientException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<CollectionBackupModel> readLatestSavedFile(
        String strategy,
        String offerId,
        FunctionalAdminCollections collection
    ) {
        // get the last version of the json backup files.
        Optional<String> lastBackupVersion = getLatestSavedFileName(strategy, offerId, DataCategory.BACKUP, collection);

        if (lastBackupVersion.isPresent()) {
            Response response = null;
            try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                response = storageClient.getContainerAsync(
                    strategy,
                    offerId,
                    lastBackupVersion.get(),
                    DataCategory.BACKUP,
                    AccessLogUtils.getNoLogAccessLog()
                );
                final InputStream inputStream = response.readEntity(InputStream.class);

                // get backup collections to reconstruct.
                return Optional.of(JsonHandler.getFromInputStream(inputStream, CollectionBackupModel.class));
            } catch (
                StorageServerClientException
                | InvalidParseOperationException
                | StorageException
                | StorageUnavailableDataFromAsyncOfferClientException e
            ) {
                LOGGER.error("ERROR: Exception has been thrown when using storage service:", e);
            } finally {
                StreamUtils.consumeAnyEntityAndClose(response);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieve list of offer log defining objects to reconstruct from offer log
     *
     * @param strategy storage strategy
     * @param dataCategory collection
     * @param offset offset
     * @param limit limit
     * @return list of offer log by bulk
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    @Override
    public Iterator<List<OfferLog>> getListing(
        String strategy,
        DataCategory dataCategory,
        Long offset,
        int limit,
        Order order
    ) throws StorageServerClientException, StorageNotFoundClientException {
        LOGGER.info(
            String.format(
                "[Reconstruction]: Retrieve listing of {%s} Collection on {%s} Vitam strategy from {%s} offset with {%s} limit",
                dataCategory,
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
                dataCategory,
                offset,
                order,
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
     * @param collection collection
     * @param filename name of file to load
     * @param offset offset
     * @return data
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    public AccessionRegisterBackupModel loadData(
        String strategy,
        FunctionalAdminCollections collection,
        String filename,
        long offset
    ) {
        LOGGER.info(
            String.format(
                "[Reconstruction]: Retrieve file {%s} from storage of {%s} Collection on {%s} Vitam strategy",
                filename,
                collection.name(),
                strategy
            )
        );
        Response response = null;
        InputStream inputStream = null;
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            DataCategory type;
            switch (collection) {
                case ACCESSION_REGISTER_DETAIL:
                    type = DataCategory.ACCESSION_REGISTER_DETAIL;
                    break;
                case ACCESSION_REGISTER_SYMBOLIC:
                    type = DataCategory.ACCESSION_REGISTER_SYMBOLIC;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("ERROR: Invalid collection {%s}", collection));
            }
            String referentOfferForStrategy = storageClient.getReferentOffer(strategy);
            response = storageClient.getContainerAsync(
                strategy,
                referentOfferForStrategy,
                filename,
                type,
                AccessLogUtils.getNoLogAccessLog()
            );
            inputStream = response.readEntity(InputStream.class);
            Document doc = JsonHandler.getFromInputStream(inputStream, Document.class);
            AccessionRegisterBackupModel accessionRegisterBackupModel = new AccessionRegisterBackupModel();
            if (type.equals(DataCategory.ACCESSION_REGISTER_DETAIL)) {
                accessionRegisterBackupModel.setAccessionRegisterDetail(doc);
            } else {
                accessionRegisterBackupModel.setAccessionRegisterSympbolic(doc);
            }

            accessionRegisterBackupModel.setOffset(offset);
            return accessionRegisterBackupModel;
        } catch (
            StorageServerClientException
            | InvalidParseOperationException
            | StorageNotFoundClientException
            | StorageException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            throw new VitamRuntimeException("ERROR: Exception has been thrown when using storage service:", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }
}
