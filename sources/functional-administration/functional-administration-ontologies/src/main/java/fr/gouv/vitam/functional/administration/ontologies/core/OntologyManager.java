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

package fr.gouv.vitam.functional.administration.ontologies.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import org.bson.conversions.Bson;

/**
 * This class manage validation and log operation of Ontology service
 */
public class OntologyManager {

    private static final String ONTOLOGY_SERVICE_ERROR = "Ontology service Error";
    private static final String FUNCTIONAL_MODULE_ONTOLOGY = "FunctionalModule-Ontology";
    private static final String COLLECTION_NAME = "Ontology";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyManager.class);

    private List<OntologyValidator> validators;

    private List<OntologyValidator> externalValidators;

    private final GUID eip;

    private LogbookOperationsClient logbookClient;
    private final MetaDataClient metaDataClient;

    public OntologyManager(LogbookOperationsClient logbookClient, MetaDataClient metaDataClient, GUID eip) {
        this.logbookClient = logbookClient;
        this.metaDataClient = metaDataClient;
        this.eip = eip;
        externalValidators = Arrays.asList(
            createMandatoryParamsValidator(),
            createWrongFieldFormatValidator(),
            createCheckIdentifierValidator(),
            createCheckDuplicateInDatabaseValidator());
        validators = Arrays.asList(
            createMandatoryParamsValidator(),
            createWrongFieldFormatValidator(),
            createCheckDuplicateInDatabaseValidator());
    }



    private boolean validate(OntologyModel ontology, VitamError error, List<OntologyValidator> validators) {

        for (OntologyValidator validator : validators) {
            Optional<OntologyValidator.RejectionCause> result = validator.validate(ontology);
            if (result.isPresent()) {
                // there is a validation error on this ontology.
                error.addToErrors(getVitamError(result.get().getReason()));
                // once a validation error is detected on a ontology, jump to next ontology
                return false;
            }
        }
        return true;
    }

    public boolean validateInternalOntology(OntologyModel ontology, VitamError error) {
        return validate(ontology, error, validators);
    }

    public boolean validateExternalOntology(OntologyModel ontology, VitamError error) {
        return validate(ontology, error, externalValidators);
    }

    private VitamError getVitamError(String error) {
        return new VitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem())
            .setMessage(ONTOLOGY_SERVICE_ERROR)
            .setState("ko").setContext(FUNCTIONAL_MODULE_ONTOLOGY).setDescription(error);
    }

    /**
     * Log validation error (business error)
     *
     * @param errorsDetails
     */
    public void logValidationError(String eventType, String objectId, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipId, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO), eip);
        logbookMessageError(objectId, errorsDetails, logbookParameters);

        logbookClient.update(logbookParameters);
    }

    private void logbookMessageError(String objectId, String errorsDetails,
        LogbookOperationParameters logbookParameters) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("ontologyCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                // Do nothing
            }
        }
        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }
    }

    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails
     * @throws VitamException
     */
    public void logFatalError(String eventType, String objectId, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipId, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.FATAL,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL), eip);

        logbookMessageError(objectId, errorsDetails, logbookParameters);

        logbookClient.update(logbookParameters);
    }

    /**
     * log start process
     *
     * @throws VitamException
     */
    public void logStarted(String eventType, String objectId) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED), eip);

        logbookMessageError(objectId, null, logbookParameters);
        logbookClient.create(logbookParameters);

    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logSuccess(String eventType, String objectId, String message) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipId, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK), eip);

        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }

        if (null != message && !message.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, message);
        }

        logbookClient.update(logbookParameters);
    }

    /**
     * Validate that the ontology has not a missing mandatory parameter
     *
     * @return
     */
    public OntologyValidator createMandatoryParamsValidator() {
        return (ontology) -> {
            OntologyValidator.RejectionCause rejection = null;
            if (ontology.getIdentifier() == null || ontology.getIdentifier().isEmpty()) {
                rejection = OntologyValidator.RejectionCause.rejectMandatoryMissing(Ontology.IDENTIFIER);
            } else if (ontology.getType() == null) {
                rejection = OntologyValidator.RejectionCause.rejectMandatoryMissing(Ontology.TYPE);
            } else if (ontology.getOrigin() == null) {
                rejection = OntologyValidator.RejectionCause.rejectMandatoryMissing(Ontology.ORIGIN);
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }

    /**
     * Set a default value if null and check for wrong data type/format/value for fields
     *
     * @return the validator with thrown errors
     */
    public OntologyValidator createWrongFieldFormatValidator() {
        return (ontology) -> {
            OntologyValidator.RejectionCause rejection = null;

            String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            try {
                if (ontology.getCreationdate() == null ||
                    ontology.getCreationdate().trim().isEmpty()) {
                    ontology.setCreationdate(now);
                } else {
                    ontology.setCreationdate(LocalDateUtil.getFormattedDateForMongo(ontology.getCreationdate()));
                }

            } catch (Exception e) {
                LOGGER.error("Error ontology parse dates", e);
                rejection = OntologyValidator.RejectionCause.rejectMandatoryMissing("CreationDate");
            }

            ontology.setLastupdate(now);

            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }



    /**
     * Check if the ontology identifier already exists in DB or is equal to a sedafield in DB
     *
     * @return
     */
    public OntologyValidator createCheckDuplicateInDatabaseValidator() {

        return (ontology) -> {
            if (ParametersChecker.isNotEmpty(ontology.getIdentifier())) {
                int tenant = ParameterHelper.getTenantParameter();
                Bson clause = and(eq(VitamDocument.TENANT_ID, tenant),
                    or(regex(Ontology.IDENTIFIER, "^(?i)" + Pattern.quote(ontology.getIdentifier())),
                        regex(Ontology.SEDAFIELD, "^(?i)" + Pattern.quote(ontology.getIdentifier()))));
                boolean exist = FunctionalAdminCollections.ONTOLOGY.getCollection().count(clause) > 0;
                if (exist) {
                    return Optional.of(OntologyValidator.RejectionCause.rejectDuplicatedInDatabase(ontology.getIdentifier()));
                }
            }
            return Optional.empty();

        };
    }

    /**
     * Check if the ontology identifier against un regular expression
     * For an identifier to be valid, it must not contain a white space, nor begin by "_" or "#"
     *
     * @return
     */
    public OntologyValidator createCheckIdentifierValidator() {

        return (ontology) -> {
            if (ParametersChecker.isNotEmpty(ontology.getIdentifier())) {
                //
                final Pattern compiledPattern = Pattern.compile("^[_#]|\\s");

                final Matcher matcher = compiledPattern.matcher(ontology.getIdentifier().trim());
                if (matcher.find()) {
                    return Optional.of(OntologyValidator.RejectionCause.rejectInvalidIdentifier(ontology.getIdentifier()));
                }
            }
            return Optional.empty();

        };
    }



}

