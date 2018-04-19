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

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.ontologies.api.OntologyService;
import fr.gouv.vitam.functional.administration.ontologies.core.OntologyManager;
import fr.gouv.vitam.functional.administration.ontologies.core.OntologyValidator;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

/**
 * The implementation of the Ontology CRUD service
 */
public class OntologyServiceImpl implements OntologyService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyServiceImpl.class);

    public static final Integer ADMIN_TENANT = VitamConfiguration.getAdminTenant();

    private static final String CTR_SCHEMA = "CTR_SCHEMA";
    private static final String ONTOLOGY_IMPORT_EVENT = "IMPORT_ONTOLOGY";
    private static final String ONTOLOGY_BACKUP_EVENT = "BACKUP_ONTOLOGY";
    private static final String ONTOLOGY_UPDATE_EVENT = "UPDATE_ONTOLOGY";

    private static final String FUNCTIONAL_ONTOLOGY = "FunctionalModule-Ontology";

    private static final String ONTOLOGIES_IS_MANDATORY_PARAMETER = "ontologies parameter is mandatory";
    private static final String ONTOLOGY_SERVICE_ERROR = "Ontology service Error";
    private static final String ONTOLOGY_NOT_FOUND = "Ontology to update not found";

    private static final String ONTOLOGY_IDENTIFIER_ALREADY_EXISTS_IN_DATABASE =
        "Ontology already exists in database ";
    private static final String ONTOLOGY_IDENTIFIER_MUST_BE_STRING =
        "Ontology identifier must be a string ";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final MetaDataClient metaDataClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private static final String _TENANT = "_tenant";
    private static final String _ID = "_id";

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
        metaDataClient = MetaDataClientFactory.getInstance().getClient();
        this.functionalBackupService = functionalBackupService;
    }


    @Override
    public RequestResponse<OntologyModel> createOntologies(List<OntologyModel> ontologyModelList)
        throws VitamException {
        ParametersChecker.checkParameter(ONTOLOGIES_IS_MANDATORY_PARAMETER, ontologyModelList);

        if (ontologyModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        boolean slaveMode = vitamCounterService
            .isSlaveFunctionnalCollectionOnTenant(SequenceType.ONTOLOGY_SEQUENCE.getCollection(),
                ParameterHelper.getTenantParameter());

        OntologyManager manager = new OntologyManager(logbookClient, metaDataClient, eip);
        manager.logStarted(ONTOLOGY_IMPORT_EVENT, null);

        final Set<String> ontologyIdentifiers = new HashSet<>();

        ArrayNode ontologiesToPersist;

        Integer tenant = ParameterHelper.getTenantParameter();

        final VitamError error =
            getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), "Global create ontology error", StatusCode.KO).setHttpCode(
                Response.Status.BAD_REQUEST.getStatusCode());

        try {
            ontologiesToPersist = JsonHandler.createArrayNode();

            for (final OntologyModel ontm : ontologyModelList) {

                //validate that the id is null at creation
                if (null != ontm.getId()) {
                    error.addToErrors(getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(),
                        OntologyValidator.RejectionCause.rejectIdNotAllowedInCreate(ontm.getId()).getReason(), StatusCode.KO));
                    continue;
                }
                // if an ontology with the same identifier or the same sedaField is already treated in ontologyModelList, then mark the current one as duplicated
                if (ParametersChecker.isNotEmpty(ontm.getIdentifier())) {
                    if (ontologyIdentifiers.contains(ontm.getIdentifier().trim().toLowerCase())) {
                        error.addToErrors(
                            getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(), "Duplicate ontology (IDENTIFIER) ", StatusCode.KO)
                                .setMessage(
                                    "Ontology identifier " + ontm.getIdentifier() + " already exists in the json file"));
                        continue;
                    } else {
                        ontologyIdentifiers.add(ontm.getIdentifier().trim().toLowerCase());
                        ontologyIdentifiers.add(ontm.getSedaField().trim().toLowerCase());
                    }

                }

                if (OntologyOrigin.EXTERNAL.equals(ontm.getOrigin())) {

                    if (manager.validateExternalOntology(ontm, error)) {
                        ontm.setId(GUIDFactory.newOntologyGUID(tenant).getId());
                    }
                } else if (OntologyOrigin.INTERNAL.equals(ontm.getOrigin())) {
                    if (!ADMIN_TENANT.equals(tenant)) {
                        error.addToErrors(
                            getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(), "INTERNAL origin not authorized for tenant ", StatusCode.KO)
                                .setMessage(
                                    "INTERNAL origin from" + ontm.getIdentifier() + " is not authorized for tenant "));

                    }
                    if (manager.validateInternalOntology(ontm, error)) {
                        ontm.setId(GUIDFactory.newOntologyGUID(tenant).getId());
                    }
                }

                if (ontm.getTenant() == null) {
                    ontm.setTenant(ParameterHelper.getTenantParameter());
                }
                JsonNode ontologyNode = buildOntologyNode(ontm);

                ontologiesToPersist.add(ontologyNode);

            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log +stop
                final String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage() + " : " + c.getDescription())
                        .collect(Collectors.joining(","));
                manager.logValidationError(ONTOLOGY_IMPORT_EVENT, null, errorsDetails);
                return error;
            }
            //If no errors are found, we insert the documents

            mongoAccess.insertDocuments(ontologiesToPersist, FunctionalAdminCollections.ONTOLOGY).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                ONTOLOGY_BACKUP_EVENT,
                FunctionalAdminCollections.ONTOLOGY,
                eip.toString()
            );
        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            final String err = "Import ontologies schema error > " + e.getMessage();
            // logbook error event
            manager.logValidationError(CTR_SCHEMA, null, err);

            return getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(), e.getMessage(),
                StatusCode.KO).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        } catch (final Exception e) {
            final String err = new StringBuilder("Import ontologies error : ").append(e.getMessage()).toString();
            manager.logFatalError(ONTOLOGY_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), err, StatusCode.KO).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess(ONTOLOGY_IMPORT_EVENT, null, null);

        return new RequestResponseOK<OntologyModel>().addAllResults(ontologyModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
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
            ontologyNode.set(_ID, jsonNode);
        }
        JsonNode hashTenant = ontologyNode.remove(VitamFieldsHelper.tenant());
        if (hashTenant != null) {
            ontologyNode.set(_TENANT, hashTenant);
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


    @Override public RequestResponse updateOntology(String id, JsonNode queryDsl) throws VitamException {
        //TODO
        return null;
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
}
