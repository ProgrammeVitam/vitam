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
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handler class used to check the ingest contract of SIP. </br>
 */
public class CheckIngestContractActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckIngestContractActionHandler.class);

    // IN RANK
    private static final int SEDA_PARAMETERS_RANK = 0;

    private static final String HANDLER_ID = "CHECK_CONTRACT_INGEST";

    private final AdminManagementClientFactory adminManagementClientFactory;
    private final StorageClientFactory storageClientFactory;

    public CheckIngestContractActionHandler() {
        this(AdminManagementClientFactory.getInstance(), StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public CheckIngestContractActionHandler(
        AdminManagementClientFactory adminManagementClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO ioParam) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        CheckIngestContractStatus status;
        try {
            ObjectNode infoNode = JsonHandler.createObjectNode();
            checkMandatoryIOParameter(ioParam);
            final Map<String, String> mandatoryValueMap = (Map<String, String>) ioParam.getInput(SEDA_PARAMETERS_RANK);
            String contractIdentifier = null;

            if (
                null != mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) &&
                !mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).isEmpty()
            ) {
                contractIdentifier = mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
                infoNode.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, contractIdentifier);
            }

            status = checkIngestContract(contractIdentifier);

            switch (status) {
                case CONTRACT_INACTIVE:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.CONTRACT_INACTIVE.toString());
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTRACT_UNKNOWN:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.CONTRACT_UNKNOWN.toString());
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTRACT_NOT_IN_MANIFEST:
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckIngestContractStatus.CONTRACT_NOT_IN_MANIFEST.toString()
                    );
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Error ingest contract not found in the Manifest");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTEXT_INACTIVE:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.CONTEXT_INACTIVE.toString());
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Context inactive");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTEXT_UNKNOWN:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.CONTEXT_UNKNOWN.toString());
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Context not found");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTEXT_CHECK_ERROR:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.CONTEXT_CHECK_ERROR.toString());
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Loading context error");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case CONTRACT_NOT_IN_CONTEXT:
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckIngestContractStatus.CONTRACT_NOT_IN_CONTEXT.toString()
                    );
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Error ingest contract not found in the Context");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case MANAGEMENT_CONTRACT_UNKNOWN:
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckIngestContractStatus.MANAGEMENT_CONTRACT_UNKNOWN.toString()
                    );
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Management Contract not found");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case MANAGEMENT_CONTRACT_INACTIVE:
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckIngestContractStatus.MANAGEMENT_CONTRACT_INACTIVE.toString()
                    );
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Management Contract inactive");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case MANAGEMENT_CONTRACT_INVALID:
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckIngestContractStatus.MANAGEMENT_CONTRACT_INVALID.toString()
                    );
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Management Contract invalid");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case FATAL:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.FATAL.toString());
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Unexpected technical error");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.FATAL);
                    break;
                case KO:
                    itemStatus.increment(StatusCode.KO);
                    break;
                case OK:
                    itemStatus.increment(StatusCode.OK);
                    break;
            }
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * @param contractIdentifier name contract
     * @return true if contract ok
     */
    private CheckIngestContractStatus checkIngestContract(String contractIdentifier) {
        // Case when no contract on the manifest
        if (!ParametersChecker.isNotEmpty(contractIdentifier)) {
            return CheckIngestContractStatus.CONTRACT_NOT_IN_MANIFEST;
        }

        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            RequestResponse<IngestContractModel> referenceContracts = adminManagementClient.findIngestContractsByID(
                contractIdentifier
            );
            if (referenceContracts.isOk()) {
                List<IngestContractModel> results =
                    ((RequestResponseOK<IngestContractModel>) referenceContracts).getResults();
                if (!results.isEmpty()) {
                    for (IngestContractModel result : results) {
                        ActivationStatus status = result.getStatus();
                        if (
                            ActivationStatus.ACTIVE.equals(status) && result.getIdentifier().equals(contractIdentifier)
                        ) {
                            CheckIngestContractStatus tempStatus = checkIngestContractInTheContext(contractIdentifier);
                            if (
                                CheckIngestContractStatus.OK.equals(tempStatus) &&
                                StringUtils.isNotBlank(result.getManagementContractId())
                            ) {
                                ManagmentContractChecker managementContractChecker = new ManagmentContractChecker(
                                    result.getManagementContractId(),
                                    adminManagementClientFactory,
                                    storageClientFactory
                                );
                                return managementContractChecker.check();
                            } else {
                                return tempStatus;
                            }
                        } else {
                            return CheckIngestContractStatus.CONTRACT_INACTIVE;
                        }
                    }
                }
            }
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return CheckIngestContractStatus.FATAL;
        } catch (ReferentialNotFoundException e) {
            // Case when the manifest's contract is not in the database of contracts
            LOGGER.error("Contract not found :", e);
            return CheckIngestContractStatus.CONTRACT_UNKNOWN;
        }

        return CheckIngestContractStatus.KO;
    }

    /**
     * @param ingestContract
     */
    private CheckIngestContractStatus checkIngestContractInTheContext(String ingestContract) {
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            final String contextId = VitamThreadUtils.getVitamSession().getContextId();

            if (null == contextId || contextId.isEmpty()) {
                return CheckIngestContractStatus.CONTEXT_UNKNOWN;
            }
            RequestResponse<ContextModel> contextResponse = adminManagementClient.findContextById(contextId);

            if (contextResponse.isOk()) {
                List<ContextModel> results = ((RequestResponseOK<ContextModel>) contextResponse).getResults();
                if (results.isEmpty()) {
                    LOGGER.error("CheckContract : The context " + contextId + "  not found in database");
                    return CheckIngestContractStatus.CONTEXT_UNKNOWN;
                } else {
                    final ContextModel context = results.get(0);

                    // Do not validate contract by the context if enable control is False
                    if (Boolean.FALSE.equals(context.isEnablecontrol())) {
                        return CheckIngestContractStatus.OK;
                    }
                    if (!ContextStatus.ACTIVE.equals(context.getStatus())) {
                        LOGGER.error("CheckContract : The context " + contextId + "  is not activated");
                        return CheckIngestContractStatus.CONTEXT_INACTIVE;
                    }

                    Integer tenant = ParameterHelper.getTenantParameter();

                    long count = context
                        .getPermissions()
                        .stream()
                        .filter(p -> Objects.equals(p.getTenant(), tenant))
                        .filter(p -> p.getIngestContract() != null)
                        .filter(p -> p.getIngestContract().contains(ingestContract))
                        .count();
                    if (count > 0) {
                        return CheckIngestContractStatus.OK;
                    } else {
                        return CheckIngestContractStatus.CONTRACT_NOT_IN_CONTEXT;
                    }
                }
            } else {
                LOGGER.error("Context check error : " + contextResponse.toString());
                return CheckIngestContractStatus.CONTEXT_CHECK_ERROR;
            }
        } catch (ReferentialNotFoundException e) {
            LOGGER.error("Context not found :", e);
            return CheckIngestContractStatus.CONTEXT_UNKNOWN;
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error("Context check error :", e);
            return CheckIngestContractStatus.FATAL;
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}

    /**
     * Check ingest contract status values
     */
    public enum CheckIngestContractStatus {
        /**
         * Ingest constract not present in the Manifest.
         */
        CONTRACT_NOT_IN_MANIFEST,
        /**
         * IngestContract not present in the context
         */
        CONTRACT_NOT_IN_CONTEXT,
        /**
         * Context not found
         */
        CONTEXT_UNKNOWN,
        /**
         * Context inactive
         */
        CONTEXT_INACTIVE,
        /**
         * Error when checking context
         */
        CONTEXT_CHECK_ERROR,
        /**
         * Missing contract: not exists in the database
         */
        CONTRACT_UNKNOWN,
        /**
         * Existing but inactive contract
         */
        CONTRACT_INACTIVE,
        /**
         * Management Contract not found
         */
        MANAGEMENT_CONTRACT_UNKNOWN,
        /**
         * Management Contract existing but inactive
         */
        MANAGEMENT_CONTRACT_INACTIVE,
        /**
         * Management Contract invalid
         */
        MANAGEMENT_CONTRACT_INVALID,
        /**
         * OK contract
         */
        OK,
        /**
         * Other error situation
         */
        KO,
        /**
         * Fatal when getting referential
         */
        FATAL,
    }
}
