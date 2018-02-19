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
package fr.gouv.vitam.metadata.core.reconstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;

/**
 * Reconstruction of Vitam Metadata Collections.<br>
 */
public class ReconstructionService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionService.class);
    private static final String RECONSTRUCTION_ITEM_MONDATORY_MSG = "the item defining reconstruction is mandatory.";
    private static final String RECONSTRUCTION_COLLECTION_MONDATORY_MSG = "the collection to reconstruct is mondatory.";
    private static final String RECONSTRUCTION_TENANT_MONDATORY_MSG = "the tenant to reconstruct is mondatory.";
    private static final String RECONSTRUCTION_LIMIT_POSITIVE_MSG = "the limit to reconstruct is should at least 0.";

    private static final String STRATEGY_ID = "default";

    private RestoreBackupService restoreBackupService;

    private VitamRepositoryProvider vitamRepositoryProvider;

    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    /**
     * Constructor
     * 
     * @param vitamRepositoryProvider vitamRepositoryProvider
     */
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider) {
        this(vitamRepositoryProvider, new RestoreBackupService(), LogbookLifeCyclesClientFactory.getInstance());
    }

    /**
     * Constructor for tests
     * 
     * @param vitamRepositoryProvider vitamRepositoryProvider
     * @param recoverBackupService recoverBackupService
     * @param logbookLifecycleClientFactory logbookLifecycleClientFactory
     */
    @VisibleForTesting
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider,
        RestoreBackupService recoverBackupService, LogbookLifeCyclesClientFactory logbookLifecycleClientFactory) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.restoreBackupService = recoverBackupService;
        this.logbookLifeCyclesClientFactory = logbookLifecycleClientFactory;
    }

    /**
     * Reconstruct a collection
     * 
     * @param reconstructionItem request for reconstruction
     * @return response of reconstruction
     * @throws DatabaseException database exception
     * @throws IllegalArgumentException invalid input
     */
    public ReconstructionResponseItem reconstruct(ReconstructionRequestItem reconstructionItem)
        throws DatabaseException {
        ParametersChecker.checkParameter(RECONSTRUCTION_ITEM_MONDATORY_MSG, reconstructionItem);
        ParametersChecker.checkParameter(RECONSTRUCTION_COLLECTION_MONDATORY_MSG, reconstructionItem.getCollection());
        ParametersChecker.checkParameter(RECONSTRUCTION_TENANT_MONDATORY_MSG, reconstructionItem.getTenant());
        if (reconstructionItem.getLimit() < 0) {
            throw new IllegalArgumentException(RECONSTRUCTION_LIMIT_POSITIVE_MSG);
        }
        LOGGER
            .info(String.format(
                "[Reconstruction]: Reconstruction of {%s} Collection on {%s} Vitam tenant from {%s} offset",
                reconstructionItem.getCollection(), reconstructionItem.getTenant(), reconstructionItem.getOffset()));
        return reconstructCollection(MetadataCollections.getFromValue(reconstructionItem.getCollection()),
            reconstructionItem.getTenant(), reconstructionItem.getOffset(), reconstructionItem.getLimit());
    }

    /**
     * Reconstruct collection.
     * 
     * @param collection collection
     * @param tenant tenant
     * @param offset offset (included in reconstruction)
     * @param limit number of data to reconstruct
     * @return response of reconstruction
     * @throws DatabaseException database exception
     * @throws IllegalArgumentException invalid input
     * @throws VitamRuntimeException storage error
     */
    private ReconstructionResponseItem reconstructCollection(MetadataCollections collection, int tenant, long offset,
        int limit)
        throws DatabaseException {

        ParametersChecker.checkParameter("Parameter collection is required.", collection);
        LOGGER.info(String
            .format(
                "[Reconstruction]: Start reconstruction of the {%s} collection on the Vitam tenant {%s} for %s elements starting from {%s}.",
                collection.name(), tenant, limit, offset));
        ReconstructionResponseItem response =
            new ReconstructionResponseItem().setCollection(collection.name()).setTenant(tenant).setOffset(offset);
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        final VitamMongoRepository mongoRepository = vitamRepositoryProvider.getVitamMongoRepository(collection);
        final VitamElasticsearchRepository esRepository = vitamRepositoryProvider.getVitamESRepository(collection);

        try {
            // This is a hack, we must set manually the tenant is the VitamSession (used and transmitted in the
            // headers)
            VitamThreadUtils.getVitamSession().setTenantId(tenant);

            // get the list of datas to backup.
            List<List<OfferLog>> listing = restoreBackupService.getListing(STRATEGY_ID, collection, offset, limit);

            for (List<OfferLog> listingBulk : listing) {

                List<MetadataBackupModel> bulkData = new ArrayList<>();
                for (OfferLog offerLog : listingBulk) {
                    MetadataBackupModel model = restoreBackupService.loadData(STRATEGY_ID, collection,
                        offerLog.getFileName(), offerLog.getSequence());
                    if (model != null && model.getMetadatas() != null && model.getLifecycle() != null &&
                        model.getOffset() != null) {
                        bulkData.add(model);
                    } else {
                        throw new StorageException(String.format(
                            "[Reconstruction]: Metadatas or Logbooklifecycle is not present in file {%s} for the collection {%s} on the tenant {%s}",
                            offerLog.getFileName(), collection, tenant));
                    }
                }

                // reconstruct Vitam collection from the backup datas.
                if (!bulkData.isEmpty()) {
                    reconstructCollectionMetadatas(mongoRepository, esRepository, bulkData);
                    reconstructCollectionLifecycles(collection, bulkData);
                    response.setOffset(Iterables.getLast(bulkData).getOffset());
                }

                // log the recontruction of Vitam collection.
                LOGGER.info(String.format(
                    "[Reconstruction]: the collection {%s} has been reconstructed on the tenant {%s} from {offset:%s} at %s",
                    collection.name(), tenant, offset, LocalDateUtil.now()));
            }
            response.setStatus(StatusCode.OK);
        } catch (DatabaseException de) {
            LOGGER.error(String.format(
                "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} metadatas on the tenant {%s} from {offset:%s}",
                collection, tenant, offset), de);
            response.setOffset(offset);
            response.setStatus(StatusCode.KO);
        } catch (StorageException se) {
            LOGGER.error(se.getMessage());
            response.setOffset(offset);
            response.setStatus(StatusCode.KO);
        } catch (LogbookClientException | InvalidParseOperationException exc) {
            LOGGER.error(String.format(
                "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} lifecycles on the tenant {%s} from {offset:%s}",
                collection, tenant, offset), exc);
            response.setOffset(offset);
            response.setStatus(StatusCode.KO);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
        return response;
    }

    /**
     * Reconstruct lifecycles in logbook
     * 
     * @param collection collection
     * @param bulk list of items to back up
     * @throws LogbookClientException error from logbook
     * @throws InvalidParseOperationException error parsing logbook response
     */
    private void reconstructCollectionLifecycles(MetadataCollections collection, List<MetadataBackupModel> bulk)
        throws LogbookClientException, InvalidParseOperationException {
        LOGGER.info(String.format("[Reconstruction]: Back up of lifecycles bulk"));
        try (LogbookLifeCyclesClient logbookLifecycleClient = logbookLifeCyclesClientFactory.getClient()) {
            List<JsonNode> lifecycles =
                bulk.stream()
                    .map(model -> {
                        try {
                            if (model.getLifecycle() != null) {
                                return JsonHandler
                                    .getFromString(
                                        model.getLifecycle().toJson(new JsonWriterSettings(JsonMode.STRICT)));
                            } else {
                                throw new VitamRuntimeException("lifecycle should not be null");
                            }
                        } catch (InvalidParseOperationException e) {
                            throw new VitamRuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
            switch (collection) {
                case UNIT:
                    logbookLifecycleClient.createRawbulkUnitlifecycles(lifecycles);
                    break;
                case OBJECTGROUP:
                    logbookLifecycleClient.createRawbulkObjectgrouplifecycles(lifecycles);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid collection");
            }

        } catch (VitamRuntimeException lifecycleParsingException) {
            throw new InvalidParseOperationException(lifecycleParsingException);
        }
    }

    /**
     * Reconstruct metadatas in databases
     * 
     * @param mongoRepository mongo access service for collection
     * @param esRepository elasticsearch access service for collection
     * @param bulk list of items to back up
     * @throws DatabaseException
     */
    private void reconstructCollectionMetadatas(final VitamMongoRepository mongoRepository,
        final VitamElasticsearchRepository esRepository, List<MetadataBackupModel> bulk)
        throws DatabaseException {
        LOGGER.info(String.format("[Reconstruction]: Back up of metadatas bulk"));
        List<Document> metadatas =
            bulk.stream().map(MetadataBackupModel::getMetadatas).collect(Collectors.toList());
        mongoRepository.saveOrUpdate(metadatas);
        esRepository.save(metadatas);
    }
}
