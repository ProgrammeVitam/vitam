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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterDetailModel;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterDetailModelBuilder;
import fr.gouv.vitam.functional.administration.client.model.RegisterValueDetailModel;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterStatus;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

/**
 * Accession Register Handler
 */
public class AccessionRegisterActionHandler extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessionRegisterActionHandler.class);
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private HandlerIO handlerIO;

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();

    private static final int HANDLER_IO_PARAMETER_NUMBER = 4;
    private static final int ARCHIVE_UNIT_MAP_RANK = 0;
    private static final int OBJECTGOUP_MAP_RANK = 1;
    private static final int BDO_TO_VERSION_BDO_MAP_RANK = 2;
    private static final int SEDA_PARAMETERS_RANK = 3;

    /**
     * Empty Constructor AccessionRegisterActionHandler
     */
    public AccessionRegisterActionHandler() {
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
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        LOGGER.debug("TransferNotificationActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        handlerIO = handler;
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            checkMandatoryIOParameter(handler);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Params: " + params);
            }
            final AccessionRegisterDetailModel register = generateAccessionRegister(params);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("register ID / Originating Agency: " + register.getId() + " / "
                    + register.getOriginatingAgency());
            }
            adminClient.createorUpdateAccessionRegister(register);
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException | AdminManagementClientServerException e) {
            LOGGER.error("Inputs/outputs are not correct", e);
            itemStatus.increment(StatusCode.KO);
        } catch (AccessionRegisterException | DatabaseConflictException e) {
            LOGGER.error("Can not create func register", e);
            itemStatus.increment(StatusCode.KO);
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

    private AccessionRegisterDetailModel generateAccessionRegister(WorkerParameters params) throws ProcessingException {
        AccessionRegisterDetailModel register =
            new AccessionRegisterDetailModelBuilder().createAccessionRegisterDetailModel();
        try (final InputStream archiveUnitMapStream =
            new FileInputStream((File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK));
            final InputStream objectGoupMapStream =
                new FileInputStream((File) handlerIO.getInput(OBJECTGOUP_MAP_RANK));
            final InputStream bdoToVersionMapTmpFile =
                new FileInputStream((File) handlerIO.getInput(BDO_TO_VERSION_BDO_MAP_RANK))) {
            final Map<String, Object> bdoVersionMap = JsonHandler.getMapFromInputStream(bdoToVersionMapTmpFile);
            final Map<String, Object> archiveUnitMap = JsonHandler.getMapFromInputStream(archiveUnitMapStream);
            final Map<String, Object> objectGroupMap = JsonHandler.getMapFromInputStream(objectGoupMapStream);
            final JsonNode sedaParameters =
                JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK))
                    .get(SedaConstants.TAG_ARCHIVE_TRANSFER);
            String originalAgency = "OriginatingAgencyUnknown";
            String submissionAgency = "SubmissionAgencyUnknown";
            if (sedaParameters != null) {
                final JsonNode node = sedaParameters.get(SedaConstants.TAG_DATA_OBJECT_PACKAGE);
                if (node != null) {
                    final JsonNode nodeOrigin = node.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
                    if (nodeOrigin != null && !Strings.isNullOrEmpty(nodeOrigin.asText())) {
                        originalAgency = nodeOrigin.asText();
                    } else {
                        throw new ProcessingException("No " + SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER + " found");
                    }
                    final JsonNode nodeSubmission = node.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER);
                    if (nodeSubmission != null && !Strings.isNullOrEmpty(nodeSubmission.asText())) {
                        submissionAgency = nodeSubmission.asText();
                    } else {
                        submissionAgency = originalAgency;
                    }
                } else {
                    throw new ProcessingException("No DataObjectPackage found");
                }
            } else {
                throw new ProcessingException("No ArchiveTransfer found");
            }

            // TODO P0 get size manifest.xml in local
            // TODO P0 extract this information from first parsing
            final SedaUtils sedaUtils = SedaUtilsFactory.create(handlerIO);
            register =
                mapParamsToAccessionRegisterDetailModel(params, bdoVersionMap, archiveUnitMap, objectGroupMap,
                    originalAgency,
                    submissionAgency, sedaUtils);
        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.error("Inputs/outputs are not correct", e);
            throw new ProcessingException(e);
        }

        return register;
    }

    private AccessionRegisterDetailModel mapParamsToAccessionRegisterDetailModel(WorkerParameters params,
        Map<String, Object> bdoVersionMap, Map<String, Object> archiveUnitMap, Map<String, Object> objectGroupMap,
        String originalAgency, String submissionAgency, SedaUtils sedaUtils) throws ProcessingException {

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        final long objectsSizeInSip = sedaUtils.computeTotalSizeOfObjectsInManifest(params);

        RegisterValueDetailModel totalObjectsGroups =
            new RegisterValueDetailModel(objectGroupMap.size(), 0, objectGroupMap.size(), null);

        RegisterValueDetailModel totalUnits =
            new RegisterValueDetailModel(archiveUnitMap.size(), 0, archiveUnitMap.size(), null);

        RegisterValueDetailModel totalObjects =
            new RegisterValueDetailModel(bdoVersionMap.size(), 0, bdoVersionMap.size(), null);

        RegisterValueDetailModel objectSize = new RegisterValueDetailModel(objectsSizeInSip, 0, objectsSizeInSip, null);

        return new AccessionRegisterDetailModelBuilder()
            .setId(params.getContainerName())
            .setOriginatingAgency(originalAgency)
            .setSubmissionAgency(submissionAgency)
            .setEndDate(sdfDate.format(new Date()))
            .setStartDate(sdfDate.format(new Date()))
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjectsGroups(totalObjectsGroups)
            .setTotalUnits(totalUnits)
            .setTotalObjects(totalObjects)
            .setObjectSize(objectSize)
            .createAccessionRegisterDetailModel();
    }

    @Override
    public void close() {
        // Empty
    }
}
