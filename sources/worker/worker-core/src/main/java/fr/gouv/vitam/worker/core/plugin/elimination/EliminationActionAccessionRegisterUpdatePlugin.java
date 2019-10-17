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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.time.LocalDateTime;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Elimination action accession register update plugin.
 */
public class EliminationActionAccessionRegisterUpdatePlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionAccessionRegisterUpdatePlugin.class);

    private static final String ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE =
        "ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE";

    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Default constructor
     */
    public EliminationActionAccessionRegisterUpdatePlugin() {
        this(AdminManagementClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionAccessionRegisterUpdatePlugin(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try {

            updateAccessionRegister(param);

            LOGGER.info("Elimination action accession register update succeeded");
            return buildItemStatus(ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE, StatusCode.OK, null);

        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format(
                "Elimination action accession register update failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE, e.getStatusCode(),
                e.getEventDetails());
        }
    }

    private void updateAccessionRegister(WorkerParameters param) throws ProcessingStatusException {

        EliminationActionAccessionRegisterModel eliminationActionAccessionRegisterModel =
            loadEliminationActionAccessionRegisterModel(param);

        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {

            RegisterValueEventModel registerValueEvent = new RegisterValueEventModel()
                .setOperation(param.getContainerName())
                .setOperationType(LogbookTypeProcess.ELIMINATION.name())
                .setTotalUnits(-1 * eliminationActionAccessionRegisterModel.getTotalUnits())
                .setTotalGots(-1 * eliminationActionAccessionRegisterModel.getTotalObjectGroups())
                .setTotalObjects(-1 * eliminationActionAccessionRegisterModel.getTotalObjects())
                .setObjectSize(-1 * eliminationActionAccessionRegisterModel.getTotalSize())
                .setCreationdate(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));

            RegisterValueDetailModel totalUnits =
                new RegisterValueDetailModel().setIngested(0)
                    .setDeleted(eliminationActionAccessionRegisterModel.getTotalUnits())
                    .setRemained(-1 * eliminationActionAccessionRegisterModel.getTotalUnits());
            RegisterValueDetailModel totalObjectsGroups =
                new RegisterValueDetailModel().setIngested(0)
                    .setDeleted(eliminationActionAccessionRegisterModel.getTotalObjectGroups())
                    .setRemained(-1 * eliminationActionAccessionRegisterModel.getTotalObjectGroups());
            RegisterValueDetailModel totalObjects =
                new RegisterValueDetailModel().setIngested(0)
                    .setDeleted(eliminationActionAccessionRegisterModel.getTotalObjects())
                    .setRemained(-1 * eliminationActionAccessionRegisterModel.getTotalObjects());
            RegisterValueDetailModel objectSize = new RegisterValueDetailModel().setIngested(0)
                .setDeleted(eliminationActionAccessionRegisterModel.getTotalSize())
                .setRemained(-1 * eliminationActionAccessionRegisterModel.getTotalSize());

            String updateDate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            GUID guid = GUIDFactory.newAccessionRegisterDetailGUID(tenantId);

            AccessionRegisterDetailModel accessionRegisterDetailModel = new AccessionRegisterDetailModel()
                .setId(guid.toString())
                .setOpc(param.getContainerName())
                .setOriginatingAgency(eliminationActionAccessionRegisterModel.getOriginatingAgency())
                .setTotalObjectsGroups(totalObjectsGroups)
                .setTotalUnits(totalUnits)
                .setTotalObjects(totalObjects)
                .setObjectSize(objectSize)
                .setOpi(eliminationActionAccessionRegisterModel.getOpi())
                .setOperationType(LogbookTypeProcess.ELIMINATION.name())
                .addEvent(registerValueEvent)
                .addOperationsId(param.getContainerName())
                .setTenant(tenantId)
                .setLastUpdate(updateDate);

            RequestResponse<AccessionRegisterDetailModel> resp =
                adminManagementClient.createOrUpdateAccessionRegister(accessionRegisterDetailModel);

            if (resp.getStatus() == javax.ws.rs.core.Response.Status.CONFLICT.getStatusCode()) {
                throw new ProcessingStatusException(
                    StatusCode.ALREADY_EXECUTED, "Plugin already executed");
            }

        } catch (AdminManagementClientServerException | AccessionRegisterException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "[Consistency ERROR] An error occurred during accession register update", e);
        }
    }

    private EliminationActionAccessionRegisterModel loadEliminationActionAccessionRegisterModel(WorkerParameters param)
        throws ProcessingStatusException {
        EliminationActionAccessionRegisterModel eliminationActionAccessionRegisterModel;
        try {
            eliminationActionAccessionRegisterModel =
                JsonHandler.getFromJsonNode(param.getObjectMetadata(), EliminationActionAccessionRegisterModel.class);

        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load accession register data", e);
        }
        return eliminationActionAccessionRegisterModel;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE;
    }
}
