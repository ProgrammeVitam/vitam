/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.model.DataType;
import fr.gouv.vitam.common.database.parser.query.helper.CheckSpecifiedFieldHelper;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.UpdatePermissionException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Check update permissions.
 */
public class UnitMetadataUpdateCheckPermission extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    /**
     * UNIT_METADATA_UPDATE_CHECK_PERMISSION
     */
    private static final String UNIT_METADATA_UPDATE_CHECK_PERMISSION = "UNIT_METADATA_UPDATE_CHECK_PERMISSION";

    /**
     * CHECK_CONTRACT_RANK
     */
    private static final int CHECK_CONTRACT_RANK = 0;

    /**
     * CHECK_MANAGEMENT_RANK
     */
    private static final int CHECK_MANAGEMENT_RANK = 1;

    /**
     * CHECK_GRAPH_RANK
     */
    private static final int CHECK_GRAPH_RANK = 2;

    /**
     * CHECK_MDD_RANK
     */
    private static final int CHECK_MDD_RANK = 3;

    /**
     * Error messages.
     */
    private static final String ACCESS_CONTRACT_NOT_FOUND_EXCEPTION = "Access contract not found";

    /**
     * AdminManagementClientFactory
     */
    private AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor.
     */
    public UnitMetadataUpdateCheckPermission() {
        this(AdminManagementClientFactory.getInstance());
    }

    /**
     * Constructor.
     * @param adminManagementClientFactory
     */
    @VisibleForTesting
    public UnitMetadataUpdateCheckPermission(
        AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    /**
     * Execute an action
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     */
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {

        checkMandatoryParameters(param);
        final ItemStatus itemStatus = new ItemStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION);

        final boolean shouldCheckContract = Boolean.valueOf((String) handler.getInput(CHECK_CONTRACT_RANK));
        final boolean shouldCheckManagement = Boolean.valueOf((String) handler.getInput(CHECK_MANAGEMENT_RANK));
        final boolean shouldCheckGraph = Boolean.valueOf((String) handler.getInput(CHECK_GRAPH_RANK));
        final boolean shouldCheckMDD = Boolean.valueOf((String) handler.getInput(CHECK_MDD_RANK));
        JsonNode initialQuery = handler.getJsonFromWorkspace("query.json");

        JsonNode queryActions = handler.getJsonFromWorkspace("actions.json");

        try {
            // Check contract permissions
            if (shouldCheckContract) {
                handler.getInput().clear();
                final String contractId = VitamThreadUtils.getVitamSession().getContractId();
                if (!StringUtils.isBlank(contractId)) {
                    try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
                        RequestResponse<AccessContractModel> contractResponse =
                            adminManagementClient.findAccessContractsByID(contractId);

                        if (contractResponse.isOk()) {
                            AccessContractModel contract =
                                ((RequestResponseOK<AccessContractModel>) contractResponse).getFirstResult();

                            if (null == contract) {
                                LOGGER.error(ACCESS_CONTRACT_NOT_FOUND_EXCEPTION);
                                throw new ProcessingException(ACCESS_CONTRACT_NOT_FOUND_EXCEPTION);
                            } else if (!contract.getWritingPermission()) {
                                throw new UpdatePermissionException(
                                    VitamCode.UPDATE_UNIT_PERMISSION.name());
                            }

                            // Check management data
                            if (shouldCheckManagement) {
                                if ((!JsonHandler.isNullOrEmpty(queryActions)|| CheckSpecifiedFieldHelper
                                    .containsSpecifiedField(initialQuery, DataType.MANAGEMENT)) &&
                                    BooleanUtils.isTrue(contract.getWritingRestrictedDesc())) {
                                    LOGGER
                                        .error(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_MANAGEMENT_UNAUTHORIZED.name());
                                    throw new UpdatePermissionException(
                                        VitamCode.UPDATE_UNIT_DESC_PERMISSION.name());
                                }
                            }
                        } else {
                            throw new ProcessingException(ACCESS_CONTRACT_NOT_FOUND_EXCEPTION);
                        }
                    } catch (final VitamException e) {
                        throw new ProcessingException(e);
                    }
                }
            }
            // Check graph data
            if (shouldCheckGraph && (queryActions == null && CheckSpecifiedFieldHelper.containsSpecifiedField(initialQuery, DataType.GRAPH))) {
                itemStatus.increment(StatusCode.KO);
                itemStatus.setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_GRAPH_UNAUTHORIZED.name());
                return new ItemStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION)
                    .setItemsStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION, itemStatus);
            }
            // Check MDD data
            if (shouldCheckMDD && (queryActions == null && CheckSpecifiedFieldHelper.containsSpecifiedField(initialQuery, DataType.MDD))) {
                itemStatus.increment(StatusCode.KO);
                itemStatus.setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_INTERNAL_DATA_UNAUTHORIZED.name());
                return new ItemStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION)
                    .setItemsStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION, itemStatus);
            }
            // Check temporary data
            if (CheckSpecifiedFieldHelper.containsSpecifiedField(initialQuery, DataType.TEMPORARY)) {
                itemStatus.increment(StatusCode.KO);
                itemStatus.setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_INTERNAL_DATA_UNAUTHORIZED.name());
                return new ItemStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION)
                    .setItemsStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION, itemStatus);
            }
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION)
            .setItemsStatus(UNIT_METADATA_UPDATE_CHECK_PERMISSION, itemStatus);
    }


    /**
     * Check mandatory parameter
     * @param handler input output list
     * @throws ProcessingException when handler io is not complete
     */
    @Override public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing
    }
}
