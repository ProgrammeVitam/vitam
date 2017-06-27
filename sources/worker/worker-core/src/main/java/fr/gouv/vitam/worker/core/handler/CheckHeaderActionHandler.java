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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookEvDetDataType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

/**
 * Check HEADER Handler
 */
public class CheckHeaderActionHandler extends ActionHandler {

    private static final String EV_DET_DATA_TYPE = "evDetDataType";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckHeaderActionHandler.class);
    private static final String HANDLER_ID = "CHECK_HEADER";
    private static final int CHECK_CONTRACT_RANK = 0;
    private static final int CHECK_ORIGINATING_AGENCY_RANK = 1;
    private static final String EV_DETAIL_REQ = "EvDetailReq";
    private static final int CHECK_PROFILE_RANK = 2;
    /**
     * empty Constructor
     *
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
        Map<String, Object> madatoryValueMap = new HashMap<>();      
        ObjectNode infoNode = JsonHandler.createObjectNode();
        final boolean shouldCheckContract  = Boolean.valueOf((String) handlerIO.getInput(CHECK_CONTRACT_RANK));
        final boolean shouldCheckOriginatingAgency = 
            Boolean.valueOf((String) handlerIO.getInput(CHECK_ORIGINATING_AGENCY_RANK));
        final boolean shouldCheckProfile = 
            Boolean.valueOf((String) handlerIO.getInput(CHECK_PROFILE_RANK));
        
        try {
            madatoryValueMap = sedaUtils.getMandatoryValues(params);
        } catch (final ProcessingException e) {
            LOGGER.error("getMandatoryValues ProcessingException", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }


        if (madatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
            itemStatus.setData(SedaConstants.TAG_MESSAGE_IDENTIFIER,
                madatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER));
            itemStatus.setMasterData(LogbookParameterName.objectIdentifierIncome.name(),
                madatoryValueMap.get(SedaConstants.TAG_MESSAGE_IDENTIFIER));
        }

        updateSedaInfo(madatoryValueMap, infoNode);
        itemStatus.setData(LogbookParameterName.eventDetailData.name(), 
            JsonHandler.unprettyPrint(infoNode));
        
        if (shouldCheckOriginatingAgency && 
            Strings.isNullOrEmpty((String) madatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER))) {
            itemStatus.increment(StatusCode.KO);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }
        
        if (madatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER) != null) {
            itemStatus.setMasterData(LogbookParameterName.agentIdentifierOriginating.name(),
                madatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
        }
        
        if (madatoryValueMap.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER) != null) {
            itemStatus.setMasterData(LogbookParameterName.agentIdentifierSubmission.name(),
                madatoryValueMap.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
        }

        if (madatoryValueMap.get(SedaConstants.TAG_COMMENT) != null) {
            itemStatus.setMasterData(LogbookParameterName.objectIdentifierIncome.name(), madatoryValueMap.get
                (SedaConstants.TAG_COMMENT));
        }

        if (shouldCheckContract) {
            if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null) {
                final String contractName = (String) madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
                handlerIO.getInput().clear();
                handlerIO.getInput().add(contractName);
                CheckIngestContractActionHandler checkIngestContractActionHandler = new CheckIngestContractActionHandler();
                final ItemStatus checkContratItemStatus = checkIngestContractActionHandler.execute(params, handlerIO);
                itemStatus.setItemsStatus(CheckIngestContractActionHandler.getId(), checkContratItemStatus);
                checkIngestContractActionHandler.close();
                if (checkContratItemStatus.shallStop(true)) {
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }
            }            
        }
        if (shouldCheckProfile) {
            if (madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {               
                final String profileName = (String) madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE);
                handlerIO.getInput().clear();
                handlerIO.getInput().add(madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE));
                handlerIO.getInput().add(madatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT));
                CheckArchiveProfileRelationActionHandler checkProfileRelation = new CheckArchiveProfileRelationActionHandler();
                final ItemStatus checkProfilRelationItemStatus = checkProfileRelation.execute(params, handlerIO);
                itemStatus.setItemsStatus(CheckArchiveProfileRelationActionHandler.getId(), checkProfilRelationItemStatus);
                checkProfileRelation.close();
                if (checkProfilRelationItemStatus.shallStop(true)) {
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }

                handlerIO.getInput().clear();
                handlerIO.getInput().add(madatoryValueMap.get(SedaConstants.TAG_ARCHIVE_PROFILE));
                CheckArchiveProfileActionHandler checkArchiveProfile = new CheckArchiveProfileActionHandler();
                final ItemStatus checkProfilItemStatus = checkArchiveProfile.execute(params, handlerIO);
                itemStatus.setItemsStatus(CheckArchiveProfileActionHandler.getId(), checkProfilItemStatus);
                checkArchiveProfile.close();
                if (checkProfilItemStatus.shallStop(true)) {
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                } 

            } else {
                // Return ok in case of missing profile
                
               ItemStatus checkProfileStatus = new ItemStatus(CheckArchiveProfileActionHandler.getId());
               checkProfileStatus.increment(StatusCode.OK);
               ItemStatus checkProfileRelationStatus = new ItemStatus(CheckArchiveProfileRelationActionHandler.getId());
               checkProfileRelationStatus.increment(StatusCode.OK);
               itemStatus.setItemsStatus(CheckArchiveProfileRelationActionHandler.getId(), checkProfileRelationStatus);
               itemStatus.setItemsStatus(CheckArchiveProfileActionHandler.getId(), checkProfileStatus);
            }
        } else {
            itemStatus.increment(StatusCode.OK);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void updateSedaInfo(Map<String, Object> madatoryValueMap, ObjectNode infoNode) {
        infoNode.put(LogbookOperationsClientHelper.EV_DET_DATA_TYPE, 
            LogbookEvDetDataType.MASTER.name());

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
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {

    }

}
