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
package fr.gouv.vitam.functional.administration.core.accession.register;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.PushAction;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSymbolicModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.retryable.RetryableOnResult;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.ReferentialAccessionRegisterSummaryUtil;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.FunctionalBackupServiceException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel.ID;
import static fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel.VERSION;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.EVENTS;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.LAST_UPDATE;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.OBJECT_SIZE;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.OPC;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.OPI;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.ORIGINATING_AGENCY;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.TOTAL_OBJECTGROUPS;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.TOTAL_OBJECTS;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.TOTAL_UNITS;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary.DELETED;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary.INGESTED;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary.REMAINED;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC;

/**
 * Referential Accession Register Implement
 */
public class ReferentialAccessionRegisterImpl implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialAccessionRegisterImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final FunctionalBackupService functionalBackupService;
    private final ReferentialAccessionRegisterSummaryUtil referentialAccessionRegisterSummaryUtil;

    private final AlertService alertService = new AlertServiceImpl(LOGGER);
    private final MetaDataClientFactory metaDataClientFactory;
    private final int accessionRegisterSymbolicThreadPoolSize;

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access configuration
     * @param metaDataClientFactory
     * @param configuration
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        MetaDataClientFactory metaDataClientFactory, AdminManagementConfiguration configuration) {
        this(dbConfiguration, new FunctionalBackupService(vitamCounterService), metaDataClientFactory,
            configuration.getAccessionRegisterSymbolicThreadPoolSize());
    }

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access configuration
     * @param metaDataClientFactory
     * @param accessionRegisterSymbolicThreadPoolSize
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration,
        FunctionalBackupService functionalBackupService,
        MetaDataClientFactory metaDataClientFactory, int accessionRegisterSymbolicThreadPoolSize) {
        mongoAccess = dbConfiguration;
        this.functionalBackupService = functionalBackupService;
        this.metaDataClientFactory = metaDataClientFactory;
        this.accessionRegisterSymbolicThreadPoolSize = accessionRegisterSymbolicThreadPoolSize;
        this.referentialAccessionRegisterSummaryUtil = new ReferentialAccessionRegisterSummaryUtil();
    }

    public void createAccessionRegisterSymbolic(List<Integer> tenants) throws ReferentialException {

        int threadPoolSize = Math.min(this.accessionRegisterSymbolicThreadPoolSize, tenants.size());
        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);

        try {
            List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

            for (Integer tenantId : tenants) {
                CompletableFuture<Void> completableFuture =
                    CompletableFuture.runAsync(
                        () -> {
                            Thread.currentThread().setName("AccessionRegisterSymbolic-" + tenantId);
                            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                            try {
                                createAccessionRegisterSymbolic();
                            } catch (Exception e) {
                                alertService.createAlert(VitamLogLevel.ERROR,
                                    "An error occurred during AccessionRegisterSymbolic update for tenant " + tenantId);
                                throw new RuntimeException(
                                    "An error occurred during AccessionRegisterSymbolic update for tenant " + tenantId,
                                    e);
                            }
                        }, executorService);
                completableFutures.add(completableFuture);
            }

            boolean allTenantsSucceeded = true;
            for (CompletableFuture<Void> completableFuture : completableFutures) {
                try {
                    completableFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ReferentialException("AccessionRegisterSymbolic update interrupted", e);
                } catch (ExecutionException e) {
                    LOGGER.error("AccessionRegisterSymbolic update failed", e);
                    allTenantsSucceeded = false;
                }
            }

            if (!allTenantsSucceeded) {
                throw new ReferentialException("One or more AccessionRegisterSymbolic updates failed");
            }

        } finally {
            executorService.shutdown();
        }
    }

    private void createAccessionRegisterSymbolic()
        throws MetaDataExecutionException, MetaDataClientServerException, ReferentialException,
        DocumentAlreadyExistsException, InvalidParseOperationException, SchemaValidationException {

        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {

            ArrayNode accessionRegisterSymbolic = (ArrayNode) metadataClient.createAccessionRegisterSymbolic()
                .get("$results");

            List<AccessionRegisterSymbolic> accessionRegisterSymbolicsToInsert =
                StreamSupport.stream(accessionRegisterSymbolic.spliterator(), false)
                    .map(AccessionRegisterSymbolic::new)
                    .collect(Collectors.toList());

            if (accessionRegisterSymbolicsToInsert.isEmpty()) {
                return;
            }

            insertAccessionRegisterSymbolic(accessionRegisterSymbolicsToInsert);
        }
    }

    /**
     * Insert a list of accession register symbolic.
     *
     * @param accessionRegisterSymbolics to insert
     * @throws ReferentialException
     * @throws SchemaValidationException
     * @throws InvalidParseOperationException
     */
    private void insertAccessionRegisterSymbolic(List<AccessionRegisterSymbolic> accessionRegisterSymbolics)
        throws ReferentialException, SchemaValidationException, InvalidParseOperationException,
        DocumentAlreadyExistsException {

        List<JsonNode> jsonNodes = new ArrayList<>();
        for (AccessionRegisterSymbolic accessionRegisterSymbolic : accessionRegisterSymbolics) {
            jsonNodes.add(JsonHandler.toJsonNode(accessionRegisterSymbolic));
        }

        mongoAccess.insertDocuments(JsonHandler.createArrayNode().addAll(jsonNodes), ACCESSION_REGISTER_SYMBOLIC);

        // Store Backup copy in storage
        try {
            for (AccessionRegisterSymbolic accessionRegisterSymbolic : accessionRegisterSymbolics) {
                functionalBackupService.saveDocument(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC,
                    mongoAccess.getDocumentById(accessionRegisterSymbolic.getId(),
                        FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC));
            }
        } catch (FunctionalBackupServiceException e) {
            throw new ReferentialException("Store backup register symbolic Error", e);
        }
    }


    /**
     * Find the accession register symbolic filtered by the query dsl,
     * if an empty query dsl is provided, the last 20 accession register
     * symbolics will be returned.
     *
     * @param queryDsl that filter the accession register to find
     * @return the list of accession register symbolic or an empty list
     */
    public RequestResponseOK<AccessionRegisterSymbolicModel> findAccessionRegisterSymbolic(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(queryDsl, ACCESSION_REGISTER_SYMBOLIC)) {
            return result
                .getRequestResponseOK(queryDsl, AccessionRegisterSymbolic.class, AccessionRegisterSymbolicModel.class);

        }
    }

    /**
     * @param registerDetail to create in Mongodb
     * @throws ReferentialException throws when insert mongodb error
     */
    public void createOrUpdateAccessionRegister(AccessionRegisterDetailModel registerDetail)
        throws BadRequestException, ReferentialException {

        LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
            registerDetail.getOriginatingAgency());

        // In case of ingest operation, opc is equal to opi
        // So, we create the accession detail
        // Else if opc != opi, must be an operation other than INGEST. Elimination, Transfer. In this case just update
        try {
            try {
                if (!registerDetail.getOpc().equals(registerDetail.getOpi())) {
                    addEventToAccessionRegisterDetail(registerDetail);
                } else {
                    JsonNode doc = VitamFieldsHelper.removeHash(JsonHandler.toJsonNode(registerDetail));
                    mongoAccess.insertDocument(doc,
                            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)
                        .close();
                }

                // Warn, this is not idempotent, if error occurs while updating accession register summary then retry will fail early
                updateAccessionRegisterSummary(registerDetail);

            } catch (DocumentAlreadyExistsException e) {
                LOGGER.warn(e);
                alertService.createAlert(VitamLogLevel.WARN,
                    "AccessionRegisterSummary maybe not up to date for the originating agency (" +
                        registerDetail.getOriginatingAgency() + ") ");
            }

            storeAccessionRegisterDetail(registerDetail);

        } catch (final InvalidParseOperationException | SchemaValidationException e) {
            throw new BadRequestException("Create register detail error", e);
        }
    }

    private void storeAccessionRegisterDetail(AccessionRegisterDetailModel registerDetail)
        throws ReferentialException {
        try {
            Document docToStorage =
                findAccessionRegisterDetail(registerDetail.getOriginatingAgency(), registerDetail.getOpi());
            // Store Backup copy in storage
            functionalBackupService.saveDocument(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, docToStorage);
        } catch (FunctionalBackupServiceException e) {
            throw new ReferentialException("Store backup register detail Error", e);
        }
    }

    void addEventToAccessionRegisterDetail(AccessionRegisterDetailModel newRegisterDetail)
        throws ReferentialException, DocumentAlreadyExistsException {

        // checks ------------------------------------------------------------
        ParametersChecker.checkParameter("Register detail mustn't be null", newRegisterDetail);
        ParametersChecker.checkParameter("Register opi mustn't be null", newRegisterDetail.getOpi());
        ParametersChecker.checkParameter("Register opc mustn't be null", newRegisterDetail.getOpc());
        ParametersChecker.checkParameter("Register tenant mustn't be null", newRegisterDetail.getTenant());
        ParametersChecker.checkParameter("Register originatingAgency mustn't be null",
            newRegisterDetail.getOriginatingAgency());

        RetryableOnResult<Boolean, ReferentialException> retryable = new RetryableOnResult<>(
            new RetryableParameters(VitamConfiguration.getOptimisticLockRetryNumber(),
                VitamConfiguration.getOptimisticLockSleepTime(), VitamConfiguration.getOptimisticLockSleepTime(),
                VitamConfiguration.getOptimisticLockSleepTime(), TimeUnit.MILLISECONDS, VitamLogLevel.WARN),
            success -> !success
        );

        try {
            retryable.exec(() -> {
                try {
                    return tryAddEventToAccessionRegisterDetailWithOptimisticLock(newRegisterDetail);
                } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException |
                         DocumentAlreadyExistsException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReferentialException) {
                throw (ReferentialException) e.getCause();
            }
            if (e.getCause() instanceof DocumentAlreadyExistsException) {
                throw (DocumentAlreadyExistsException) e.getCause();
            }
            throw e;
        }
    }

    private boolean tryAddEventToAccessionRegisterDetailWithOptimisticLock(
        AccessionRegisterDetailModel newRegisterDetail)
        throws ReferentialException, InvalidParseOperationException, ConcurrentModificationException,
        InvalidCreateOperationException, DocumentAlreadyExistsException {

        LOGGER.debug("Update register ID / Originating Agency: {} / {}", newRegisterDetail.getId(),
            newRegisterDetail.getOriginatingAgency());
        Select select = (Select) new Select().setQuery(QueryHelper.and().add(
            QueryHelper.eq(ORIGINATING_AGENCY, newRegisterDetail.getOriginatingAgency()),
            QueryHelper.eq(OPI, newRegisterDetail.getOpi())));
        List<AccessionRegisterDetailModel> documents = mongoAccess.findDocuments(select.getFinalSelect(),
            ACCESSION_REGISTER_DETAIL).getDocuments(AccessionRegisterDetail.class, AccessionRegisterDetailModel.class);
        if (CollectionUtils.isEmpty(documents)) {
            throw new ReferentialException("Document not found");
        }
        AccessionRegisterDetailModel accessionRegisterDetailStored = documents.get(0);
        if (accessionRegisterDetailStored.getEvents().stream()
            .anyMatch(e -> Objects.equals(e.getOperation(), newRegisterDetail.getOpc()))) {
            throw new DocumentAlreadyExistsException(String.format(
                "Accession register detail for originating agency (%s) and opi (%s) found and already contains the detail (%s)",
                newRegisterDetail.getOriginatingAgency(), newRegisterDetail.getOpi(), newRegisterDetail.getOpc()));
        }
        // MAJ ---------------------------------------------------------------
        try {
            mergeNewRegisterDetailIntoOld(accessionRegisterDetailStored, newRegisterDetail);
            List<Action> actions = new ArrayList<>();

            actions.add(new SetAction(LAST_UPDATE,
                LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())));
            actions.add(new SetAction(OPC,
                accessionRegisterDetailStored.getOpc()));
            actions.add(new PushAction(EVENTS, JsonHandler.toJsonNode(
                convertRegisterDetailToRegisterEvent(newRegisterDetail))));

            actions.add(new SetAction(TOTAL_OBJECTGROUPS + "." + INGESTED,
                accessionRegisterDetailStored.getTotalObjectsGroups().getIngested()));
            actions.add(new SetAction(TOTAL_OBJECTGROUPS + "." + DELETED,
                accessionRegisterDetailStored.getTotalObjectsGroups().getDeleted()));
            actions.add(new SetAction(TOTAL_OBJECTGROUPS + "." + REMAINED,
                accessionRegisterDetailStored.getTotalObjectsGroups().getRemained()));

            actions.add(new SetAction(TOTAL_OBJECTS + "." + INGESTED,
                accessionRegisterDetailStored.getTotalObjects().getIngested()));
            actions.add(new SetAction(TOTAL_OBJECTS + "." + DELETED,
                accessionRegisterDetailStored.getTotalObjects().getDeleted()));
            actions.add(new SetAction(TOTAL_OBJECTS + "." + REMAINED,
                accessionRegisterDetailStored.getTotalObjects().getRemained()));

            actions.add(new SetAction(TOTAL_UNITS + "." + INGESTED,
                accessionRegisterDetailStored.getTotalUnits().getIngested()));
            actions.add(new SetAction(TOTAL_UNITS + "." + DELETED,
                accessionRegisterDetailStored.getTotalUnits().getDeleted()));
            actions.add(new SetAction(TOTAL_UNITS + "." + REMAINED,
                accessionRegisterDetailStored.getTotalUnits().getRemained()));

            actions.add(new SetAction(OBJECT_SIZE + "." + INGESTED,
                accessionRegisterDetailStored.getObjectSize().getIngested()));
            actions.add(new SetAction(OBJECT_SIZE + "." + DELETED,
                accessionRegisterDetailStored.getObjectSize().getDeleted()));
            actions.add(new SetAction(OBJECT_SIZE + "." + REMAINED,
                accessionRegisterDetailStored.getObjectSize().getRemained()));

            if (statusShouldBeUnstored(accessionRegisterDetailStored)) {
                actions.add(new SetAction(AccessionRegisterDetail.STATUS,
                    AccessionRegisterStatus.UNSTORED.name()));
            } else {
                actions.add(new SetAction(AccessionRegisterDetail.STATUS,
                    accessionRegisterDetailStored.getStatus().name()));
            }
            // request -----------------------------------------------------------
            Update update = ((Update) new Update().setQuery(QueryHelper.and().add(
                QueryHelper.eq(ID, accessionRegisterDetailStored.getId()),
                QueryHelper.eq(VERSION, accessionRegisterDetailStored.getVersion()))
            ));

            update.addActions(actions.toArray(Action[]::new));
            DbRequestResult dbRequestResult =
                mongoAccess.updateData(update.getFinalUpdate(), ACCESSION_REGISTER_DETAIL);

            // Update failed if no document updated
            return (dbRequestResult.getCount() > 0L);

        } catch (final Exception ex) {
            throw new ReferentialException("Create register detail error", ex);
        }
    }

    private boolean statusShouldBeUnstored(AccessionRegisterDetailModel accessionRegisterDetail) {
        return (accessionRegisterDetail.getStatus() != AccessionRegisterStatus.UNSTORED
            && accessionRegisterDetail.getTotalObjectsGroups().getRemained() == 0
            && accessionRegisterDetail.getTotalUnits().getRemained() == 0
            && accessionRegisterDetail.getTotalObjects().getRemained() == 0
            && accessionRegisterDetail.getObjectSize().getRemained() == 0);
    }

    private void mergeNewRegisterDetailIntoOld(AccessionRegisterDetailModel oldOne,
        AccessionRegisterDetailModel newOne) {
        oldOne.setOpc(newOne.getOpc());
        oldOne.setStatus(newOne.getStatus());
        oldOne.setTotalObjectsGroups(mergeNewValueDetailIntoOld(oldOne.getTotalObjectsGroups(),
            newOne.getTotalObjectsGroups()));
        oldOne.setTotalObjects(mergeNewValueDetailIntoOld(oldOne.getTotalObjects(), newOne.getTotalObjects()));
        oldOne.setTotalUnits(mergeNewValueDetailIntoOld(oldOne.getTotalUnits(), newOne.getTotalUnits()));
        oldOne.setObjectSize(mergeNewValueDetailIntoOld(oldOne.getObjectSize(), newOne.getObjectSize()));
    }

    private RegisterValueDetailModel mergeNewValueDetailIntoOld(RegisterValueDetailModel oldOne,
        RegisterValueDetailModel newOne) {
        oldOne.setIngested(oldOne.getIngested() + newOne.getIngested());
        oldOne.setRemained(oldOne.getRemained() + newOne.getRemained());
        oldOne.setDeleted(oldOne.getDeleted() + newOne.getDeleted());
        return oldOne;
    }

    private RegisterValueEventModel convertRegisterDetailToRegisterEvent(AccessionRegisterDetailModel registerDetail) {
        return new RegisterValueEventModel()
            .setOperation(registerDetail.getOpc())
            .setOperationType(registerDetail.getOperationType())
            .setTotalGots(registerDetail.getTotalObjectsGroups().getRemained())
            .setTotalObjects(registerDetail.getTotalObjects().getRemained())
            .setTotalUnits(registerDetail.getTotalUnits().getRemained())
            .setObjectSize(registerDetail.getObjectSize().getRemained())
            .setCreationdate(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
    }

    @Override
    public void close() {
        // Empty
    }

    /**
     * search for an accession register's summary
     *
     * @param select the search criteria for the select operation
     * @return A list of AccressionRegisterSummaries matching the 'select' criteria.
     * @throws ReferentialException If the search's result is null or empty, or if the mongo search throw error
     */
    public RequestResponseOK<AccessionRegisterSummary> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result = mongoAccess.findDocuments(select, ACCESSION_REGISTER_SUMMARY)) {
            return result.getRequestResponseOK(select, AccessionRegisterSummary.class);
        }
    }

    /**
     * search for an accession register's operation detail
     *
     * @param select the search criteria for the select operation
     * @return A list of AccressionRegisterDetails matching the 'select' criteria.
     * @throws ReferentialException If the search's result is null or empty, or if the mongo search throw error
     */
    public RequestResponseOK<AccessionRegisterDetail> findDetail(JsonNode select) throws ReferentialException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(select, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
            return result.getRequestResponseOK(select, AccessionRegisterDetail.class);
        }
    }

    private VitamDocument<AccessionRegisterDetail> findAccessionRegisterDetail(String originatingAgency, String opi) {
        Bson filterQuery = and(eq(ORIGINATING_AGENCY, originatingAgency),
            eq(OPI, opi));
        return FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL
            .<VitamDocument<AccessionRegisterDetail>>getCollection()
            .find(filterQuery).iterator().next();
    }

    private void updateAccessionRegisterSummary(AccessionRegisterDetailModel registerDetail)
        throws ReferentialException, BadRequestException {
        // store accession register summary
        try {

            final AccessionRegisterSummary accessionRegister = referentialAccessionRegisterSummaryUtil
                .initAccessionRegisterSummary(registerDetail.getOriginatingAgency(),
                    GUIDFactory.newAccessionRegisterSummaryGUID(ParameterHelper.getTenantParameter()).getId());

            LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
                registerDetail.getOriginatingAgency());

            mongoAccess.insertDocument(JsonHandler.toJsonNode(accessionRegister),
                ACCESSION_REGISTER_SUMMARY);
        } catch (DocumentAlreadyExistsException e) {
            // Do nothing
        } catch (final InvalidParseOperationException | SchemaValidationException e) {
            throw new BadRequestException(e);
        }

        try {
            Update update = referentialAccessionRegisterSummaryUtil.generateUpdateQuery(registerDetail);

            mongoAccess.updateData(update.getFinalUpdate(), ACCESSION_REGISTER_SUMMARY);
        } catch (final Exception e) {
            throw new ReferentialException("Unknown error", e);
        }
    }

    public void migrateAccessionRegister(AccessionRegisterDetailModel accessionRegister,
        List<String> fieldsToUpdate)
        throws ReferentialException, BadRequestException {
        try {
            List<Action> actions = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(accessionRegister.getComment()) &&
                fieldsToUpdate.contains(AccessionRegisterDetail.COMMENT)) {
                actions.add(new SetAction(AccessionRegisterDetail.COMMENT, accessionRegister.getComment()));
            }

            if (fieldsToUpdate.contains(AccessionRegisterDetail.OB_ID_IN)) {
                actions.add(new SetAction(AccessionRegisterDetail.OB_ID_IN, accessionRegister.getObIdIn()));
            }

            Update update = new Update();
            update.setQuery(QueryHelper.and().add(
                QueryHelper.eq(ORIGINATING_AGENCY, accessionRegister.getOriginatingAgency()),
                QueryHelper.eq(OPI, accessionRegister.getOpi())));

            update.addActions(actions.toArray(Action[]::new));

            // Update document in on mongo & ES
            mongoAccess.updateData(update.getFinalUpdate(), ACCESSION_REGISTER_DETAIL);

            // Update document in Offer
            storeAccessionRegisterDetail(accessionRegister);

        } catch (final InvalidCreateOperationException | SchemaValidationException e) {
            throw new BadRequestException("Error when migrating register detail !", e);
        } catch (final Exception e) {
            throw new ReferentialException("Error when migrating register detail !", e);
        }
    }
}
