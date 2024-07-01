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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.common.model.administration.ContractsDetailsModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Check HEADER Handler
 */
public class CheckHeaderActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckHeaderActionHandler.class);

    private static final String HANDLER_ID = "CHECK_HEADER";
    private static final String EV_DETAIL_REQ = "EvDetailReq";
    public static final String INGEST_CONTRACT = "ingestContract";
    public static final String MANAGEMENT_CONTRACT = "managementContract";

    private static final int CHECK_ORIGINATING_AGENCY_RANK_INPUT = 0;
    private static final int CHECK_PROFILE_RANK_INPUT = 1;

    private static final int REFERENTIAL_CONTRACTS_RANK_OUTPUT = 0;

    private final AdminManagementClientFactory adminManagementClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final SedaUtilsFactory sedaUtilsFactory;

    public CheckHeaderActionHandler() {
        this(
            AdminManagementClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
            SedaUtilsFactory.getInstance()
        );
    }

    @VisibleForTesting
    public CheckHeaderActionHandler(
        AdminManagementClientFactory adminManagementClientFactory,
        StorageClientFactory storageClientFactory,
        SedaUtilsFactory sedaUtilsFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.sedaUtilsFactory = sedaUtilsFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        Map<String, String> mandatoryValueMap;
        ObjectNode infoNode = JsonHandler.createObjectNode();
        final boolean shouldCheckOriginatingAgency = Boolean.parseBoolean(
            handlerIO.getInput(CHECK_ORIGINATING_AGENCY_RANK_INPUT, String.class)
        );
        final boolean shouldCheckProfile = Boolean.parseBoolean(
            handlerIO.getInput(CHECK_PROFILE_RANK_INPUT, String.class)
        );

        try {
            final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(handlerIO);
            mandatoryValueMap = sedaUtils.getMandatoryValues(params);
        } catch (final ProcessingException e) {
            LOGGER.error("Getting Mandatory Values throws ProcessingException", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        if (mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
            itemStatus.setData(
                SedaConstants.TAG_MESSAGE_IDENTIFIER,
                mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER)
            );
            itemStatus.setMasterData(
                LogbookParameterName.objectIdentifierIncome.name(),
                mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER)
            );
        }

        updateSedaInfo(mandatoryValueMap, infoNode);
        String evDevDetailData = JsonHandler.unprettyPrint(infoNode);
        itemStatus.setEvDetailData(evDevDetailData);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evDevDetailData);

        if (shouldCheckOriginatingAgency) {
            handlerIO.getInput().clear();
            handlerIO.getInput().add(mandatoryValueMap);
            CheckOriginatingAgencyHandler checkOriginatingAgencyHandler = new CheckOriginatingAgencyHandler(
                adminManagementClientFactory
            );
            final ItemStatus checkOriginatingAgencyStatus = checkOriginatingAgencyHandler.execute(params, handlerIO);
            itemStatus.setItemsStatus(CheckOriginatingAgencyHandler.getId(), checkOriginatingAgencyStatus);
            checkOriginatingAgencyHandler.close();
            if (checkOriginatingAgencyStatus.shallStop(true)) {
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        }

        if (mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
            itemStatus.setMasterData(
                LogbookParameterName.objectIdentifierIncome.name(),
                mandatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER)
            );
        }

        handlerIO.getInput().clear();
        handlerIO.getInput().add(mandatoryValueMap);
        CheckIngestContractActionHandler checkIngestContractActionHandler = new CheckIngestContractActionHandler(
            adminManagementClientFactory,
            storageClientFactory
        );
        final ItemStatus checkContratItemStatus = checkIngestContractActionHandler.execute(params, handlerIO);
        itemStatus.setItemsStatus(CheckIngestContractActionHandler.getId(), checkContratItemStatus);
        checkIngestContractActionHandler.close();
        if (checkContratItemStatus.shallStop(true)) {
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        String contractIdentifier = mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
        String profileIdentifier = mandatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE);

        ContractsDetailsModel contractsDetailsModel = new ContractsDetailsModel();
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            RequestResponse<IngestContractModel> referenceContracts = adminClient.findIngestContractsByID(
                contractIdentifier
            );
            if (referenceContracts.isOk()) {
                List<IngestContractModel> results =
                    ((RequestResponseOK<IngestContractModel>) referenceContracts).getResults();
                if (null != results && results.size() > 0) {
                    contractsDetailsModel.setIngestContractModel(results.iterator().next());
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

        if (StringUtils.isNotBlank(contractsDetailsModel.getIngestContractModel().getManagementContractId())) {
            try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
                RequestResponse<ManagementContractModel> managementReferenceContract =
                    adminClient.findManagementContractsByID(
                        contractsDetailsModel.getIngestContractModel().getManagementContractId()
                    );
                if (managementReferenceContract.isOk()) {
                    List<ManagementContractModel> results =
                        ((RequestResponseOK<ManagementContractModel>) managementReferenceContract).getResults();
                    if (null != results && results.size() > 0) {
                        contractsDetailsModel.setManagementContractModel(results.iterator().next());
                    }
                } else {
                    ObjectNode evDetailData = JsonHandler.createObjectNode();
                    evDetailData.put(INGEST_CONTRACT, contractIdentifier);
                    evDetailData.put(
                        MANAGEMENT_CONTRACT,
                        contractsDetailsModel.getIngestContractModel().getManagementContractId()
                    );
                    itemStatus.setEvDetailData(evDetailData.toString());
                    itemStatus.increment(StatusCode.KO);
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                ObjectNode evDetailData = JsonHandler.createObjectNode();
                evDetailData.put(INGEST_CONTRACT, contractIdentifier);
                evDetailData.put(
                    MANAGEMENT_CONTRACT,
                    contractsDetailsModel.getIngestContractModel().getManagementContractId()
                );
                itemStatus.setEvDetailData(evDetailData.toString());
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            } catch (InvalidParseOperationException | AdminManagementClientServerException e) {
                LOGGER.error(e);
                ObjectNode evDetailData = JsonHandler.createObjectNode();
                evDetailData.put(INGEST_CONTRACT, contractIdentifier);
                evDetailData.put(
                    MANAGEMENT_CONTRACT,
                    contractsDetailsModel.getIngestContractModel().getManagementContractId()
                );
                itemStatus.setEvDetailData(evDetailData.toString());
                itemStatus.increment(StatusCode.FATAL);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        }

        try {
            writeIngestContractToWorkspace(handlerIO, contractsDetailsModel);
        } catch (ProcessingException | InvalidParseOperationException e) {
            LOGGER.error("Error when saving mandatory Seda parameter to Workspace ", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        if (shouldCheckProfile) {
            if (!contractsDetailsModel.getIngestContractModel().getArchiveProfiles().isEmpty()) {
                handlerIO.getInput().clear();
                handlerIO.getInput().add(profileIdentifier);
                handlerIO.getInput().add(contractIdentifier);

                CheckArchiveProfileRelationActionHandler checkProfileRelation =
                    new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
                final ItemStatus checkProfilRelationItemStatus = checkProfileRelation.execute(params, handlerIO);
                itemStatus.setItemsStatus(
                    CheckArchiveProfileRelationActionHandler.getId(),
                    checkProfilRelationItemStatus
                );
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

    private void writeIngestContractToWorkspace(
        HandlerIO handlerIO,
        ContractsDetailsModel ingestContractWithDetailsModel
    ) throws InvalidParseOperationException, ProcessingException {
        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(REFERENTIAL_CONTRACTS_RANK_OUTPUT).getPath());
        // create json file
        JsonHandler.writeAsFile(ingestContractWithDetailsModel, tempFile);
        // put file in workspace
        handlerIO.addOutputResult(REFERENTIAL_CONTRACTS_RANK_OUTPUT, tempFile, true, false);
    }

    private void updateSedaInfo(Map<String, String> madatoryValueMap, ObjectNode infoNode) {
        if (madatoryValueMap.get(SedaConstants.TAG_COMMENT) != null) {
            infoNode.put(EV_DETAIL_REQ, madatoryValueMap.get(SedaConstants.TAG_COMMENT));
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null) {
            final String contractName = madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
            infoNode.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, contractName);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {
            final String profileName = madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE);
            infoNode.put(SedaConstants.TAG_ARCHIVE_PROFILE, profileName);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_ACQUISITIONINFORMATION) != null) {
            final String acquisitionInformation = madatoryValueMap.get(SedaConstants.TAG_ACQUISITIONINFORMATION);
            infoNode.put(SedaConstants.TAG_ACQUISITIONINFORMATION, acquisitionInformation);
        }
        if (madatoryValueMap.get(SedaConstants.TAG_LEGALSTATUS) != null) {
            final String legalStatus = madatoryValueMap.get(SedaConstants.TAG_LEGALSTATUS);
            infoNode.put(SedaConstants.TAG_LEGALSTATUS, legalStatus);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // do nothing
    }
}
