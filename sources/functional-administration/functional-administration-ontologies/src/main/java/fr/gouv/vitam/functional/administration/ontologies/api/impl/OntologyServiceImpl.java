/**
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
 */


package fr.gouv.vitam.functional.administration.ontologies.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamErrorMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ErrorReportOntologies;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.OntologyErrorCode;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.ontologies.api.OntologyService;
import fr.gouv.vitam.functional.administration.ontologies.core.OntologyManager;
import fr.gouv.vitam.functional.administration.ontologies.core.OntologyValidator;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.bson.Document;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ADDITIONAL_INFORMATION;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.CODE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ERROR;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_DATE_TIME;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_ID;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_TYPE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.JDO_DISPLAY;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.MESSAGE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.OUT_MESSG;

/**
 * The implementation of the Ontology CRUD service
 */
public class OntologyServiceImpl implements OntologyService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyServiceImpl.class);

    public static final Integer ADMIN_TENANT = VitamConfiguration.getAdminTenant();

    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String CTR_SCHEMA = "CTR_SCHEMA";
    private static final String ONTOLOGY_IMPORT_EVENT = "IMPORT_ONTOLOGY";


    private static final String ONTOLOGIES_IS_MANDATORY_PARAMETER = "ontologies parameter is mandatory";

    private static final String ONTOLOGIES_REPORT = "ONTOLOGY_REPORT";

    private static final String DELETED_ONTOLOGIES = "deletedOntologies";
    private static final String UPDATED_ONTOLOGIES = "updatedOntologies";
    private static final String CREATED_ONTOLOGIES = "createdOntologies";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";

    private Map<Integer, List<VitamError>> errorsMap;

    private Map<OntologyType, List<OntologyType>> typeMap;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param vitamCounterService the vitam counter service
     * @param functionalBackupService the functional backup service
     */
    public OntologyServiceImpl(MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService, FunctionalBackupService functionalBackupService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        errorsMap = new HashMap<>();
        initTypeMap();
        this.functionalBackupService = functionalBackupService;
    }

    /**
     * Init the map of autorized type changes
     */
    private void initTypeMap() {
        typeMap = new HashMap<>();
        typeMap.put(OntologyType.TEXT, Arrays.asList(OntologyType.KEYWORD));
        typeMap.put(OntologyType.KEYWORD, Arrays.asList(OntologyType.TEXT));
        typeMap.put(OntologyType.DATE, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
        typeMap.put(OntologyType.LONG, new ArrayList<>());
        typeMap.put(OntologyType.DOUBLE, new ArrayList<>());
        typeMap.put(OntologyType.BOOLEAN, new ArrayList<>());
        typeMap.put(OntologyType.GEO_POINT, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
        typeMap.put(OntologyType.ENUM, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
    }


    @Override
    public RequestResponse<OntologyModel> importOntologies(boolean forceUpdate, List<OntologyModel> ontologyModelList)
        throws VitamException, IOException {

        ParametersChecker.checkParameter(ONTOLOGIES_IS_MANDATORY_PARAMETER, ontologyModelList);

        if (ontologyModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        GUID eip= GUIDReader.getGUID(operationId);

        Map<String, List<ErrorReportOntologies>> errors = new HashMap<>();
        OntologyManager manager = new OntologyManager(logbookClient, eip, errors);
        manager.logStarted(ONTOLOGY_IMPORT_EVENT, null);

        Integer tenant = ParameterHelper.getTenantParameter();

        List<OntologyModel> toDelete = new ArrayList<OntologyModel>();
        List<OntologyModel> toCreate = new ArrayList<OntologyModel>();
        List<OntologyModel> toUpdate = new ArrayList<OntologyModel>();

        final Set<String> ontologyIdentifiers = new HashSet<>();
        ArrayNode ontologiesToCreate;

        try {

            //get the actual ontologies in db
            List<OntologyModel> actualOntologies = this.selectOntologies();

            Map<String, OntologyModel> actualOntologiesMap =
                actualOntologies.stream().collect(Collectors.toMap(OntologyModel::getIdentifier, item -> item));


            Map<String, OntologyModel> importedOntologiesMap = new HashMap<String, OntologyModel>();
            //Check if the ontologies must be created or updated
            for (final OntologyModel ontm : ontologyModelList) {
                // if the ontology in json file already exists in db, it will be updated, otherwise it will be created
                if (actualOntologiesMap.containsKey(ontm.getIdentifier())) {
                    toUpdate.add(ontm);
                } else {
                    toCreate.add(ontm);
                }
                importedOntologiesMap.put(ontm.getIdentifier(), ontm);
            }

            for (final OntologyModel actualTenantOntology : actualOntologies) {
                //if the ontology in db doesnt exist in the json file anymore, it will be deleted
                if (!importedOntologiesMap.containsKey(actualTenantOntology.getIdentifier())) {
                    toDelete.add(actualTenantOntology);
                }

            }


            final VitamError vitamError =
                getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), "Global import ontology error", StatusCode.KO).setHttpCode(
                    Response.Status.BAD_REQUEST.getStatusCode());


            ontologiesToCreate = JsonHandler.createArrayNode();

            for (final OntologyModel ontm : ontologyModelList) {
                boolean create = false;
                boolean update = false;
                //Check if the ontology ontm must be created or updated
                if (toCreate.contains(ontm)) {
                    create = true;
                } else if (toUpdate.contains(ontm)) {
                    update = true;
                }



                //VALIDATION
                // if an ontology with the same identifier or the same sedaField is already treated in ontologyModelList, then mark the current one as duplicated
                if (ParametersChecker.isNotEmpty(ontm.getIdentifier())) {
                    if (ontologyIdentifiers.contains(ontm.getIdentifier().trim().toLowerCase())) {
                        manager.addError(ontm.getIdentifier(),
                            new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_IDENTIFIER_ALREADY_IN_ONTOLOGY, OntologyModel.TAG_IDENTIFIER,
                            "Ontology identifier " + ontm.getIdentifier() + " already exists in the json file", ontm), errors);
                        continue;
                    } else {
                        ontologyIdentifiers.add(ontm.getIdentifier().trim().toLowerCase());
                        if (ontm.getSedaField() != null) {
                            ontologyIdentifiers.add(ontm.getSedaField().trim().toLowerCase());
                        }
                    }
                }

                //ontologies to create validation
                if (create) {

                    if (!ADMIN_TENANT.equals(tenant)) {
                        manager
                            .addError(ontm.getIdentifier(),
                                new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_NOT_AUTHORIZED_FOR_TENANT, OntologyModel.TAG_IDENTIFIER,
                                    "Creation not authorized for tenant ", ontm), errors);
                        continue;
                    }
                    //validate that the id is null at creation
                    if (null != ontm.getId()) {
                        OntologyValidator.RejectionCause cause = OntologyValidator.RejectionCause.rejectIdNotAllowedInCreate(ontm);
                        manager.addError(ontm.getIdentifier(), new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_ID_NOT_ALLOWED_IN_CREATE, cause.getFieldName(),
                            "Ontology identifier " + ontm.getIdentifier() + " already exists in the json file", ontm), errors);
                        continue;
                    }
                    //Validation of external ontology for creation
                    if (OntologyOrigin.EXTERNAL.equals(ontm.getOrigin())) {
                        if (manager.validateCreateExternalOntology(ontm.getIdentifier(), ontm)) {
                            ontm.setId(GUIDFactory.newOntologyGUID(tenant).getId());
                        }
                        // Validation of internal ontology for creation
                    } else if (OntologyOrigin.INTERNAL.equals(ontm.getOrigin())) {
                        if (manager.validateCreateInternalOntology(ontm.getIdentifier(), ontm)) {
                            ontm.setId(GUIDFactory.newOntologyGUID(tenant).getId());
                        }
                    }

                    if (ontm.getTenant() == null) {
                        ontm.setTenant(ParameterHelper.getTenantParameter());
                    }
                    JsonNode ontologyNode = buildOntologyNode(ontm);

                    ontologiesToCreate.add(ontologyNode);

                    //  ontologies to update validation
                } else if (update) {//if the ontology must be update

                    if (!ADMIN_TENANT.equals(tenant)) {
                        manager
                            .addError(ontm.getIdentifier(),
                                new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_NOT_AUTHORIZED_FOR_TENANT, OntologyModel.TAG_IDENTIFIER,
                                    "Update not authorized for tenant ", ontm), errors);
                        continue;
                    }

                    // Validation of external ontology for update
                    if (OntologyOrigin.EXTERNAL.equals(ontm.getOrigin())) {
                        if (manager.validateUpdateExternalOntology(ontm.getIdentifier(), ontm)) {
                            ontm.setId(actualOntologiesMap.get(ontm.getIdentifier()).getId());
                        }
                        // Validation of internal ontology for update
                    } else if (OntologyOrigin.INTERNAL.equals(ontm.getOrigin())) {
                        if (manager.validateUpdateInternalOntology(ontm.getIdentifier(), ontm)) {
                            ontm.setId(actualOntologiesMap.get(ontm.getIdentifier()).getId());
                        }
                    }


                    //type change validation
                    OntologyModel modelInDb = actualOntologiesMap.get(ontm.getIdentifier());
                    OntologyType typeInDb = modelInDb.getType();
                    OntologyType newType = ontm.getType();
                    if (!newType.name().equalsIgnoreCase(typeInDb.toString())) {
                        List<OntologyType> compatibleTypes = typeMap.get(typeInDb);
                        if (!compatibleTypes.contains(newType)) {
                            manager.addError(ontm.getIdentifier(), new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_UPDATE_INVALID_TYPE, OntologyModel.TAG_TYPE,
                                "Change of type from " + typeInDb + " to " + newType + " is not possible", ontm), errors);
                        }
                    }

                    if (ontm.getTenant() == null) {
                        ontm.setTenant(ParameterHelper.getTenantParameter());
                    }
                }
            }
            //ontologies to delete validation
            for (final OntologyModel ontm : toDelete) {
                if (!ADMIN_TENANT.equals(tenant)) {
                    manager
                        .addError(ontm.getIdentifier(),
                            new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_NOT_AUTHORIZED_FOR_TENANT, OntologyModel.TAG_IDENTIFIER,
                                "Delete not authorized for tenant ", ontm), errors);
                    continue;
                }

                if (!forceUpdate && OntologyOrigin.INTERNAL.equals(ontm.getOrigin())) {
                    manager
                        .addError(ontm.getIdentifier(),
                            new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_DELETE_NOT_AUTHORIZED, OntologyModel.TAG_IDENTIFIER,
                                "header \"FORCE_UPDATE\" must be true", ontm), errors);
                    continue;

                }
                manager.validateDeleteOntology(ontm.getIdentifier(), ontm);
            }


            if (!errors.isEmpty()) {
                String errorsDetails = errors.entrySet().stream().map(c -> c.getKey() + " : " + c.getValue().get(0).getMessage())
                    .collect(Collectors.joining(","));
                InputStream errorStream = generateErrorReport(errors, StatusCode.OK, eip);
                backupReport(errorStream, eip);
                manager.logValidationError(ONTOLOGY_IMPORT_EVENT, null, errorsDetails);
                return vitamError;
            }

            //If no errors are found, we create/update / delete the documents
            createOntologies(ontologiesToCreate);
            if (toDelete.size() > 0) {
                for (OntologyModel ontology : toDelete) {
                    deleteOntology(ontology, FunctionalAdminCollections.ONTOLOGY);
                }
            }
            if (toUpdate.size() > 0) {
                for (OntologyModel ontology : toUpdate) {
                    updateOntology(ontology);
                }
            }

            InputStream errorStream = generateReportOK(toCreate, toDelete, toUpdate, eip);
            backupReport(errorStream, eip);

        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            final String err = "Import ontologies schema error > " + e.getMessage();
            // logbook error event

            Map<String, List<ErrorReportOntologies>> exception = new HashMap<>();
            ErrorReportOntologies errorReport = new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_EXCEPTION, "",
                err, null);
            exception.put(err, Arrays.asList(errorReport));
            InputStream errorStream = generateErrorReport(exception, StatusCode.KO, eip);
            backupReport(errorStream, eip);
            manager.logKoError(ONTOLOGY_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(), e.getMessage(),
                StatusCode.KO).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        } catch (Exception e) {
            LOGGER.error(e);
            final String err = new StringBuilder("Import ontologies error : ").append(e.getMessage()).toString();
            Map<String, List<ErrorReportOntologies>> exception = new HashMap<>();
            ErrorReportOntologies errorReport = new ErrorReportOntologies(OntologyErrorCode.STP_IMPORT_ONTOLOGIES_EXCEPTION, "",
                err, null);
            exception.put(err, Arrays.asList(errorReport));
            InputStream errorStream = generateErrorReport(exception, StatusCode.KO, eip);
            backupReport(errorStream, eip);
            manager.logFatalError(ONTOLOGY_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), err, StatusCode.KO).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess(ONTOLOGY_IMPORT_EVENT, null, null);

        return new RequestResponseOK<OntologyModel>().addAllResults(ontologyModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    /**
     * @return a list of OntologyModel
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    private List<OntologyModel> selectOntologies() throws ReferentialException, InvalidParseOperationException {

        final Select select = new Select();
        DbRequestResult result = mongoAccess.findDocuments(select.getFinalSelect(), FunctionalAdminCollections.ONTOLOGY);
        return result.getDocuments(Ontology.class, OntologyModel.class);
    }

    @Override
    public RequestResponseOK<OntologyModel> findOntologiesForCache(JsonNode queryDsl)
            throws ReferentialException, InvalidParseOperationException {
        final RequestResponseOK<OntologyModel> response = new RequestResponseOK<>(queryDsl);

        FindIterable<Document> documents = FunctionalAdminCollections.ONTOLOGY.getCollection().find();
        MongoCursor<Document> it = documents.iterator();
        while (it.hasNext()) {
            response.addResult(JsonHandler.getFromString(JSON.serialize(it.next()), OntologyModel.class));
        }

        return response;
    }

    /**
     * Create the specified ontologies
     *
     * @param ontologiesToCreate
     * @throws ReferentialException
     * @throws SchemaValidationException
     */
    private void createOntologies(ArrayNode ontologiesToCreate)
        throws ReferentialException, SchemaValidationException {
        if (ontologiesToCreate.size() > 0) {
            mongoAccess.insertDocuments(ontologiesToCreate, FunctionalAdminCollections.ONTOLOGY).close();
        }
    }
    
    /**
     * Build a jsonNode from an OntologyModel
     *
     * @param ontm
     * @return a JsonNode
     * @throws InvalidParseOperationException
     */
    private JsonNode buildOntologyNode(OntologyModel ontm) throws InvalidParseOperationException {
        final ObjectNode ontologyNode = (ObjectNode) JsonHandler.toJsonNode(ontm);
        JsonNode jsonNode = ontologyNode.remove(VitamFieldsHelper.id());
        if (jsonNode != null) {
            ontologyNode.set(UND_ID, jsonNode);
        }
        JsonNode hashTenant = ontologyNode.remove(VitamFieldsHelper.tenant());
        if (hashTenant != null) {
            ontologyNode.set(UND_TENANT, hashTenant);
        }
        return ontologyNode;
    }

    @Override
    public RequestResponseOK<OntologyModel> findOntologies(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ONTOLOGY)) {
            return result.getRequestResponseOK(queryDsl, Ontology.class, OntologyModel.class);
        }
    }

    @Override
    public void close() {
        if (null != logbookClient) {
            logbookClient.close();
        }
    }


    private VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "Ontology", statusCode);
    }


    /**
     * Create QueryDsl to update the given ontology
     *
     * @param ontologyModel
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     * @throws SchemaValidationException
     * @throws BadRequestException
     */
    private void updateOntology(OntologyModel ontologyModel)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException,
        SchemaValidationException, BadRequestException {
        // FIXME use bulk create instead like LogbookMongoDbAccessImpl.
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateOntologies = new Update();
        List<SetAction> actions = new ArrayList<SetAction>();
        String description = ontologyModel.getDescription() == null ? "" : ontologyModel.getDescription();
        SetAction setDescription = new SetAction(OntologyModel.TAG_DESCRIPTION, description);
        actions.add(setDescription);
        SetAction setUpdateDate =
            new SetAction(OntologyModel.LAST_UPDATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        actions.add(setUpdateDate);

        String apiField = ontologyModel.getApiField() == null ? "" : ontologyModel.getApiField();
        SetAction setApiValue;
        setApiValue = new SetAction(OntologyModel.TAG_APIFIELD, apiField);
        actions.add(setApiValue);

        String sedaField = ontologyModel.getSedaField() == null ? "" : ontologyModel.getSedaField();
        SetAction setSedaValue;
        setSedaValue = new SetAction(OntologyModel.TAG_SEDAFIELD, sedaField);
        actions.add(setSedaValue);

        SetAction setTypeValue;
        setTypeValue = new SetAction(OntologyModel.TAG_TYPE, ontologyModel.getType().toString());
        actions.add(setTypeValue);

        String shortName = ontologyModel.getShortName() == null ? "" : ontologyModel.getShortName();
        SetAction setShortName;
        setShortName = new SetAction(OntologyModel.TAG_SHORT_NAME, shortName);
        actions.add(setShortName);

        List<String> collections = ontologyModel.getCollections() == null ? new ArrayList<String>() : ontologyModel.getCollections();
        SetAction setCollections;
        setCollections = new SetAction(OntologyModel.TAG_COLLECTIONS, collections);
        actions.add(setCollections);

        updateOntologies.setQuery(eq(OntologyModel.TAG_IDENTIFIER, ontologyModel.getIdentifier()));
        updateOntologies.addActions(actions.toArray(new SetAction[actions.size()]));
        updateParser.parse(updateOntologies.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        mongoAccess.updateData(queryDslForUpdate, FunctionalAdminCollections.ONTOLOGY);
    }

    /**
     * Delete an ontology by id
     *
     * @param ontologyModel the ontologyModel to delete
     * @param collection the given FunctionalAdminCollections
     */
    private void deleteOntology(OntologyModel ontologyModel, FunctionalAdminCollections collection) {
        final Delete delete = new Delete();
        DbRequestResult result = null;
        DbRequestSingle dbRequest = new DbRequestSingle(collection.getVitamCollection());
        try {
            delete.setQuery(eq(OntologyModel.TAG_IDENTIFIER, ontologyModel.getIdentifier()));
            result = dbRequest.execute(delete);
            result.close();
        } catch (InvalidParseOperationException | BadRequestException | InvalidCreateOperationException |
            DatabaseException | VitamDBException | SchemaValidationException e) {
            LOGGER.error(e);
        }
    }

    /**
     * generate an error Report
     *
     * @param errors the list of error for generated errors
     * @param status status
     * @param eipMaster eipMaster
     * @return the error report inputStream
     */
    public InputStream generateErrorReport(Map<String, List<ErrorReportOntologies>> errors,
        StatusCode status, GUID eipMaster) {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ObjectNode lineNode = JsonHandler.createObjectNode();
        guidmasterNode.put(EV_TYPE, ONTOLOGY_IMPORT_EVENT);
        guidmasterNode.put(EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        if (eipMaster != null) {
            guidmasterNode.put(EV_ID, eipMaster.toString());
        }
        guidmasterNode.put(OUT_MESSG, VitamErrorMessages.getFromKey(ONTOLOGY_IMPORT_EVENT + "." + status));

        for (String identifier : errors.keySet()) {
            List<ErrorReportOntologies> errorsReports = errors.get(identifier);
            ArrayNode messagesArrayNode = JsonHandler.createArrayNode();
            for (ErrorReportOntologies error : errorsReports) {
                final ObjectNode errorNode = JsonHandler.createObjectNode();
                if (error.getOntologyModel() != null) {
                    errorNode.put(IDENTIFIER, error.getOntologyModel().getIdentifier());
                }
                errorNode.put(CODE, error.getCode() + ".KO");
                errorNode.put(MESSAGE, error.getMessage());
                errorNode.put(ADDITIONAL_INFORMATION, error.getFieldName());
                messagesArrayNode.add(errorNode);
            }
            lineNode.set(identifier, messagesArrayNode);
        }

        reportFinal.set(JDO_DISPLAY, guidmasterNode);
        if (!errors.isEmpty()) {
            reportFinal.set(ERROR, lineNode);
        }

        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    }


    /**
     * generate Ok Report
     *
     * @param createdOntologies the list of created ontologies
     * @param deletedOntologies the list of deleted ontologies
     * @param updatedOntologies the list of updated ontologies
     * @return the error report inputStream
     */
    private InputStream generateReportOK(List<OntologyModel> createdOntologies, List<OntologyModel> deletedOntologies, List<OntologyModel> updatedOntologies, GUID eip) {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ArrayNode deletedArrayNode = JsonHandler.createArrayNode();
        final ArrayNode createdArrayNode = JsonHandler.createArrayNode();
        final ArrayNode updatedArrayNode = JsonHandler.createArrayNode();
        guidmasterNode.put(EV_TYPE, ONTOLOGY_IMPORT_EVENT);
        guidmasterNode.put(EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        guidmasterNode.put(EV_ID, eip.toString());
        guidmasterNode.put(OUT_MESSG,
            ONTOLOGY_IMPORT_EVENT);
        for (OntologyModel ontologyModel : createdOntologies) {
            createdArrayNode.add(ontologyModel.getIdentifier());
        }
        for (OntologyModel ontologyModel : updatedOntologies) {
            updatedArrayNode.add(ontologyModel.getIdentifier());
        }
        for (OntologyModel ontologyModel : deletedOntologies) {
            deletedArrayNode.add(ontologyModel.getIdentifier());
        }
        reportFinal.set(JDO_DISPLAY, guidmasterNode);
        reportFinal.set(DELETED_ONTOLOGIES, deletedArrayNode);
        reportFinal.set(UPDATED_ONTOLOGIES, updatedArrayNode);
        reportFinal.set(CREATED_ONTOLOGIES, createdArrayNode);
        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    }

    /**
     * Save the report stream
     *
     * @param stream
     * @param eip
     * @throws VitamException
     * @throws IOException
     */
    public void backupReport(InputStream stream, GUID eip) throws VitamException, IOException {
        functionalBackupService.saveFile(stream, eip, ONTOLOGIES_REPORT, DataCategory.REPORT,
            eip + ".json");
        stream.close();
    }


}
