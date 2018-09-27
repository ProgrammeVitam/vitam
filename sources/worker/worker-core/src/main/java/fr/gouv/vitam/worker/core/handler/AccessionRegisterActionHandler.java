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

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

/**
 * Accession Register Handler
 */
public class AccessionRegisterActionHandler extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessionRegisterActionHandler.class);
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private static final String VOLUMETRY = "Volumetry";
    private HandlerIO handlerIO;

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();

    private static final int HANDLER_IO_PARAMETER_NUMBER = 4;
    private static final int SEDA_PARAMETERS_RANK = 3;

    private MetaDataClientFactory metaDataClientFactory;
    private AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Empty Constructor AccessionRegisterActionHandler
     */
    public AccessionRegisterActionHandler() {
        this(MetaDataClientFactory.getInstance(), AdminManagementClientFactory.getInstance());
    }

    AccessionRegisterActionHandler(MetaDataClientFactory metaDataClientFactory,
        AdminManagementClientFactory adminManagementClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.add(File.class);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        LOGGER.debug("TransferNotificationActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        handlerIO = handler;

        int tenantId = HeaderIdHelper.getTenantId();
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient();
            MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            checkMandatoryIOParameter(handler);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Params: " + params);
            }


            String originatingAgency;
            String submissionAgency = null;
            String acquisitionInformation = null;
            String legalStatus = null;
            String archivalAgreement = null;

            final JsonNode sedaParameters =
                JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK))
                    .get(SedaConstants.TAG_ARCHIVE_TRANSFER);

            if (sedaParameters != null) {
                final JsonNode dataObjectNode = sedaParameters.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
                if (dataObjectNode != null) {

                    final JsonNode nodeSubmission = dataObjectNode.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER);
                    if (nodeSubmission != null && !Strings.isNullOrEmpty(nodeSubmission.asText())) {
                        submissionAgency = nodeSubmission.asText();
                    }

                    final JsonNode nodeOrigin = dataObjectNode.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
                    if (nodeOrigin != null && !Strings.isNullOrEmpty(nodeOrigin.asText())) {
                        originatingAgency = nodeOrigin.asText();
                    } else {
                        throw new ProcessingException("No " + SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER + " found");
                    }

                    final JsonNode nodeAcquisitionInformation =
                        dataObjectNode.get(SedaConstants.TAG_ACQUISITIONINFORMATION);
                    if (nodeAcquisitionInformation != null &&
                        !Strings.isNullOrEmpty(nodeAcquisitionInformation.asText())) {
                        acquisitionInformation = nodeAcquisitionInformation.asText();
                    }

                    final JsonNode nodeLegalStatus = dataObjectNode.get(SedaConstants.TAG_LEGALSTATUS);
                    if (nodeLegalStatus != null && !Strings.isNullOrEmpty(nodeLegalStatus.asText())) {
                        legalStatus = nodeLegalStatus.asText();
                    }


                } else {
                    throw new ProcessingException("No DataObjectPackage found");
                }

                final JsonNode archivalArchivalAgreement = sedaParameters.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
                if (archivalArchivalAgreement != null && !Strings.isNullOrEmpty(archivalArchivalAgreement.asText())) {
                    archivalAgreement = archivalArchivalAgreement.asText();
                }
            } else {
                throw new ProcessingException("No ArchiveTransfer found");
            }

            if (Strings.isNullOrEmpty(submissionAgency)) {
                submissionAgency = originatingAgency;
            }

            // Operation id
            String ingestOperationId = params.getContainerName();


            List<ObjectGroupPerOriginatingAgency> objectGroupPerOriginatingAgencies =
                metaDataClient.selectAccessionRegisterOnObjectByOperationId(ingestOperationId);

            Map<String, Map<String, ObjectGroupPerOriginatingAgency>> map = new HashMap<>();

            if (objectGroupPerOriginatingAgencies == null || objectGroupPerOriginatingAgencies.isEmpty()) {
                map.put(ingestOperationId, new HashMap<>());
            } else {

                for (ObjectGroupPerOriginatingAgency o : objectGroupPerOriginatingAgencies) {
                    String qualifierVersionOpi = o.getOperation();
                    String agency = o.getAgency();

                    Map<String, ObjectGroupPerOriginatingAgency> opMap =
                        map.getOrDefault(qualifierVersionOpi, new HashMap<>());

                    if (opMap.isEmpty()) {
                        map.put(qualifierVersionOpi, opMap);
                    }

                    // We are in the context of ObjectGroupPerOriginatingAgency of qualifierVersionOpi
                    ObjectGroupPerOriginatingAgency ino = opMap.get(agency);
                    if (null == ino) {
                        opMap.put(agency, o);
                    } else {
                        // After un-count GOT where opi != qualifierVersionOpi
                        // Sum all ObjectGroupPerOriginatingAgency of the same agency
                        ino.setNumberOfGOT(ino.getNumberOfGOT() + o.getNumberOfGOT());
                        ino.setNumberOfObject(ino.getNumberOfObject() + o.getNumberOfObject());
                        ino.setSize(ino.getSize() + o.getSize());
                    }
                }
            }


            Set<String> allConcernedOperationSet = new HashSet<>(map.keySet());
            allConcernedOperationSet.remove(ingestOperationId);
            // Start by the current operation
            List<String> allConcernedOperations = new ArrayList<>();
            allConcernedOperations.add(ingestOperationId);
            allConcernedOperations.addAll(allConcernedOperationSet);


            // Get ingest originating agency


            List<UnitPerOriginatingAgency> unitPerOriginatingAgencies =
                metaDataClient.selectAccessionRegisterOnUnitByOperationId(ingestOperationId);
            if (unitPerOriginatingAgencies == null) {
                unitPerOriginatingAgencies = new ArrayList<>();
            }


            ObjectNode evDetDataInformation = JsonHandler.createObjectNode();
            ArrayNode arrayInformation = JsonHandler.createArrayNode();
            boolean alreadyExecuted = false;
            boolean mayBeRestartAfterFatal = false;
            for (String currentOperation : allConcernedOperations) {
                Map<String, UnitPerOriginatingAgency> unitPerOriginatingAgenciesMap = new HashMap<>();

                if (currentOperation.equals(ingestOperationId)) {
                    for (UnitPerOriginatingAgency o : unitPerOriginatingAgencies) {
                        unitPerOriginatingAgenciesMap.put(o.getValue(), o);
                    }
                }

                Map<String, ObjectGroupPerOriginatingAgency> objectGroupPerOriginatingAgenciesMap =
                    map.getOrDefault(currentOperation, new HashMap<>());


                if (unitPerOriginatingAgenciesMap.isEmpty() && objectGroupPerOriginatingAgenciesMap.isEmpty()) {
                    return itemStatus.increment(StatusCode.OK);
                }

                Set<String> agencies = Stream.concat(unitPerOriginatingAgenciesMap.keySet().stream(),
                    objectGroupPerOriginatingAgenciesMap.keySet().stream()).collect(
                    Collectors.toSet());


                for (String agency : agencies) {
                    if (!originatingAgency.equals(agency)) {
                        continue;
                    }
                    final AccessionRegisterDetailModel register = generateAccessionRegister(params,
                        ingestOperationId,
                        currentOperation,
                        objectGroupPerOriginatingAgenciesMap
                            .getOrDefault(agency, new ObjectGroupPerOriginatingAgency().setAgency(agency)),
                        unitPerOriginatingAgenciesMap.getOrDefault(agency, new UnitPerOriginatingAgency(agency, 0)),
                        originatingAgency,
                        submissionAgency,
                        acquisitionInformation,
                        legalStatus,
                        archivalAgreement,
                        tenantId);

                    if (null == register) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                "All count equals to 0 => register not created for Originating Agency: " + agency);
                        }
                        continue;
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("register ID / Originating Agency: " + register.getId() + " / " +
                            register.getOriginatingAgency());
                    }

                    // ugly hack > using raw and non raw method
                    ObjectNode jsonNodeRegister = (ObjectNode) JsonHandler.toJsonNode(register);
                    jsonNodeRegister.put("_id", register.getId());
                    jsonNodeRegister.put("_tenant", tenantId);
                    jsonNodeRegister.put("_v", 0);
                    jsonNodeRegister.remove("#id");
                    RequestResponse<AccessionRegisterDetailModel> resp =
                        adminClient.createorUpdateAccessionRegister(register);

                    // If already exists in database
                    if (resp.getStatus() == javax.ws.rs.core.Response.Status.CONFLICT.getStatusCode()) {
                        // If the current ingest operation and the accession register detail already exists, then already executed
                        if (currentOperation.equals(ingestOperationId)) {
                            LOGGER.warn(String.format(
                                "Step already executed, this is a replayed step for this operation : %s .",
                                currentOperation));
                            alreadyExecuted = true;
                        }
                    } else {
                        if (currentOperation.equals(ingestOperationId)) {
                            // In case where 2 agency (we created detail for one, then fatal occus
                            // for the second because of database issue.
                            // after restart step, the first one will be conflict, the second will be created.
                            // We should not consider the step as already executed
                            mayBeRestartAfterFatal = true;
                        }
                        // Add only created one, ignore conflict one
                        arrayInformation.addPOJO(jsonNodeRegister);
                    }
                }
            }
            if (alreadyExecuted && !mayBeRestartAfterFatal) {
                // Only if all originating agency
                itemStatus.increment(StatusCode.ALREADY_EXECUTED);
            } else {

                if (arrayInformation.size() > 0) {
                    evDetDataInformation.set(VOLUMETRY, arrayInformation);
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(evDetDataInformation));
                }
                itemStatus.increment(StatusCode.OK);
            }
        } catch (ProcessingException | AdminManagementClientServerException e) {
            LOGGER.error("Inputs/outputs are not correct", e);
            itemStatus.increment(StatusCode.KO);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Can not parse register", e);
            itemStatus.increment(StatusCode.KO);
        } catch (AccessionRegisterException | DatabaseConflictException e) {
            LOGGER.error("Can not create func register", e);
            itemStatus.increment(StatusCode.KO);
        } catch (MetaDataClientServerException e) {
            LOGGER.error("unable to call metadata Client", e);
            itemStatus.increment(StatusCode.FATAL);
        }

        LOGGER.debug("TransferNotificationActionHandler response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(0, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }

    private AccessionRegisterDetailModel generateAccessionRegister(
        WorkerParameters params,
        String ingestOperation,
        String currentOperation,
        ObjectGroupPerOriginatingAgency objectGroupPerOriginatingAgency,
        UnitPerOriginatingAgency unitPerOriginatingAgency,
        String originatingAgency,
        String submissionAgency,
        String acquisitionInformation,
        String legalStatus,
        String archivalAgreement,
        int tenantId) {

        String unitAgency = unitPerOriginatingAgency.getValue();

        long unitCount = unitPerOriginatingAgency.getCount();

        long nbGot = objectGroupPerOriginatingAgency.getNumberOfGOT();
        long nbObject = objectGroupPerOriginatingAgency.getNumberOfObject();
        long size = objectGroupPerOriginatingAgency.getSize();

        boolean returnNull = 0l == nbGot && 0l == nbObject && 0l == size;
        boolean zeroUnit = unitCount == 0;

        if (zeroUnit && returnNull) {
            // Do not create accession register detail
            return null;
        }

        RegisterValueDetailModel totalUnits =
            new RegisterValueDetailModel().setIngested(unitCount).setRemained(unitCount);
        RegisterValueDetailModel totalObjectsGroups =
            new RegisterValueDetailModel().setIngested(nbGot).setRemained(nbGot);
        RegisterValueDetailModel totalObjects =
            new RegisterValueDetailModel().setIngested(nbObject).setRemained(nbObject);
        RegisterValueDetailModel objectSize = new RegisterValueDetailModel().setIngested(size).setRemained(size);

        String updateDate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

        GUID guid = GUIDFactory.newAccessionRegisterDetailGUID(tenantId);

        RegisterValueEventModel registerValueEvent = new RegisterValueEventModel()
            .setOperation(currentOperation)
            .setOperationType(LogbookTypeProcess.INGEST.name())
            .setTotalUnits(totalUnits.getRemained())
            .setTotalGots(totalObjectsGroups.getRemained())
            .setTotalObjects(totalObjects.getRemained())
            .setObjectSize(objectSize.getRemained())
            .setCreationdate(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));

        return new AccessionRegisterDetailModel()
            .setId(guid.toString())
            .setOpc(currentOperation)
            .setOriginatingAgency(unitAgency)
            .setSubmissionAgency(submissionAgency)
            .setArchivalAgreement(archivalAgreement)
            .setAcquisitionInformation(acquisitionInformation)
            .setLegalStatus(legalStatus)
            .setEndDate(updateDate)
            .setLastUpdate(updateDate)
            .setStartDate(updateDate)
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjectsGroups(totalObjectsGroups)
            .setTotalUnits(totalUnits)
            .setTotalObjects(totalObjects)
            .setObjectSize(objectSize)
            .setOpi(ingestOperation)
            .setOperationType(LogbookTypeProcess.INGEST.name())
            .addEvent(registerValueEvent)
            .addOperationsId(params.getContainerName())
                .setTenant(tenantId);
    }

    @Override
    public void close() {
        // Empty
    }

}
