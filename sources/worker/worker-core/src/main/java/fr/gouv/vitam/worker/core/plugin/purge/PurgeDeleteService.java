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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import joptsimple.internal.Strings;

import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.pull;

/**
 * PurgeDeleteService class
 */
public class PurgeDeleteService {

    private final StorageClientFactory storageClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @VisibleForTesting
    PurgeDeleteService(
        StorageClientFactory storageClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory
    ) {
        this.storageClientFactory = storageClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
    }

    public PurgeDeleteService() {
        this(
            StorageClientFactory.getInstance(),
            MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance()
        );
    }

    public void deleteObjects(Map<String, String> objectsGuidsWithStrategies) throws StorageServerClientException {
        storageDelete(objectsGuidsWithStrategies, DataCategory.OBJECT, Strings.EMPTY);
    }

    public void deleteObjectGroups(Map<String, String> objectGroupsGuidsWithStrategies)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataClientServerException, StorageServerClientException, LogbookClientBadRequestException, LogbookClientServerException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            logbookLifeCyclesClient.deleteLifecycleObjectGroupBulk(objectGroupsGuidsWithStrategies.keySet());
        }

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            metaDataClient.deleteObjectGroupBulk(objectGroupsGuidsWithStrategies.keySet());
        }

        storageDelete(objectGroupsGuidsWithStrategies, DataCategory.OBJECTGROUP, ".json");
    }

    public void deleteUnits(Map<String, String> unitsGuidsWithStrategies)
        throws MetaDataExecutionException, MetaDataClientServerException, StorageServerClientException, LogbookClientBadRequestException, LogbookClientServerException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            logbookLifeCyclesClient.deleteLifecycleUnitsBulk(unitsGuidsWithStrategies.keySet());
        }

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            metaDataClient.deleteUnitsBulk(unitsGuidsWithStrategies.keySet());
        }

        storageDelete(unitsGuidsWithStrategies, DataCategory.UNIT, ".json");
    }

    private void storageDelete(Map<String, String> idsWithStrategies, DataCategory dataCategory, String fileExtension)
        throws StorageServerClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            for (Map.Entry<String, String> idWithStrategy : idsWithStrategies.entrySet()) {
                storageClient.delete(idWithStrategy.getValue(), dataCategory, idWithStrategy.getKey() + fileExtension);
            }
        }
    }

    public void detachObjectGroupFromDeleteParentUnits(String objectGroupId, Set<String> parentUnitsToRemove)
        throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            updateMultiQuery.addActions(
                pull(VitamFieldsHelper.unitups(), parentUnitsToRemove.toArray(new String[0])),
                add(VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId())
            );

            metaDataClient.updateObjectGroupById(updateMultiQuery.getFinalUpdate(), objectGroupId);
        } catch (
            MetaDataClientServerException
            | MetaDataExecutionException
            | InvalidParseOperationException
            | InvalidCreateOperationException e
        ) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "An error occurred during object group detachment",
                e
            );
        }
    }
}
