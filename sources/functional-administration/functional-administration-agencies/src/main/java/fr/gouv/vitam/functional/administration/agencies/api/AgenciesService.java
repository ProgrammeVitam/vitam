/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.agencies.api;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Agencies;
import org.bson.conversions.Bson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class AgenciesService implements VitamAutoCloseable {

    private static final String AGENCIES_IMPORT_EVENT = "STP_IMPORT_AGENCIES";
    private static final String AGENCIES_UPDATE_EVENT = "STP_UPDATE_AGENCIES";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesService.class);

    private static final String AGENCIES_IS_MANDATORY_PATAMETER = "agency parameter is mandatory";
    private static final String IDENTIFIER = "Identifier";
    private static final String UPDATE_AGENCIES_MANDATORY_PATAMETER = "agency is mandatory";
    private static final String TMP = "tmpAgencies";
    private static final String CSV = "csv";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logBookclient;


    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public AgenciesService(MongoDbAccessAdminImpl mongoAccess) {
        this.mongoAccess = mongoAccess;
        logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }



    public RequestResponse<AgenciesModel> importAgencies(InputStream stream) throws VitamException, IOException {
        List<AgenciesModel> agenciesModelList = AgenciesParser.readFromCsv(stream);
        if (agenciesModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        final AgenciesManager manager = new AgenciesManager(logBookclient, mongoAccess);
        manager.logStarted();

        ArrayNode agenciesNodeToPersist = JsonHandler.createArrayNode();
        final VitamError error = new VitamError(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {
            for (final AgenciesModel agency : agenciesModelList) {
                // if a agency have an id
                if (agency.getId() != null) {
                    error.addToErrors(new VitamError(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
                        .setMessage(
                            AgenciesValidator.AgenciesRejectionCause.rejectIdNotAllowedInCreate(agency.getName())
                                .getReason()));
                    continue;
                }
                // validate agency
                if (manager.validateAgencies(agency, error)) {

                    agency.setId(GUIDFactory.newAgencyGUID().getId());
                    agency.setTenant(ParameterHelper.getTenantParameter());
                    final JsonNode agencyNode = JsonHandler.toJsonNode(agency);

                    agenciesNodeToPersist.add(agencyNode);
                    Optional<AgenciesValidator.AgenciesRejectionCause> result;
                    result = AgenciesManager.checkDuplicateInIdentifier().validate(agency);
                    result.ifPresent(t -> error.addToErrors(
                        new VitamError(VitamCode.AGENCIES_VALIDATION_ERROR.getItem()).setMessage(t.getReason())));
                }
            }

            if ((error.getErrors() != null) && !error.getErrors().isEmpty()) {

                final String errorsDetails;
                errorsDetails = error.getErrors().stream().map(VitamError::getMessage).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, AGENCIES_IMPORT_EVENT);

                return error;
            }
            mongoAccess.insertDocuments(agenciesNodeToPersist, FunctionalAdminCollections.AGENCIES).close();

        } catch (final Exception exp) {
            final String err = "Import agency error > " + exp.getMessage();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();
        return new RequestResponseOK<AgenciesModel>().addAllResults(agenciesModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    public DbRequestResult findAgencies(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        return mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.AGENCIES);
    }

    public AgenciesModel findOneAgencyById(String id) throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(id);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        try (DbRequestResult result =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), FunctionalAdminCollections.AGENCIES)) {
            final List<AgenciesModel> list = result.getDocuments(Agencies.class, AgenciesModel.class);
            if (list.isEmpty()) {
                throw new ReferentialException("Agency not found");
            }
            return list.get(0);
        }
    }

    public RequestResponse<AgenciesModel> updateAgency(String id, JsonNode queryDsl)
        throws VitamException {
        ParametersChecker.checkParameter(UPDATE_AGENCIES_MANDATORY_PATAMETER, queryDsl);
        SanityChecker.checkJsonAll(queryDsl);
        final VitamError error = new VitamError(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.NOT_IMPLEMENTED.getStatusCode());

        return new RequestResponseOK<>();

    }


    /**
     * Agency validator and logBook manager
     */
    protected final static class AgenciesManager {

        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;
        private final LogbookOperationsClient logBookclient;

        private static List<AgenciesValidator> validators = Arrays.asList(
            createMandatoryParamsValidator());

        public AgenciesManager(LogbookOperationsClient logBookclient,
            MongoDbAccessAdminImpl mongoAccess) {
            this.logBookclient = logBookclient;

        }

        public boolean validateAgencies(AgenciesModel agency, VitamError error) {
            for (final AgenciesValidator validator : validators) {
                final Optional<AgenciesValidator.AgenciesRejectionCause> result = validator.validate(agency);
                if (result.isPresent()) {
                    // there is a validation error on this agency
                    /* agency is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
                        .setMessage(result.get().getReason()));
                    // once a validation error is detected on a agency, jump to next agency
                    return false;
                }
            }
            return true;
        }

        /**
         * log start process
         *
         * @throws VitamException
         */
        private void logStarted() throws VitamException {
            eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.STARTED), eip);
            helper.createDelegate(logbookParameters);
        }

        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.OK), eip);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        /**
         * log update start process
         *
         * @throws VitamException
         */
        private void logUpdateStarted(String id) throws VitamException {
            eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, AGENCIES_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(AGENCIES_UPDATE_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, AGENCIES_UPDATE_EVENT +
                "." + StatusCode.STARTED);
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            helper.createDelegate(logbookParameters);

        }

        /**
         * log update success process
         *
         * @throws VitamException
         */
        private void logUpdateSuccess(String id, String query, String oldValue) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put("oldValue", oldValue);
            msg.put("request", query);
            evDetData.set("Agency", msg);
            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final LogbookOperationParameters logbookParameters =
                LogbookParametersFactory
                    .newLogbookOperationParameters(
                        eip,
                        AGENCIES_UPDATE_EVENT,
                        eip,
                        LogbookTypeProcess.MASTERDATA,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(AGENCIES_UPDATE_EVENT, StatusCode.OK),
                        eip);

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                wellFormedJson);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, AGENCIES_UPDATE_EVENT +
                "." + StatusCode.OK);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        /**
         * log fatal error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logFatalError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.FATAL), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        private void logValidationError(String errorsDetails, String action) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, action, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(action, StatusCode.KO), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));

        }

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("agencyCheck", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (final InvalidParseOperationException e) {
                    // Do nothing
                }
            }
        }

        /**
         *
         * Validate that agency have not a missing mandatory parameter
         *
         * @return
         */
        private static AgenciesValidator createMandatoryParamsValidator() {
            return (agency) -> {
                AgenciesValidator.AgenciesRejectionCause rejection = null;
                if (agency.getName() == null || agency.getName().trim().isEmpty()) {
                    rejection =
                        AgenciesValidator.AgenciesRejectionCause.rejectMandatoryMissing(Agencies.NAME);
                }
                if (agency.getIdentifier() == null || agency.getIdentifier().trim().isEmpty()) {
                    rejection =
                        AgenciesValidator.AgenciesRejectionCause.rejectMandatoryMissing(Agencies.IDENTIFIER);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the Id of the  agency  already exists in database
         *
         * @return
         */
        private static AgenciesValidator checkDuplicateInIdentifier() {
            return (agency) -> {
                if (agency.getIdentifier() == null || agency.getIdentifier().isEmpty()) {
                    return Optional.of(AgenciesValidator.AgenciesRejectionCause.rejectMandatoryMissing(
                        Agencies.IDENTIFIER));
                }
                AgenciesValidator.AgenciesRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), eq(Agencies.IDENTIFIER, agency.getIdentifier()));
                final boolean exist = FunctionalAdminCollections.AGENCIES.getCollection().count(clause) > 0;
                if (exist) {
                    rejection =
                        AgenciesValidator.AgenciesRejectionCause.rejectDuplicatedInDatabase(agency.getIdentifier());
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }
    }

    @Override
    public void close() {
        logBookclient.close();
    }
}
