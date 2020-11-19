/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.accession.register.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.IncAction;
import fr.gouv.vitam.common.database.builder.query.action.PushAction;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
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
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.ReferentialAccessionRegisterSummaryUtil;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.FunctionalBackupServiceException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
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

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access configuration
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService) {
        this(dbConfiguration, new FunctionalBackupService(vitamCounterService));
    }

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access configuration
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration,
        FunctionalBackupService functionalBackupService) {
        mongoAccess = dbConfiguration;
        this.functionalBackupService = functionalBackupService;
        this.referentialAccessionRegisterSummaryUtil = new ReferentialAccessionRegisterSummaryUtil();
    }

    /**
     * Insert a list of accession register symbolic.
     *
     * @param accessionRegisterSymbolics to insert
     * @throws ReferentialException
     * @throws SchemaValidationException
     * @throws InvalidParseOperationException
     */
    public void insertAccessionRegisterSymbolic(List<AccessionRegisterSymbolic> accessionRegisterSymbolics)
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
    public List<AccessionRegisterSymbolic> findAccessionRegisterSymbolic(JsonNode queryDsl)
        throws ReferentialException {
        return mongoAccess.findDocuments(queryDsl, ACCESSION_REGISTER_SYMBOLIC)
            .getRequestResponseOK(queryDsl, AccessionRegisterSymbolic.class)
            .getResults();
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
                        FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();
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

        } catch (final InvalidParseOperationException | InvalidCreateOperationException | SchemaValidationException e) {
            throw new BadRequestException("Create register detail error", e);
        }
    }

    private void storeAccessionRegisterDetail(AccessionRegisterDetailModel registerDetail)
        throws ReferentialException {

        try {
            Document docToStorage =
                findAccessionRegisterDetail(registerDetail.getOriginatingAgency(), registerDetail.getOpi());

            // Store Backup copy in storage
            functionalBackupService
                .saveDocument(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, docToStorage);

        } catch (FunctionalBackupServiceException e) {
            throw new ReferentialException("Store backup register detail Error", e);
        }
    }

    /**
     * Add event to an existing accession register detail
     *
     * @param registerDetail
     * @throws ReferentialException
     */
    private void addEventToAccessionRegisterDetail(AccessionRegisterDetailModel registerDetail)
        throws ReferentialException, SchemaValidationException, InvalidCreateOperationException,
        InvalidParseOperationException, BadRequestException, DocumentAlreadyExistsException {

        ParametersChecker.checkParameter("Param mustn't be null", registerDetail);
        ParametersChecker.checkParameter("Register opc mustn't be null", registerDetail.getOpc());
        ParametersChecker.checkParameter("Register opi mustn't be null", registerDetail.getOpi());
        ParametersChecker.checkParameter("Register tenant mustn't be null", registerDetail.getTenant());
        ParametersChecker
            .checkParameter("Register originatingAgency mustn't be null", registerDetail.getOriginatingAgency());

        LOGGER.debug("Update register ID / Originating Agency: {} / {}", registerDetail.getId(),
            registerDetail.getOriginatingAgency());
        // Store accession register detail
        try {

            // We use Mongo query to prevent potential desynchronization between Mongo and ES
            Bson filterQuery = and(eq(AccessionRegisterDetail.ORIGINATING_AGENCY, registerDetail
                    .getOriginatingAgency()),
                eq(AccessionRegisterDetail.OPI, registerDetail.getOpi()),
                eq(AccessionRegisterDetail.EVENTS + "." + RegisterValueEventModel.OPERATION, registerDetail.getOpc()));


            long count =
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments(filterQuery);

            if (count > 0) {
                throw new DocumentAlreadyExistsException(String.format(
                    "Accession register detail for originating agency (%s) and opi (%s) found and already contains the detail (%s)",
                    registerDetail.getOriginatingAgency(), registerDetail.getOpi(), registerDetail.getOpc()));
            }


            List<Action> actions = new ArrayList<>();

            actions.add(new SetAction(AccessionRegisterDetail.LAST_UPDATE, LocalDateUtil.getFormattedDateForMongo(
                LocalDateUtil.now())));

            actions.add(
                new IncAction(AccessionRegisterDetail.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.INGESTED,
                    registerDetail.getTotalObjectsGroups().getIngested()));
            actions
                .add(new IncAction(AccessionRegisterDetail.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.DELETED,
                    registerDetail.getTotalObjectsGroups().getDeleted()));
            actions.add(
                new IncAction(AccessionRegisterDetail.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.REMAINED,
                    registerDetail.getTotalObjectsGroups().getRemained()));

            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_OBJECTS + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getTotalObjects().getIngested()));
            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_OBJECTS + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getTotalObjects().getDeleted()));
            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_OBJECTS + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getTotalObjects().getRemained()));

            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_UNITS + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getTotalUnits().getIngested()));
            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_UNITS + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getTotalUnits().getDeleted()));
            actions.add(new IncAction(AccessionRegisterDetail.TOTAL_UNITS + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getTotalUnits().getRemained()));

            actions.add(new IncAction(AccessionRegisterDetail.OBJECT_SIZE + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getObjectSize().getIngested()));
            actions.add(new IncAction(AccessionRegisterDetail.OBJECT_SIZE + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getObjectSize().getDeleted()));
            actions.add(new IncAction(AccessionRegisterDetail.OBJECT_SIZE + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getObjectSize().getRemained()));

            actions.add(new SetAction(AccessionRegisterDetail.STATUS, registerDetail.getStatus().name() ));

            RegisterValueEventModel registerValueEvent = new RegisterValueEventModel()
                .setOperation(registerDetail.getOpc())
                .setOperationType(registerDetail.getOperationType())
                .setTotalUnits(registerDetail.getTotalUnits().getRemained())
                .setTotalGots(registerDetail.getTotalObjectsGroups().getRemained())
                .setTotalObjects(registerDetail.getTotalObjects().getRemained())
                .setObjectSize(registerDetail.getObjectSize().getRemained())
                .setCreationdate(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));

            actions.add(new PushAction(AccessionRegisterDetail.EVENTS, JsonHandler.toJsonNode(registerValueEvent)));

            Update update = new Update();
            update.setQuery(
                QueryHelper.and().add(QueryHelper.eq(AccessionRegisterDetail.ORIGINATING_AGENCY, registerDetail
                    .getOriginatingAgency()), QueryHelper.eq(AccessionRegisterDetail.OPI, registerDetail.getOpi())));

            update.addActions(actions.toArray(Action[]::new));

            mongoAccess.updateData(update.getFinalUpdate(), ACCESSION_REGISTER_DETAIL);

        } catch (final Exception e) {
            if (e instanceof DocumentAlreadyExistsException) {
                throw e;
            }
            throw new ReferentialException("Create register detail Error", e);
        }
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
        Bson filterQuery = and(eq(AccessionRegisterDetail.ORIGINATING_AGENCY, originatingAgency),
            eq(AccessionRegisterDetail.OPI, opi));
        return FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.<VitamDocument<AccessionRegisterDetail>>getCollection()
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
}
