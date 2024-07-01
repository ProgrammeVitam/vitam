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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Accession Register Handler
 */
public class IngestAccessionRegisterActionHandler extends AbstractAccessionRegisterAction {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        IngestAccessionRegisterActionHandler.class
    );
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private static final int SEDA_PARAMETERS_RANK = 0;

    /**
     * Empty Constructor AccessionRegisterActionHandler
     */
    public IngestAccessionRegisterActionHandler() {
        this(MetaDataClientFactory.getInstance(), AdminManagementClientFactory.getInstance());
    }

    IngestAccessionRegisterActionHandler(
        MetaDataClientFactory metaDataClientFactory,
        AdminManagementClientFactory adminManagementClientFactory
    ) {
        super(metaDataClientFactory, adminManagementClientFactory);
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("AbstractAccessionRegisterAction running ...");
        return super.execute(params, handler);
    }

    @Override
    protected void prepareAccessionRegisterInformation(
        WorkerParameters params,
        HandlerIO handler,
        AccessionRegisterInfo accessionRegisterInfo
    ) throws ProcessingException, InvalidParseOperationException {
        checkMandatoryIOParameter(handler);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Params: " + params);
        }

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handler.getInput(SEDA_PARAMETERS_RANK)).get(
            SedaConstants.TAG_ARCHIVE_TRANSFER
        );

        if (sedaParameters == null) {
            throw new ProcessingException("No ArchiveTransfer found");
        }

        final JsonNode dataObjectNode = sedaParameters.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
        if (dataObjectNode != null) {
            final JsonNode nodeSubmission = dataObjectNode.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER);
            if (nodeSubmission != null && !Strings.isNullOrEmpty(nodeSubmission.asText())) {
                accessionRegisterInfo.setSubmissionAgency(nodeSubmission.asText());
            }

            final JsonNode nodeOrigin = dataObjectNode.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
            if (nodeOrigin != null && !Strings.isNullOrEmpty(nodeOrigin.asText())) {
                accessionRegisterInfo.setOriginatingAgency(nodeOrigin.asText());
            } else {
                throw new ProcessingException("No " + SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER + " found");
            }

            final JsonNode nodeAcquisitionInformation = dataObjectNode.get(SedaConstants.TAG_ACQUISITIONINFORMATION);
            if (nodeAcquisitionInformation != null && !Strings.isNullOrEmpty(nodeAcquisitionInformation.asText())) {
                accessionRegisterInfo.setAcquisitionInformation(nodeAcquisitionInformation.asText());
            }

            final JsonNode nodeLegalStatus = dataObjectNode.get(SedaConstants.TAG_LEGALSTATUS);
            if (nodeLegalStatus != null && !Strings.isNullOrEmpty(nodeLegalStatus.asText())) {
                accessionRegisterInfo.setLegalStatus(nodeLegalStatus.asText());
            }

            final JsonNode nodeArchivalProfile = dataObjectNode.get(SedaConstants.TAG_ARCHIVE_PROFILE);
            if (nodeArchivalProfile != null && !Strings.isNullOrEmpty(nodeArchivalProfile.asText())) {
                accessionRegisterInfo.setArchivalProfile(nodeArchivalProfile.asText());
            }
        } else {
            throw new ProcessingException("No DataObjectPackage found");
        }

        final JsonNode archivalArchivalAgreement = sedaParameters.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
        if (archivalArchivalAgreement != null && !Strings.isNullOrEmpty(archivalArchivalAgreement.asText())) {
            accessionRegisterInfo.setArchivalAgreement(archivalArchivalAgreement.asText());
        }

        final JsonNode messageIdentifier = sedaParameters.get(SedaConstants.TAG_MESSAGE_IDENTIFIER);
        if (messageIdentifier != null && !Strings.isNullOrEmpty(messageIdentifier.asText())) {
            accessionRegisterInfo.setMessageIdentifier(messageIdentifier.asText());
        }

        final JsonNode comment = sedaParameters.get(SedaConstants.TAG_COMMENT);
        if (!(comment instanceof NullNode || comment == null)) {
            List<String> resultedComments = new ArrayList<>();
            if (comment instanceof ArrayNode) {
                resultedComments.addAll(JsonHandler.getFromJsonNode(comment, new TypeReference<>() {}));
            } else if (!Strings.isNullOrEmpty(comment.asText())) {
                resultedComments.add(comment.asText());
            }
            accessionRegisterInfo.setComment(resultedComments);
        }
    }

    @Override
    protected String getHandlerId() {
        return HANDLER_ID;
    }

    @Override
    protected LogbookTypeProcess getOperationType() {
        return LogbookTypeProcess.INGEST;
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }
}
