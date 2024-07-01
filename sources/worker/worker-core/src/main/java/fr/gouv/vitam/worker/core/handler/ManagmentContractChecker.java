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

import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ManagementContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyNotFoundException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyReferentOfferException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyUtils;
import fr.gouv.vitam.worker.core.handler.CheckIngestContractActionHandler.CheckIngestContractStatus;

import java.util.List;

public class ManagmentContractChecker {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ManagmentContractChecker.class);

    private String managementContractId;
    private AdminManagementClientFactory adminManagementClientFactory;
    private StorageClientFactory storageClientFactory;

    public ManagmentContractChecker(
        String managementContractId,
        AdminManagementClientFactory adminManagementClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.managementContractId = managementContractId;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    public CheckIngestContractStatus check() {
        try (
            AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient()
        ) {
            RequestResponse<ManagementContractModel> ManagementContractResponse =
                adminManagementClient.findManagementContractsByID(managementContractId);
            if (ManagementContractResponse.isOk()) {
                List<ManagementContractModel> results =
                    ((RequestResponseOK<ManagementContractModel>) ManagementContractResponse).getResults();
                if (results.isEmpty()) {
                    LOGGER.error(
                        "CheckContract : The Management Contract " + managementContractId + "  not found in database"
                    );
                    return CheckIngestContractStatus.MANAGEMENT_CONTRACT_UNKNOWN;
                } else {
                    final ManagementContractModel managementContract = Iterables.getFirst(results, null);

                    if (!ActivationStatus.ACTIVE.equals(managementContract.getStatus())) {
                        LOGGER.error(
                            "CheckContract : The Management Contract " + managementContract + "  is not activated"
                        );
                        return CheckIngestContractStatus.MANAGEMENT_CONTRACT_INACTIVE;
                    }

                    if (managementContract.getStorage() != null) {
                        RequestResponse<StorageStrategy> strategiesResponse = storageClient.getStorageStrategies();
                        if (!strategiesResponse.isOk()) {
                            LOGGER.error(strategiesResponse.toString());
                            throw new StorageServerClientException("Exception while retrieving storage strategies");
                        }
                        List<StorageStrategy> strategies =
                            ((RequestResponseOK<StorageStrategy>) strategiesResponse).getResults();

                        try {
                            if (managementContract.getStorage().getObjectGroupStrategy() != null) {
                                StorageStrategyUtils.checkStrategy(
                                    managementContract.getStorage().getObjectGroupStrategy(),
                                    strategies,
                                    ManagementContract.OBJECTGROUP_STRATEGY,
                                    true
                                );
                            }
                            if (managementContract.getStorage().getUnitStrategy() != null) {
                                StorageStrategyUtils.checkStrategy(
                                    managementContract.getStorage().getUnitStrategy(),
                                    strategies,
                                    ManagementContract.UNIT_STRATEGY,
                                    true
                                );
                            }

                            if (managementContract.getStorage().getObjectStrategy() != null) {
                                StorageStrategyUtils.checkStrategy(
                                    managementContract.getStorage().getObjectStrategy(),
                                    strategies,
                                    ManagementContract.OBJECT_STRATEGY,
                                    false
                                );
                            }
                        } catch (StorageStrategyNotFoundException | StorageStrategyReferentOfferException exc) {
                            LOGGER.error(exc);
                            return CheckIngestContractStatus.MANAGEMENT_CONTRACT_INVALID;
                        }
                    }

                    return CheckIngestContractStatus.OK;
                }
            }
        } catch (ReferentialNotFoundException e) {
            LOGGER.error("Management Contract not found :", e);
            return CheckIngestContractStatus.MANAGEMENT_CONTRACT_UNKNOWN;
        } catch (
            AdminManagementClientServerException | InvalidParseOperationException | StorageServerClientException e
        ) {
            LOGGER.error("Fatal check error :", e);
            return CheckIngestContractStatus.FATAL;
        }

        return CheckIngestContractStatus.KO;
    }
}
