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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookEvDetDataType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaConstants;

/**
 * Check Archive Profile Relation Handler  
 * Verify the relation between ingest contract and profil in manifest
 */
public class CheckArchiveProfileRelationActionHandler extends ActionHandler {
    
    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";
    private static final String CAN_NOT_GET_THE_INGEST_CONTRACT = "Can not get the ingest contract";
    private static final String PROFIL_IS_NOT_DECLARED_IN_THE_INGEST_CONTRACT = "Profil is not declared in the ingest contract";
    private static final String PROFILE_NOT_FOUND = "Profile not found";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveProfileRelationActionHandler.class);
    private static final String HANDLER_ID = "CHECK_IC_AP_RELATION";
    private static final int PROFILE_IDENTIFIER_RANK = 0;
    private static final int CONTRACT_NAME_RANK = 1;

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public CheckArchiveProfileRelationActionHandler() {
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
        final String profileIdentifier = (String) handlerIO.getInput(PROFILE_IDENTIFIER_RANK); 
        final String contractName = (String) handlerIO.getInput(CONTRACT_NAME_RANK);
        Boolean isValid = true;
        
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(IngestContract.NAME, contractName));
            JsonNode queryDsl = select.getFinalSelect();
            RequestResponse<IngestContractModel> referenceContracts = adminClient.findIngestContracts(queryDsl);
            if (referenceContracts.isOk() && ((RequestResponseOK<IngestContractModel> ) referenceContracts).getResults().size() > 0) {
                IngestContractModel contract = ((RequestResponseOK<IngestContractModel> ) referenceContracts).getResults().get(0);
                isValid = contract.getArchiveProfiles().contains(profileIdentifier);
            } else {
                isValid = false;
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | 
            AdminManagementClientServerException e) {
            LOGGER.error(PROFILE_NOT_FOUND, e);
            itemStatus.increment(StatusCode.KO);
            itemStatus.setData(CAN_NOT_GET_THE_INGEST_CONTRACT, contractName);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        } catch (Exception e) {
            LOGGER.error(UNKNOWN_TECHNICAL_EXCEPTION, e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }
        
        if (isValid) {
            itemStatus.increment(StatusCode.OK);
        } else {
            itemStatus.increment(StatusCode.KO);
            ObjectNode errorNode = JsonHandler.createObjectNode();
            errorNode.put(LogbookOperationsClientHelper.EV_DET_DATA_TYPE, 
                LogbookEvDetDataType.MASTER.name());
            errorNode.put(SedaConstants.TAG_ARCHIVE_PROFILE, profileIdentifier);
            itemStatus.setData(LogbookParameterName.eventDetailData.name(), 
                JsonHandler.unprettyPrint(errorNode));
            itemStatus.setData(PROFIL_IS_NOT_DECLARED_IN_THE_INGEST_CONTRACT, contractName);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }

}
