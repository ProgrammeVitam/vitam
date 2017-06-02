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
package fr.gouv.vitam.functionnal.administration.context.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
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
import fr.gouv.vitam.functional.administration.client.model.ContextModel;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.context.api.ContextService;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functionnal.administration.context.core.ContextValidator.ContextRejectionCause;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class ContextServiceImpl implements ContextService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextServiceImpl.class);

    private static final String CONTEXT_IS_MANDATORY_PATAMETER = "contexts parameter is mandatory";
    private static final String CONTEXTS_IMPORT_EVENT_START = "STP_IMPORT_CONTEXT.START";
    private static final String CONTEXTS_IMPORT_EVENT_OK = "STP_IMPORT_CONTEXT.OK";
    private static final String CONTEXTS_IMPORT_EVENT_KO = "STP_IMPORT_CONTEXT.KO";
    private final MongoDbAccessAdminImpl mongoAccess;
    private LogbookOperationsClient logBookclient;
    private final VitamCounterService vitamCounterService;

    
    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public ContextServiceImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }
    
    @Override
    public RequestResponse<ContextModel> createContexts(List<ContextModel> contextModelList) throws VitamException{
        ParametersChecker.checkParameter(CONTEXT_IS_MANDATORY_PATAMETER, contextModelList);

        if (contextModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        
        ContextServiceImpl.ContextManager manager = new ContextServiceImpl.ContextManager(logBookclient);
        
        manager.logStarted();
        
        ArrayNode contextsToPersist = null;
        final Set<String> contextNames = new HashSet<>();
        final VitamError error = new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        
        try {            
            for (final ContextModel cm : contextModelList){
                String code = vitamCounterService.getNextSequence(ParameterHelper.getTenantParameter(),"CT");
                cm.setIdentifier(code);
                
                // if a contract have an id
                if (null != cm.getId()) {
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                        ContextRejectionCause.rejectIdNotAllowedInCreate(cm.getName()).getReason()));
                    continue;
                }

                // if a contract with the same name is already treated mark the current one as duplicated
                if (contextNames.contains(cm.getName())) {
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                         ContextRejectionCause.rejectDuplicatedEntry(cm.getName()).getReason()));
                    continue;
                }
                
                // mark the current contract as treated
                contextNames.add(cm.getName());
                
                // validate context
                if (manager.validateContext(cm, error)) {

                    cm.setId(GUIDFactory.newContextGUID().getId());

                    final JsonNode contextNode = JsonHandler.toJsonNode(cm);


                    /* profile is valid, add it to the list to persist */
                    if (contextsToPersist == null) {
                        contextsToPersist = JsonHandler.createArrayNode();
                    }

                    contextsToPersist.add(contextNode);
                }
            }
            
            mongoAccess.insertDocuments(contextsToPersist, FunctionalAdminCollections.CONTEXT);
        } catch (Exception exp) {
            String err = new StringBuilder("Import contexts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        
        manager.logSuccess();
        
        return new RequestResponseOK<ContextModel>().addAllResults(contextModelList).setHits(
            contextModelList.size(), 0, contextModelList.size()).setHttpCode(Response.Status.CREATED.getStatusCode());
    }
    
    /**
     * Context validator and logBook manager
     */
    protected final static class ContextManager {
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;
        private LogbookOperationsClient logBookclient;
        
        private static List<ContextValidator> validators = Arrays.asList(
            createMandatoryParamsValidator(), createCheckDuplicateInDatabaseValidator());

        public ContextManager(LogbookOperationsClient logBookclient) {
            this.logBookclient = logBookclient;
        }
        
        public boolean validateContext(ContextModel context, VitamError error) {
            for (ContextValidator validator : validators) {
                Optional<ContextRejectionCause> result = validator.validate(context);
                if (result.isPresent()) {
                    // there is a validation error on this context
                    /* context is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                        .setMessage(result.get().getReason()));
                    // once a validation error is detected on a context, jump to next context
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
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT_START, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT_START, StatusCode.STARTED), eip);

            helper.createDelegate(logbookParameters);

        }

        /**
         * log end success process
         * 
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT_OK, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT_OK, StatusCode.OK), eip);
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
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT_KO, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT_KO, StatusCode.FATAL), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }
        
        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("contextCheck", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (InvalidParseOperationException e) {
                    //Do nothing
                }
            }
        }
        
        /**
         * Validate that context have not a missing mandatory parameter
         * 
         * @return
         */
        private static ContextValidator createMandatoryParamsValidator() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;
                if (context.getName() == null || context.getName().trim().isEmpty()) {
                    rejection =
                        ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.NAME);
                }

                return (rejection == null) ? Optional.empty() : Optional.of(rejection);
            };
        }


        /**
         * Check if the context the same name already exists in database
         * 
         * @return
         */
        private static ContextValidator createCheckDuplicateInDatabaseValidator() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;
                Bson clause = eq(IngestContract.NAME, context.getName());
                boolean exist = FunctionalAdminCollections.CONTEXT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = ContextValidator.ContextRejectionCause.rejectDuplicatedInDatabase(context.getName());
                }
                return (rejection == null) ? Optional.empty() : Optional.of(rejection);
            };
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
}
