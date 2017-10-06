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

import static fr.gouv.vitam.common.SedaConstants.DATE_TIME_FORMAT_PATERN;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
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

    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATERN);

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
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
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

            // operation id
            String operationId = params.getContainerName();

            List<UnitPerOriginatingAgency> agencies =
                metaDataClient.selectAccessionRegisterOnUnitByOperationId(operationId);

            if (agencies == null || agencies.isEmpty()) {
                return itemStatus.increment(StatusCode.OK);
            }

            List<ObjectGroupPerOriginatingAgency> objectGroupPerOriginatingAgencies =
                metaDataClient.selectAccessionRegisterOnObjectByOperationId(operationId);

            ImmutableMap<String, ObjectGroupPerOriginatingAgency> objectGroupPerOriginatingAgencyImmutableMap =
                Maps.uniqueIndex(objectGroupPerOriginatingAgencies,
                    ObjectGroupPerOriginatingAgency::getOriginatingAgency);
            ObjectNode evDetDataInformation = JsonHandler.createObjectNode();
            ArrayNode arrayInformation = JsonHandler.createArrayNode();
            for (UnitPerOriginatingAgency agency : agencies) {
                final AccessionRegisterDetailModel register = generateAccessionRegister(params,
                    objectGroupPerOriginatingAgencyImmutableMap
                        .getOrDefault(agency.getId(), new ObjectGroupPerOriginatingAgency()),
                    agency, tenantId);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("register ID / Originating Agency: " + register.getId() + " / " +
                        register.getOriginatingAgency());
                }
                VitamThreadUtils.getVitamSession().setContractId(VitamConstants.EVERY_ORIGINATING_AGENCY);
                Select select = new Select();
                try {
                    select
                        .setQuery(QueryHelper.and().add(QueryHelper.in("OperationIds", operationId)));

                    RequestResponse<AccessionRegisterDetailModel> response =
                        adminClient.getAccessionRegisterDetail(agency.getId(), select.getFinalSelect());
                    if (response.isOk()) {
                        RequestResponseOK<AccessionRegisterDetailModel> responseOK =
                            (RequestResponseOK<AccessionRegisterDetailModel>) response;
                        if (responseOK.getResults().size() > 0) {
                            LOGGER.warn(
                                "Step already executed, this is a replayed step for this operation : " + operationId);
                            itemStatus.increment(StatusCode.ALREADY_EXECUTED);
                            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                        }
                    }
                } catch (InvalidCreateOperationException | InvalidParseOperationException | ReferentialException e) {
                    LOGGER.warn("Couldnt check accession register for this operation : " + operationId +
                        ". Lets continue anyways.");
                }

                ArrayNode agencyVolumetry = JsonHandler.createArrayNode();
                agencyVolumetry.addPOJO(JsonHandler.createObjectNode().set("TotalObjects",
                    JsonHandler.toJsonNode(register.getTotalObjects())));
                agencyVolumetry.addPOJO(JsonHandler.createObjectNode().set("TotalObjectsGroups",
                    JsonHandler.toJsonNode(register.getTotalObjectsGroups())));
                agencyVolumetry.addPOJO(
                    JsonHandler.createObjectNode().set("TotalUnits", JsonHandler.toJsonNode(register.getTotalUnits())));
                agencyVolumetry.addPOJO(
                    JsonHandler.createObjectNode().set("ObjectSize", JsonHandler.toJsonNode(register.getObjectSize())));
                arrayInformation.addPOJO(JsonHandler.createObjectNode().set(agency.getId(), agencyVolumetry));
                adminClient.createorUpdateAccessionRegister(register);
            }

            if (arrayInformation.size() > 0) {
                evDetDataInformation.set(VOLUMETRY, arrayInformation);
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(evDetDataInformation));
            }

            itemStatus.increment(StatusCode.OK);
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

    private AccessionRegisterDetailModel generateAccessionRegister(WorkerParameters params,
        ObjectGroupPerOriginatingAgency objectGroupPerOriginatingAgency, UnitPerOriginatingAgency agency, int tenantId)
        throws ProcessingException {
        try {

            final JsonNode sedaParameters =
                JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK))
                    .get(SedaConstants.TAG_ARCHIVE_TRANSFER);
            String originalAgency = agency.getId();
            String submissionAgency;
            String archivalAgreement = "ArchivalAgreementUnknow";

            boolean symbolic;

            if (sedaParameters != null) {
                final JsonNode dataObjectNode = sedaParameters.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
                if (dataObjectNode != null) {

                    final JsonNode nodeSubmission = dataObjectNode.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER);
                    if (nodeSubmission != null && !Strings.isNullOrEmpty(nodeSubmission.asText())) {
                        submissionAgency = nodeSubmission.asText();
                    } else {
                        submissionAgency = originalAgency;
                    }

                    final JsonNode nodeOrigin = dataObjectNode.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
                    if (nodeOrigin != null && !Strings.isNullOrEmpty(nodeOrigin.asText())) {
                        symbolic = !nodeOrigin.asText().equals(originalAgency);
                    } else {
                        throw new ProcessingException("No " + SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER + " found");
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

            // TODO P0 get size manifest.xml in local
            // TODO P0 extract this information from first parsing
            return mapParamsToAccessionRegisterDetailModel(params,
                originalAgency, submissionAgency, archivalAgreement, agency,
                objectGroupPerOriginatingAgency, tenantId, symbolic);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Inputs/outputs are not correct", e);
            throw new ProcessingException(e);
        }
    }

    private AccessionRegisterDetailModel mapParamsToAccessionRegisterDetailModel(WorkerParameters params,
        String originalAgency, String submissionAgency, String archivalAgreement, UnitPerOriginatingAgency agency,
        ObjectGroupPerOriginatingAgency objectGroupPerOriginatingAgency, int tenantId, boolean symbolic)
        throws ProcessingException {

        RegisterValueDetailModel totalObjectsGroups, totalUnits, totalObjects, objectSize;

        if (!symbolic) {
            totalObjectsGroups =
                new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getNumberOfGOT(), 0,
                    objectGroupPerOriginatingAgency.getNumberOfGOT());
            totalUnits =
                new RegisterValueDetailModel(agency.getCount(), 0, agency.getCount());
            totalObjects =
                new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getNumberOfObject(), 0,
                    objectGroupPerOriginatingAgency.getNumberOfObject());
            objectSize = new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getSize(), 0,
                objectGroupPerOriginatingAgency.getSize());
        } else {
            totalObjectsGroups =
                new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getNumberOfGOT(),
                    objectGroupPerOriginatingAgency.getNumberOfGOT(), 0, true);
            totalUnits =
                new RegisterValueDetailModel(agency.getCount(),
                    agency.getCount(), 0, true);
            totalObjects =
                new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getNumberOfObject(),
                    objectGroupPerOriginatingAgency.getNumberOfObject(), 0, true);
            objectSize = new RegisterValueDetailModel(objectGroupPerOriginatingAgency.getSize(),
                objectGroupPerOriginatingAgency.getSize(), 0, true);
        }

        String updateDate = ZonedDateTime.now().format(DATE_TIME_FORMATTER);

        GUID guid = GUIDFactory.newAccessionRegisterDetailGUID(tenantId);

        return new AccessionRegisterDetailModel()
            .setId(guid.toString())
            .setOriginatingAgency(originalAgency)
            .setSubmissionAgency(submissionAgency)
            .setArchivalAgreement(archivalAgreement)
            .setEndDate(updateDate)
            .setLastUpdate(updateDate)
            .setStartDate(updateDate)
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjectsGroups(totalObjectsGroups)
            .setTotalUnits(totalUnits)
            .setTotalObjects(totalObjects)
            .setObjectSize(objectSize)
            .setSymbolic(symbolic)
            .addOperationsId(params.getContainerName());
    }

    @Override
    public void close() {
        // Empty
    }

}
