/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.functional.administration.profile.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.profile.core.ProfileValidator.RejectionCause;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import org.bson.conversions.Bson;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

/**
 * This class manage validation and log operation of profile service
 */
public class ProfileManager {

    private static final String PROFILE_SERVICE_ERROR = "Profile service Error";
    private static final String FUNCTIONAL_MODULE_PROFILE = "FunctionalModule-Profile";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProfileManager.class);
    public static final String RNG_GRAMMAR = "rng:grammar";
    public static final String XSD_SCHEMA = "xsd:schema";


    private static List<ProfileValidator> validators = Arrays.asList(
        createMandatoryParamsValidator(),
        createWrongFieldFormatValidator(),
        createCheckDuplicateInDatabaseValidator()
    );

    final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
    private GUID eip = null;

    private LogbookOperationsClient logBookclient;

    public ProfileManager(LogbookOperationsClient logBookclient) {
        this.logBookclient = logBookclient;
    }

    public boolean validateProfile(ProfileModel profile,
        VitamError error) {

        for (ProfileValidator validator : validators) {
            Optional<RejectionCause> result = validator.validate(profile);
            if (result.isPresent()) {
                // there is a validation error on this profile
                    /* profile is valid, add it to the list to persist */
                error.addToErrors(getVitamError(result.get().getReason()));
                // once a validation error is detected on a profile, jump to next profile
                return false;
            }
        }
        return true;
    }


    /**
     * Validate if the profile file is valide
     * XSD => is file xsd is xml valide
     * RNG => if file rng is xml valide, rng valide, check default values if already exists in vitam
     * @param profileModel
     * @param inputStream
     * @param error
     * @return boolean true/false
     */
    public boolean validateProfileFile(ProfileModel profileModel, InputStream inputStream, VitamError error) {

        if (null == profileModel) {
            error.addToErrors(getVitamError("Profile metadata not found for the corresponding inputstream"));
            return false;
        }

        switch (profileModel.getFormat()) {
            case XSD:
                return validateXSD(inputStream, error);
            case RNG:
                return validateRNG(inputStream, error);
            default:
                error.addToErrors(getVitamError("Profile format not supported"));
                return false;
        }

    }

    /**
     * Juste check if inputStream is xml valide
     * @param inputStream
     * @param error
     * @return boolean true/false
     */
    public boolean validateXSD(InputStream inputStream, VitamError error) {

        try {
            // parse an XML document into a DOM tree
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(inputStream);

            final String tagName = document.getDocumentElement().getTagName();
            if (null == tagName || !String.valueOf(XSD_SCHEMA).equals(tagName)) {
                error.addToErrors(getVitamError("Profile file xsd have not the xsd:schema tag name"));
                return false;
            }

            return true;
        } catch (SAXException  | IOException e) {
            error.addToErrors(getVitamError("Profile file xsd is not xml valide >> "+e.getMessage()));
            return false;
        } catch (ParserConfigurationException e) {
            error.addToErrors(getVitamError("Profile file xsd ParserConfigurationException >> "+e.getMessage()));
            return false;
        }
    }

    private VitamError getVitamError(String error) {
        return new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem()).setMessage(PROFILE_SERVICE_ERROR).setState("ko").setContext(FUNCTIONAL_MODULE_PROFILE).setDescription(error);
    }
    /**
     * TODO
     * 1. Validate if rng is xml valide,
     * 2. Validate if rng is rng valide
     * 3. Validate if data in rng is valide
     *
     * @param inputStream
     * @param error
     * @return boolean true/false
     */
    public boolean validateRNG(InputStream inputStream, VitamError error) {

        try {
            // parse an XML document into a DOM tree
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(inputStream);

            final String tagName = document.getDocumentElement().getTagName();
            if (null == tagName || !String.valueOf(RNG_GRAMMAR).equals(tagName)) {
                error.addToErrors(getVitamError("Profile file rng have not the rng:grammar tag name"));
                return false;
            }
            // TODO: 5/12/17 parse rng and validate RG and OriginatingAgencies
            return true;
        } catch (SAXException  | IOException e) {
            error.addToErrors(getVitamError("Profile file rng is not xml valide >> "+e.getMessage()));
            return false;
        } catch (ParserConfigurationException e) {
            error.addToErrors(getVitamError("Profile file rng ParserConfigurationException >> "+e.getMessage()));
            return false;
        }
    }
    /**
     *
     * Log validation error (business error)
     *
     * @param errorsDetails
     */
    public void logValidationError(String eventType, String objectId, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO), eip);
        logbookMessageError(objectId, errorsDetails, logbookParameters);

        helper.updateDelegate(logbookParameters);
        logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));

    }

    private void logbookMessageError(String objectId, String errorsDetails, LogbookOperationParameters logbookParameters) {
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
        if (null != objectId && ! objectId.isEmpty()) {
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
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.FATAL,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL), eip);

        logbookMessageError(objectId, errorsDetails, logbookParameters);

        helper.updateDelegate(logbookParameters);
        logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
    }

    /**
     * log start process
     *
     * @throws VitamException
     */
    public void logStarted(String eventType, String objectId) throws VitamException {
        eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED), eip);

        logbookMessageError(objectId, null, logbookParameters);
        helper.createDelegate(logbookParameters);

    }

    /**
     * log in progress process
     *
     * @throws VitamException
     */
    public void logInProgress(String eventType, String objectId, StatusCode statusCode) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                statusCode,
                VitamLogbookMessages.getCodeOp(eventType, statusCode), eip);

        logbookMessageError(objectId, null, logbookParameters);

        helper.updateDelegate(logbookParameters);
    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logSuccess(String eventType, String objectId, String message) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK), eip);

        if (null != objectId && ! objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }

        if (null != message && ! message.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, message);
        }

        helper.updateDelegate(logbookParameters);
        logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
    }

    /**
     * Validate that profile have not a missing mandatory parameter
     *
     * @return
     */
    private static ProfileValidator createMandatoryParamsValidator() {
        return (profile) -> {
            RejectionCause rejection = null;

            if (profile.getFormat() == null || (!profile.getFormat().equals(ProfileFormat.RNG) && !profile.getFormat().equals(ProfileFormat.XSD))) {
                rejection = RejectionCause.rejectMandatoryMissing(Profile.FORMAT);
            }

            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }

    /**
     * Set a default value if null
     *
     * @return
     */
    private static ProfileValidator createWrongFieldFormatValidator() {
        return (profile) -> {
            RejectionCause rejection = null;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.ofPattern("dd/MM/yyyy");


            String now = LocalDateUtil.now().toString();
            if (profile.getStatus() == null) {
                profile.setStatus(ProfileStatus.INACTIVE);
            }


            if (!profile.getStatus().equals(ProfileStatus.ACTIVE) &&
                !profile.getStatus().equals(ProfileStatus.INACTIVE)) {
                LOGGER.error("Error profile status not valide (must be ACTIVE or INACTIVE");
                rejection =
                    RejectionCause.rejectMandatoryMissing("Status " + profile.getStatus() +
                        " not valide must be ACTIVE or INACTIVE");
            }


            try {
                if (profile.getCreationdate() == null || profile.getCreationdate().trim().isEmpty()) {
                    profile.setCreationdate(now);
                } else {
                    profile.setCreationdate(
                        LocalDate.parse(profile.getCreationdate(), formatter).atStartOfDay().toString());
                }

            } catch (Exception e) {
                LOGGER.error("Error profile parse dates", e);
                rejection = RejectionCause.rejectMandatoryMissing("Creationdate");
            }
            try {
                if (profile.getActivationdate() == null || profile.getActivationdate().trim().isEmpty()) {
                    profile.setActivationdate(now);
                } else {
                    profile.setActivationdate(
                        LocalDate.parse(profile.getActivationdate(), formatter).atStartOfDay().toString());

                }
            } catch (Exception e) {
                LOGGER.error("Error profile parse dates", e);
                rejection = RejectionCause.rejectMandatoryMissing("ActivationDate");
            }
            try {

                if (profile.getDeactivationdate() == null || profile.getDeactivationdate().trim().isEmpty()) {
                    profile.setDeactivationdate(null);
                } else {

                    profile.setDeactivationdate(
                        LocalDate.parse(profile.getDeactivationdate(), formatter).atStartOfDay().toString());
                }
            } catch (Exception e) {
                LOGGER.error("Error profile parse dates", e);
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
    public static ProfileValidator checkDuplicateInIdentifierSlaveModeValidator() {
        return (profileModel) -> {
            if(profileModel.getIdentifier() == null || profileModel.getIdentifier().isEmpty()){
                return    Optional.of( ProfileValidator.RejectionCause.rejectMandatoryMissing(
                    AccessContract.IDENTIFIER));
            }
            RejectionCause rejection = null;
            final int tenant = ParameterHelper.getTenantParameter();
            final Bson clause =
                and(eq(VitamDocument.TENANT_ID, tenant), eq(AccessContract.IDENTIFIER, profileModel.getIdentifier()));
            final boolean exist = FunctionalAdminCollections.PROFILE.getCollection().count(clause) > 0;
            if (exist) {
                rejection = ProfileValidator.RejectionCause.rejectDuplicatedInDatabase(profileModel.getIdentifier());
            }
            return rejection == null ? Optional.empty() : Optional.of(rejection);
        };
    }


    /**
     * Check if the profile the same name or identifier already exists in database
     *
     * @return
     */
    private static ProfileValidator createCheckDuplicateInDatabaseValidator() {
        return (profile) -> {
            RejectionCause rejection = null;
            int tenant = ParameterHelper.getTenantParameter();
            Bson clause = or(and(eq(VitamDocument.TENANT_ID, tenant), eq(Profile.IDENTIFIER, profile.getIdentifier())), and(eq(VitamDocument.TENANT_ID, tenant), eq(Profile.NAME, profile.getName())));
            boolean exist = FunctionalAdminCollections.PROFILE.getCollection().count(clause) > 0;
            if (exist) {
                rejection = RejectionCause.rejectDuplicatedInDatabase(profile.getName());
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);

        };
    }

}

