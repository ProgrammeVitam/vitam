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
package fr.gouv.vitam.functional.administration.core.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.profile.ProfileValidator.RejectionCause;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import org.bson.conversions.Bson;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * This class manage validation and log operation of profile service
 */
public class ProfileManager {

    public static final String EMPTY_REQUIRED_FIELD = "STP_IMPORT_PROFILE_JSON.EMPTY_REQUIRED_FIELD.KO";
    public static final String WRONG_FIELD_FORMAT = "STP_IMPORT_PROFILE_JSON.TO_BE_DEFINED.KO";
    public static final String DUPLICATE_IN_DATABASE = "STP_IMPORT_PROFILE_JSON.IDENTIFIER_DUPLICATION.KO";
    public static final String PROFILE_NOT_FOUND_IN_DATABASE = "STP_IMPORT_PROFILE_JSON.PROFILE_NOT_FOUND.KO";
    public static final String IMPORT_KO = "STP_IMPORT_PROFILE_JSON.KO";
    public static final String UPDATE_PROFILE_NOT_FOUND = "STP_UPDATE_PROFILE_JSON.PROFILE_NOT_FOUND.KO";
    public static final String UPDATE_VALUE_NOT_IN_ENUM = "STP_UPDATE_PROFILE_JSON.NOT_IN_ENUM.KO";
    public static final String UPDATE_DUPLICATE_IN_DATABASE = "STP_UPDATE_PROFILE_JSON.IDENTIFIER_DUPLICATION.KO";
    public static final String UPDATE_KO = "STP_UPDATE_PROFILE_JSON.KO";
    private static final String PROFILE_SERVICE_ERROR = "Profile service Error";
    private static final String FUNCTIONAL_MODULE_PROFILE = "FunctionalModule-Profile";
    private static final String PARSE_PROFILE_DATE_ERROR = "Error profile parse dates";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProfileManager.class);
    private final Map<ProfileValidator, String> validators = new HashMap<>();

    private final GUID eip;

    private final LogbookOperationsClient logbookClient;

    public ProfileManager(LogbookOperationsClient logbookClient, GUID eip) {
        this.logbookClient = logbookClient;
        this.eip = eip;
        validators.put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
        validators.put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
        validators.put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
    }

    public boolean validateProfile(ProfileModel profile, VitamError error) {
        for (ProfileValidator validator : validators.keySet()) {
            Optional<RejectionCause> result = validator.validate(profile);
            if (result.isPresent()) {
                // there is a validation error on this profile
                /* profile is valid, add it to the list to persist */
                error.addToErrors(
                    getVitamError(result.get().getReason())
                        .setDescription(result.get().getReason())
                        .setMessage(validators.get(validator))
                );
                // once a validation error is detected on a profile, jump to next profile
                return false;
            }
        }
        return true;
    }

    /**
     * Validate if the profile file is valide
     * XSD =&gt; is file xsd is xml valide
     * RNG =&gt; if file rng is xml valide, rng valide, check default values if already exists in vitam
     *
     * @param profileModel
     * @param file
     * @param error
     * @return boolean true/false
     */
    public boolean validateProfileFile(ProfileModel profileModel, File file, VitamError error) throws Exception {
        if (null == profileModel) {
            error.addToErrors(getVitamError("Profile metadata not found for the corresponding inputstream"));
            return false;
        }

        switch (profileModel.getFormat()) {
            case XSD:
                return validateXSD(file, error);
            case RNG:
                return validateRNG(file, error);
            default:
                error.addToErrors(getVitamError("Profile format not supported"));
                return false;
        }
    }

    /**
     * Just check if inputStream is xml valid
     *
     * @param file
     * @param error
     * @return boolean true/false
     */
    public boolean validateXSD(File file, VitamError error) throws Exception {
        // Check xml valid
        try {
            SchemaFactory.newInstance(ValidationXsdUtils.HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1).newSchema(file);
        } catch (SAXException e) {
            LOGGER.error("Malformed profile xsd file", e);
            return false;
        }

        return checkTag(file, "xsd", "schema", error);
    }

    private boolean checkTag(File file, String prefix, String element, VitamError error)
        throws FileNotFoundException, XMLStreamException {
        final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
        final XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(file));
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartDocument()) {
                continue;
            }

            if (event.isStartElement()) {
                String elementName = event.asStartElement().getName().getLocalPart();
                String elementPrefix = event.asStartElement().getName().getPrefix();

                if (Objects.equals(element, elementName) || Objects.equals(prefix, elementPrefix)) {
                    error.addToErrors(getVitamError("Profile file xsd have not the xsd:schema tag name"));
                    return true;
                }
            }
        }
        return false;
    }

    private VitamError getVitamError(String error) {
        return new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem())
            .setMessage(PROFILE_SERVICE_ERROR)
            .setState("ko")
            .setContext(FUNCTIONAL_MODULE_PROFILE)
            .setDescription(error);
    }

    /**
     * TODO
     * 1. Validate if rng is xml valide,
     * 2. Validate if rng is rng valide
     * 3. Validate if data in rng is valide
     *
     * @param file
     * @param error
     * @return boolean true/false
     */
    public boolean validateRNG(File file, VitamError error) throws Exception {
        try {
            System.setProperty(ValidationXsdUtils.RNG_PROPERTY_KEY, ValidationXsdUtils.RNG_FACTORY);
            SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI).newSchema(file);
        } catch (SAXException e) {
            LOGGER.error("Malformed profile rng file", e);
            return false;
        }

        return checkTag(file, "rng", "grammar", error);
    }

    /**
     * Log validation error (business error)
     *
     * @param eventType
     * @param objectId
     * @param errorsDetails
     * @param eventTypeKO
     */
    public void logValidationError(String eventType, String objectId, String errorsDetails, String eventTypeKO)
        throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getFromFullCodeKey(eventTypeKO),
            eip
        );
        logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, eventTypeKO);
        logbookMessageError(objectId, errorsDetails, logbookParameters, eventTypeKO);

        logbookClient.update(logbookParameters);
    }

    private void logbookMessageError(
        String objectId,
        String errorsDetails,
        LogbookOperationParameters logbookParameters
    ) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("profileCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                //Do nothing
            }
        }
        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }
    }

    private void logbookMessageError(
        String objectId,
        String errorsDetails,
        LogbookOperationParameters logbookParameters,
        String eventTypeKO
    ) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                String evDetDataKey;
                switch (eventTypeKO) {
                    case EMPTY_REQUIRED_FIELD:
                        evDetDataKey = "Mandatory Fields";
                        break;
                    case WRONG_FIELD_FORMAT:
                        evDetDataKey = "Incorrect Field and value";
                        break;
                    case DUPLICATE_IN_DATABASE:
                    case UPDATE_DUPLICATE_IN_DATABASE:
                        evDetDataKey = "Duplicate Field";
                        break;
                    case PROFILE_NOT_FOUND_IN_DATABASE:
                    case UPDATE_PROFILE_NOT_FOUND:
                        evDetDataKey = "Profile not found";
                        break;
                    case UPDATE_VALUE_NOT_IN_ENUM:
                        evDetDataKey = "Not in Enum";
                        break;
                    default:
                        evDetDataKey = " profileCheck";
                        break;
                }

                object.put(evDetDataKey, errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                //Do nothing
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
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.FATAL,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL),
            eip
        );

        logbookMessageError(objectId, errorsDetails, logbookParameters);

        logbookClient.update(logbookParameters);
    }

    /**
     * log start process
     *
     * @throws VitamException
     */
    public void logStarted(String eventType, String objectId) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED),
            eip
        );

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
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK),
            eip
        );

        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }

        if (null != message && !message.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, message);
        }

        logbookClient.update(logbookParameters);
    }

    /**
     * Validate that profile have not a missing mandatory parameter
     *
     * @return
     */
    public ProfileValidator createMandatoryParamsValidator() {
        return profile -> {
            List<String> missingParams = new ArrayList<>();

            if (
                profile.getFormat() == null ||
                (!profile.getFormat().equals(ProfileFormat.RNG) && !profile.getFormat().equals(ProfileFormat.XSD))
            ) {
                missingParams.add(Profile.FORMAT);
            }
            if (profile.getName() == null || profile.getName().length() == 0) {
                missingParams.add(Profile.NAME);
            }

            return (missingParams.isEmpty())
                ? Optional.empty()
                : Optional.of(RejectionCause.rejectSeveralMandatoryMissing(missingParams));
        };
    }

    /**
     * Set a default value if null
     *
     * @return
     */
    public ProfileValidator createWrongFieldFormatValidator() {
        return profile -> {
            RejectionCause rejection = null;

            String now = LocalDateUtil.nowFormatted();
            if (profile.getStatus() == null) {
                profile.setStatus(ProfileStatus.INACTIVE);
            }

            if (
                !profile.getStatus().equals(ProfileStatus.ACTIVE) && !profile.getStatus().equals(ProfileStatus.INACTIVE)
            ) {
                LOGGER.error("Error profile status not valide (must be ACTIVE or INACTIVE");
                rejection = RejectionCause.rejectMandatoryMissing(
                    "Status " + profile.getStatus() + " not valide must be ACTIVE or INACTIVE"
                );
            }

            try {
                if (profile.getCreationdate() == null || profile.getCreationdate().trim().isEmpty()) {
                    profile.setCreationdate(now);
                } else {
                    profile.setCreationdate(LocalDateUtil.getFormattedDateTimeForMongo(profile.getCreationdate()));
                }
            } catch (Exception e) {
                LOGGER.error(PARSE_PROFILE_DATE_ERROR, e);
                rejection = RejectionCause.rejectMandatoryMissing("Creationdate");
            }
            try {
                if (profile.getActivationdate() == null || profile.getActivationdate().trim().isEmpty()) {
                    profile.setActivationdate(now);
                } else {
                    profile.setActivationdate(LocalDateUtil.getFormattedDateTimeForMongo(profile.getActivationdate()));
                }
            } catch (Exception e) {
                LOGGER.error(PARSE_PROFILE_DATE_ERROR, e);
                rejection = RejectionCause.rejectMandatoryMissing("ActivationDate");
            }
            try {
                if (profile.getDeactivationdate() == null || profile.getDeactivationdate().trim().isEmpty()) {
                    profile.setDeactivationdate(null);
                } else {
                    profile.setDeactivationdate(
                        LocalDateUtil.getFormattedDateTimeForMongo(profile.getDeactivationdate())
                    );
                }
            } catch (Exception e) {
                LOGGER.error(PARSE_PROFILE_DATE_ERROR, e);
                rejection = RejectionCause.rejectMandatoryMissing("deactivationdate");
            }

            profile.setLastupdate(now);

            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }

    /**
     * Check if the Id of the  contract  already exists in database
     *
     * @return
     */
    public ProfileValidator checkEmptyIdentifierSlaveModeValidator() {
        return profileModel -> {
            if (profileModel.getIdentifier() == null || profileModel.getIdentifier().isEmpty()) {
                return Optional.of(ProfileValidator.RejectionCause.rejectMandatoryMissing(AccessContract.IDENTIFIER));
            }
            return Optional.empty();
        };
    }

    /**
     * Check if the profile identifier already exists in database
     *
     * @return
     */
    public ProfileValidator createCheckDuplicateInDatabaseValidator() {
        return profile -> {
            if (ParametersChecker.isNotEmpty(profile.getIdentifier())) {
                int tenant = ParameterHelper.getTenantParameter();
                Bson clause = and(eq(VitamDocument.TENANT_ID, tenant), eq(Profile.IDENTIFIER, profile.getIdentifier()));
                boolean exist = FunctionalAdminCollections.PROFILE.getCollection().countDocuments(clause) > 0;
                if (exist) {
                    return Optional.of(RejectionCause.rejectDuplicatedInDatabase(profile.getIdentifier()));
                }
            }
            return Optional.empty();
        };
    }
}
