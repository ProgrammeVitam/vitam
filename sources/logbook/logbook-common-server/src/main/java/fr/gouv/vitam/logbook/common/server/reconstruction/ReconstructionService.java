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
package fr.gouv.vitam.logbook.common.server.reconstruction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookTransformData;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reconstruction of Vitam Logbook Operation Collections.<br>
 */
public class ReconstructionService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionService.class);
    private static final AlertService alertService = new AlertServiceImpl();

    private static final String RECONSTRUCTION_ITEM_MONDATORY_MSG = "the item defining reconstruction is mandatory.";
    private static final String RECONSTRUCTION_TENANT_MONDATORY_MSG = "the tenant to reconstruct is mondatory.";
    private static final String RECONSTRUCTION_LIMIT_POSITIVE_MSG = "the limit to reconstruct is should at least 0.";

    public static final String LOGBOOK = "logbook";

    private RestoreBackupService restoreBackupService;
    private VitamRepositoryProvider vitamRepositoryProvider;
    private AdminManagementClientFactory adminManagementClientFactory;
    private LogbookTransformData logbookTransformData;

    private OffsetRepository offsetRepository;

    /**
     * Constructor
     *
     * @param vitamRepositoryProvider vitamRepositoryProvider
     * @param offsetRepository
     */
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider,
        OffsetRepository offsetRepository) {
        this(vitamRepositoryProvider, new RestoreBackupService(), AdminManagementClientFactory.getInstance(),
            new LogbookTransformData(), offsetRepository);
    }

    /**
     * Constructor for tests
     *
     * @param vitamRepositoryProvider vitamRepositoryProvider
     * @param recoverBackupService recoverBackupService
     * @param adminManagementClientFactory adminManagementClientFactory
     * @param logbookTransformData logbookTransformData
     * @param offsetRepository
     */
    @VisibleForTesting
    public ReconstructionService(VitamRepositoryProvider vitamRepositoryProvider,
        RestoreBackupService recoverBackupService, AdminManagementClientFactory adminManagementClientFactory,
        LogbookTransformData logbookTransformData,
        OffsetRepository offsetRepository) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.restoreBackupService = recoverBackupService;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.logbookTransformData = logbookTransformData;
        this.offsetRepository = offsetRepository;
    }

    /**
     * Reconstruct logbook operation on a tenant
     *
     * @param reconstructionItem request for reconstruction
     * @return response of reconstruction
     * @throws DatabaseException database exception
     * @throws IllegalArgumentException invalid input
     */
    public ReconstructionResponseItem reconstruct(ReconstructionRequestItem reconstructionItem)
        throws DatabaseException {
        ParametersChecker.checkParameter(RECONSTRUCTION_ITEM_MONDATORY_MSG, reconstructionItem);
        ParametersChecker.checkParameter(RECONSTRUCTION_TENANT_MONDATORY_MSG, reconstructionItem.getTenant());
        if (reconstructionItem.getLimit() < 0) {
            throw new IllegalArgumentException(RECONSTRUCTION_LIMIT_POSITIVE_MSG);
        }
        LOGGER
            .info(String.format(
                "[Reconstruction]: Reconstruction of {%s} Collection on {%s} Vitam tenant",
                DataCategory.BACKUP_OPERATION.name(), reconstructionItem.getTenant()));
        return reconstructCollection(reconstructionItem.getTenant(),
            reconstructionItem.getLimit());
    }

    /**
     * Reconstruct collection logbook operation.
     *
     * @param tenant tenant
     * @param limit number of data to reconstruct
     * @return response of reconstruction
     * @throws DatabaseException database exception
     * @throws IllegalArgumentException invalid input
     * @throws VitamRuntimeException storage error
     */
    private ReconstructionResponseItem reconstructCollection(int tenant, int limit)
        throws DatabaseException {

        final long offset = offsetRepository.findOffsetBy(tenant, LOGBOOK);

        LOGGER.info(String
            .format(
                "[Reconstruction]: Start reconstruction of the {%s} collection on the Vitam tenant {%s} for %s elements starting from {%s}.",
                DataCategory.BACKUP_OPERATION.name(), tenant, limit, offset));
        ReconstructionResponseItem response = new ReconstructionResponseItem().setTenant(tenant);
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        final VitamMongoRepository mongoRepository =
            vitamRepositoryProvider.getVitamMongoRepository(LogbookCollections.OPERATION.getVitamCollection());
        final VitamElasticsearchRepository esRepository =
            vitamRepositoryProvider.getVitamESRepository(LogbookCollections.OPERATION.getVitamCollection());

        long newOffset = offset;

        try {
            // This is a hack, we must set manually the tenant is the VitamSession (used and transmitted in the
            // headers)
            VitamThreadUtils.getVitamSession().setTenantId(tenant);

            Iterator<List<OfferLog>> listing = restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), offset, limit);

            while (listing.hasNext()) {

                List<OfferLog> listingBulk = listing.next();

                List<LogbookBackupModel> bulkData = new ArrayList<>();
                for (OfferLog offerLog : listingBulk) {

                    try {

                        LogbookBackupModel model =
                            restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), offerLog.getFileName(), offerLog.getSequence());

                        if (model.getLogbookOperation() == null || model.getLogbookId() == null) {
                            throw new StorageException(String.format(
                                "[Reconstruction]: Invalid LogbookOperation in file {%s} on the tenant {%s}",
                                offerLog.getFileName(), tenant));
                        }
                        bulkData.add(model);

                    } catch (StorageNotFoundException ex) {
                        alertService.createAlert(VitamLogLevel.ERROR, String.format(
                            "[Reconstruction]: LogbookOperation is not present in file {%s} on the tenant {%s}",
                            offerLog.getFileName(), tenant));
                        throw new StorageException(String.format(
                            "[Reconstruction]: LogbookOperation is not present in file {%s} on the tenant {%s}",
                            offerLog.getFileName(), tenant), ex);
                    }
                }

                // reconstruct Vitam collection from the backup datas.
                if (!bulkData.isEmpty()) {
                    reconstructCollectionLogbookOperation(mongoRepository, esRepository, bulkData);
                    LogbookBackupModel last = Iterables.getLast(bulkData);
                    newOffset = last.getOffset();
                }

                // log the recontruction of Vitam collection.
                LOGGER.info(String.format(
                    "[Reconstruction]: the collection {%s} has been reconstructed on the tenant {%s} from {offset:%s} at %s",
                    DataCategory.BACKUP_OPERATION.name(), tenant, offset, LocalDateUtil.now()));
            }
            response.setStatus(StatusCode.OK);
        } catch (DatabaseException em) {
            LOGGER.error(String.format(
                "[Reconstruction]: Exception has been thrown when reconstructing Vitam collection {%s} on the tenant {%s} from {offset:%s}",
                DataCategory.BACKUP_OPERATION.name(), tenant, offset), em);
            newOffset = offset;
            response.setStatus(StatusCode.KO);
        } catch (StorageException se) {
            LOGGER.error(se.getMessage());
            newOffset = offset;
            response.setStatus(StatusCode.KO);
        } finally {
            offsetRepository.createOrUpdateOffset(tenant, LOGBOOK, newOffset);
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
        return response;
    }

    /**
     * Reconstruct logbookOperations in databases
     *
     * @param mongoRepository mongo access service for collection
     * @param esRepository elasticsearch access service for collection
     * @param bulk list of items to back up
     * @throws DatabaseException
     */
    private void reconstructCollectionLogbookOperation(final VitamMongoRepository mongoRepository,
        final VitamElasticsearchRepository esRepository, List<LogbookBackupModel> bulk)
        throws DatabaseException {
        LOGGER.info("[Reconstruction]: Back up of logbookOperation bulk");
        List<Document> logbooks =
            bulk.stream().map(LogbookBackupModel::getLogbookOperation).collect(Collectors.toList());
        mongoRepository.saveOrUpdate(logbooks);
        logbooks.forEach(logbook -> this.logbookTransformData.transformDataForElastic(logbook));
        esRepository.save(logbooks);
    }
}
