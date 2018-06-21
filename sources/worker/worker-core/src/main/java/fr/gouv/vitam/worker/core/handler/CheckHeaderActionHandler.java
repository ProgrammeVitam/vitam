/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Check HEADER Handler
 */
public class CheckHeaderActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckHeaderActionHandler.class);
    private static final String HANDLER_ID = "CHECK_HEADER";
    private static final int CHECK_CONTRACT_RANK = 0;
    private static final int CHECK_ORIGINATING_AGENCY_RANK = 1;
    private static final String EV_DETAIL_REQ = "EvDetailReq";
    private static final int CHECK_PROFILE_RANK = 2;
    private static final int GLOBAL_MANDATORY_SEDA_PARAMS_OUT_RANK = 0;
    public static final String INGEST_CONTRACT = "ingestContract";


    /**
     * empty Constructor
     */
    public CheckHeaderActionHandler() {
        // empty constructor
    }


    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        final SedaUtils sedaUtils = SedaUtilsFactory.create(handlerIO);
        Map<String, Object> mandatoryValueMap;
        ObjectNode infoNode = JsonHandler.createObjectNode();
        final boolean shouldCheckContract = Boolean.valueOf((String) handlerIO.getInput(CHECK_CONTRACT_RANK));
        final boolean shouldCheckOriginatingAgency =
            Boolean.valueOf((String) handlerIO.getInput(CHECK_ORIGINATING_AGENCY_RANK));
        final boolean shouldCheckProfile =
            Boolean.valueOf((String) handlerIO.getInput(CHECK_PROFILE_RANK));

        try {
            mandatoryValueMap = sedaUtils.getMandatoryValues(params);
        } catch (final ProcessingException e) {
            LOGGER.error("getMandatoryValues ProcessingException", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }


        if (mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
            itemStatus.setData(SedaConstants.TAG_MESSAGE_IDENTIFIER,
                mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER));
            itemStatus.setMasterData(LogbookParameterName.objectIdentifierIncome.name(),
                mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER));
        }

        updateSedaInfo(mandatoryValueMap, infoNode);
        String evDevDetailData = JsonHandler.unprettyPrint(infoNode);
        itemStatus.setEvDetailData(evDevDetailData);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evDevDetailData);

        if (shouldCheckOriginatingAgency) {
            handlerIO.getInput().clear();
            handlerIO.getInput().add(mandatoryValueMap);
            CheckOriginatingAgencyHandler checkOriginatingAgencyHandler = new CheckOriginatingAgencyHandler();
            final ItemStatus checkOriginatingAgencyStatus = checkOriginatingAgencyHandler.execute(params, handlerIO);
            itemStatus.setItemsStatus(CheckOriginatingAgencyHandler.getId(), checkOriginatingAgencyStatus);
            checkOriginatingAgencyHandler.close();
            if (checkOriginatingAgencyStatus.shallStop(true)) {
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        }

        if (mandatoryValueMap.get(SedaConstants.TAG_COMMENT) != null) {
            itemStatus.setMasterData(LogbookParameterName.objectIdentifierIncome.name(),
                mandatoryValueMap.get(SedaConstants.TAG_COMMENT));
        }

        if (shouldCheckContract) {
            handlerIO.getInput().clear();
            handlerIO.getInput().add(mandatoryValueMap);
            CheckIngestContractActionHandler checkIngestContractActionHandler = new CheckIngestContractActionHandler();
            final ItemStatus checkContratItemStatus = checkIngestContractActionHandler.execute(params, handlerIO);
            itemStatus.setItemsStatus(CheckIngestContractActionHandler.getId(), checkContratItemStatus);
            checkIngestContractActionHandler.close();
            if (checkContratItemStatus.shallStop(true)) {
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        }

        String contractIdentifier = (String) mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
        String profileIdentifier = (String) mandatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE);

        IngestContractModel ingestContractModel = null;
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<IngestContractModel> referenceContracts = adminClient.findIngestContractsByID(contractIdentifier);
            if (referenceContracts.isOk()) {
                List<IngestContractModel> results =
                        ((RequestResponseOK<IngestContractModel>) referenceContracts).getResults();
                if (null != results && results.size() > 0) {
                    ingestContractModel = results.iterator().next();
                }
            } else {
                ObjectNode evDetailData = JsonHandler.createObjectNode();
                evDetailData.put(INGEST_CONTRACT, contractIdentifier);
                itemStatus.setEvDetailData(evDetailData.toString());
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            ObjectNode evDetailData = JsonHandler.createObjectNode();
            evDetailData.put(INGEST_CONTRACT, contractIdentifier);
            itemStatus.setEvDetailData(evDetailData.toString());
            itemStatus.increment(StatusCode.KO);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        } catch (InvalidParseOperationException | AdminManagementClientServerException e) {
           LOGGER.error(e);
            ObjectNode evDetailData = JsonHandler.createObjectNode();
            evDetailData.put(INGEST_CONTRACT, contractIdentifier);
            itemStatus.setEvDetailData(evDetailData.toString());
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        try {
            writeIngestContractToWorkspace(handlerIO, ingestContractModel);
        } catch (ProcessingException | InvalidParseOperationException e) {
            LOGGER.error("Error when saving mandatory Seda parameter to Workspace ", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        boolean doNotCheckProfile = (null == contractIdentifier && null == profileIdentifier);
        if (shouldCheckProfile && !doNotCheckProfile) {
            handlerIO.getInput().clear();
            handlerIO.getInput().add(profileIdentifier);
            handlerIO.getInput().add(contractIdentifier);

            boolean checkRelationBetweenProfileAndContract = true;
            if (null == profileIdentifier) {
                // Verify if contract have archive profiles, else do nothing
                if (ingestContractModel.getArchiveProfiles().isEmpty()) {
                    checkRelationBetweenProfileAndContract = false;
                } else {
                    handlerIO.getInput().add(ingestContractModel);
                }
            }

            if (checkRelationBetweenProfileAndContract) {
                CheckArchiveProfileRelationActionHandler checkProfileRelation =
                    new CheckArchiveProfileRelationActionHandler();
                final ItemStatus checkProfilRelationItemStatus = checkProfileRelation.execute(params, handlerIO);
                itemStatus.setItemsStatus(CheckArchiveProfileRelationActionHandler.getId(),
                    checkProfilRelationItemStatus);
                checkProfileRelation.close();
                if (checkProfilRelationItemStatus.shallStop(true)) {
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }
            }

            handlerIO.getInput().clear();
            if (ParametersChecker.isNotEmpty(profileIdentifier)) {
                handlerIO.getInput().add(mandatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE));
                CheckArchiveProfileActionHandler checkArchiveProfile = new CheckArchiveProfileActionHandler();
                final ItemStatus checkProfilItemStatus = checkArchiveProfile.execute(params, handlerIO);
                itemStatus.setItemsStatus(CheckArchiveProfileActionHandler.getId(), checkProfilItemStatus);
                checkArchiveProfile.close();
                if (checkProfilItemStatus.shallStop(true)) {
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }
            }
        } else {
            itemStatus.increment(StatusCode.OK);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void writeIngestContractToWorkspace(HandlerIO handlerIO, IngestContractModel ingestContractModel) throws InvalidParseOperationException, ProcessingException {

        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(GLOBAL_MANDATORY_SEDA_PARAMS_OUT_RANK).getPath());
        // create json file
        JsonHandler.writeAsFile(ingestContractModel, tempFile);
        // put file in workspace
        handlerIO.addOutputResult(GLOBAL_MANDATORY_SEDA_PARAMS_OUT_RANK, tempFile, true, false);
    }

    private void updateSedaInfo(Map<String, Object> madatoryValueMap, ObjectNode infoNode) {

        if (madatoryValueMap.get(SedaConstants.TAG_COMMENT) != null) {
            infoNode.put(EV_DETAIL_REQ, (String) madatoryValueMap.get(SedaConstants.TAG_COMMENT));
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null) {
            final String contractName = (String) madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
            infoNode.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, contractName);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {
            final String profileName = (String) madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE);
            infoNode.put(SedaConstants.TAG_ARCHIVE_PROFILE, profileName);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ACQUISITIONINFORMATION) != null) {
            final String  acquisitionInformation = (String) madatoryValueMap.get(SedaConstants.TAG_ACQUISITIONINFORMATION);
            infoNode.put(SedaConstants.TAG_ACQUISITIONINFORMATION, acquisitionInformation);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_LEGALSTATUS) != null) {
            final String legalStatus = (String) madatoryValueMap.get(SedaConstants.TAG_LEGALSTATUS);
            infoNode.put(SedaConstants.TAG_LEGALSTATUS, legalStatus);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // do nothing
    }

}
