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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.util.List;

/**
 * Check Archive Profile Relation Handler Verify the relation between ingest contract and profil in manifest
 */
public class CheckArchiveProfileRelationActionHandler extends ActionHandler {

    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";
    private static final String CAN_NOT_GET_THE_INGEST_CONTRACT = "Can_not_get_the_ingest_contract";
    private static final String CAN_NOT_GET_THE_PROFILE = "Can_not_get_the_profile";
    private static final String THE_PROFILE_NOT_FOUND = "Profile_Not_Found";
    private static final String PROFILE_NOT_FOUND = "Profile not found";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        CheckArchiveProfileRelationActionHandler.class
    );
    private static final String HANDLER_ID = "CHECK_IC_AP_RELATION";
    private static final int PROFILE_IDENTIFIER_RANK = 0;
    private static final int CONTRACT_IDENTIFIER_RANK = 1;

    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckArchiveProfileRelationActionHandler() {
        this(AdminManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public CheckArchiveProfileRelationActionHandler(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
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
        String profileIdentifier = (String) handlerIO.getInput(PROFILE_IDENTIFIER_RANK);
        final String ingestContractIdentifier = (String) handlerIO.getInput(CONTRACT_IDENTIFIER_RANK);
        CheckProfileStatus status = null;
        ObjectNode infoNode = JsonHandler.createObjectNode();
        String dataKey = null;
        String dataValue = null;
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            if (ParametersChecker.isNotEmpty(profileIdentifier)) {
                // Check that profile exists and not inactive
                Select select = new Select();
                select.setQuery(QueryHelper.eq(Profile.IDENTIFIER, profileIdentifier));
                RequestResponse<ProfileModel> response = adminClient.findProfiles(select.getFinalSelect());
                if (response.isOk()) {
                    List<ProfileModel> results = ((RequestResponseOK<ProfileModel>) response).getResults();
                    if (null != results && results.size() > 0) {
                        final ProfileModel profile = results.iterator().next();
                        if (!ProfileStatus.ACTIVE.equals(profile.getStatus())) {
                            status = CheckProfileStatus.INACTIVE;
                        }
                    } else {
                        dataKey = THE_PROFILE_NOT_FOUND;
                        dataValue = profileIdentifier;
                        status = CheckProfileStatus.UNKNOWN;
                    }
                } else {
                    dataKey = CAN_NOT_GET_THE_PROFILE;
                    dataValue = profileIdentifier;
                    status = CheckProfileStatus.UNKNOWN;
                }
            } else {
                // Force to null even if profileIdentifier is empty not null string
                profileIdentifier = null;
            }
            // Validate profile according to contract
            if (null == status) {
                if (ParametersChecker.isNotEmpty(ingestContractIdentifier)) {
                    Select select = new Select();
                    select.setQuery(QueryHelper.eq(IngestContract.IDENTIFIER, ingestContractIdentifier));
                    JsonNode queryDsl = select.getFinalSelect();
                    RequestResponse<IngestContractModel> referenceContracts = adminClient.findIngestContracts(queryDsl);
                    if (referenceContracts.isOk()) {
                        List<IngestContractModel> results =
                            ((RequestResponseOK<IngestContractModel>) referenceContracts).getResults();
                        if (null != results && results.size() > 0) {
                            IngestContractModel contract = results.iterator().next();
                            if (
                                (null == profileIdentifier && contract.getArchiveProfiles().isEmpty()) ||
                                (contract.getArchiveProfiles().contains(profileIdentifier))
                            ) {
                                status = CheckProfileStatus.OK;
                            } else {
                                status = CheckProfileStatus.DIFF;
                            }
                        } else {
                            dataKey = CAN_NOT_GET_THE_INGEST_CONTRACT;
                            dataValue = ingestContractIdentifier;
                            status = CheckProfileStatus.UNKNOWN;
                        }
                    } else {
                        dataKey = CAN_NOT_GET_THE_INGEST_CONTRACT;
                        dataValue = ingestContractIdentifier;
                        status = CheckProfileStatus.UNKNOWN;
                    }
                } else {
                    status = CheckProfileStatus.OK;
                }
            }
        } catch (
            InvalidCreateOperationException | InvalidParseOperationException | AdminManagementClientServerException e
        ) {
            LOGGER.error(PROFILE_NOT_FOUND, e);
            status = CheckProfileStatus.KO;
        } catch (Exception e) {
            LOGGER.error(UNKNOWN_TECHNICAL_EXCEPTION, e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        switch (status) {
            case INACTIVE:
                itemStatus.setGlobalOutcomeDetailSubcode(CheckProfileStatus.INACTIVE.toString());
                infoNode.put(
                    SedaConstants.EV_DET_TECH_DATA,
                    "The profile " + profileIdentifier + " has not the status ACTIVE"
                );
                itemStatus.increment(StatusCode.KO);
                break;
            case UNKNOWN:
                itemStatus.setGlobalOutcomeDetailSubcode(CheckProfileStatus.UNKNOWN.toString());
                itemStatus.increment(StatusCode.KO);
                infoNode.put(SedaConstants.EV_DET_TECH_DATA, dataKey + " " + dataValue);
                break;
            case DIFF:
                itemStatus.setGlobalOutcomeDetailSubcode(CheckProfileStatus.DIFF.toString());
                itemStatus.increment(StatusCode.KO);
                infoNode.put(
                    SedaConstants.EV_DET_TECH_DATA,
                    "The profile " + profileIdentifier + " was not found in the ingest contract"
                );

                break;
            case KO:
                itemStatus.increment(StatusCode.KO);
                break;
            case OK:
                itemStatus.increment(StatusCode.OK);
                break;
        }

        infoNode.put(SedaConstants.TAG_ARCHIVE_PROFILE, profileIdentifier);
        String evdev = JsonHandler.unprettyPrint(infoNode);
        itemStatus.setEvDetailData(evdev);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evdev);

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}

    /**
     * Check profile status values
     */
    public enum CheckProfileStatus {
        /**
         * Missing profile
         */
        UNKNOWN,
        /**
         * Existing but inactive profile
         */
        INACTIVE,
        /**
         * The profile mentioned in the contract but not mentioned in the SIP
         */
        DIFF,
        /**
         * OK profile
         */
        OK,
        /**
         * Other error situation
         */
        KO,
    }
}
