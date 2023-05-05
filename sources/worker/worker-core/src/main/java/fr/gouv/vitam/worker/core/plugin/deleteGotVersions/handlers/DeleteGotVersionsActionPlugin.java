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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.entry.ObjectGroupToDeleteReportEntry;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModelCustomized;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNodeList;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class DeleteGotVersionsActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DeleteGotVersionsActionPlugin.class);

    private static final String PLUGIN_NAME = "DELETE_GOT_VERSIONS_ACTION";

    private final MetaDataClientFactory metaDataClientFactory;

    public DeleteGotVersionsActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    public DeleteGotVersionsActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("Starting DeleteGotVersionsActionPlugin... ");
        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<String> gotIds = params.getObjectNameList();
        List<JsonNode> objectGroupToDeleteReportEntriesNodes = params.getObjectMetadataList();
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            DeleteGotVersionsRequest deleteGotVersionsRequest = loadRequest(handler);
            RequestResponse<JsonNode> objectGroupByIdsResponse = metaDataClient.getObjectGroupsByIdsRaw(gotIds);
            if (!objectGroupByIdsResponse.isOk()) {
                final String errorMsg = "A problem occured when getting ObjectGroup by ids ";
                final ObjectNode error = createObjectNode().put("error", errorMsg);
                gotIds.forEach(gotId -> itemStatuses.add(buildItemStatus(PLUGIN_NAME, FATAL, error)));
                return itemStatuses;
            }
            List<DbObjectGroupModel> results =
                getFromJsonNodeList(((RequestResponseOK<JsonNode>) objectGroupByIdsResponse).getResults(),
                    new TypeReference<>() {
                    });

            if (results.size() != gotIds.size()) {
                final String errorMsg =
                    "The size of Object Groups readed from database is not coherant with object Groups in distribution file";
                final ObjectNode error = createObjectNode().put("error", errorMsg);
                gotIds.forEach(gotId -> itemStatuses.add(buildItemStatus(PLUGIN_NAME, FATAL, error)));
                return itemStatuses;
            }

            for (int i = 0; i < gotIds.size(); i++) {
                String objectId = gotIds.get(i);
                List<ObjectGroupToDeleteReportEntry> objectGroupToDeleteReportEntries =
                    objectGroupToDeleteReportEntriesNodes != null && !objectGroupToDeleteReportEntriesNodes.isEmpty() ?
                        getFromJsonNode(objectGroupToDeleteReportEntriesNodes.get(i), new TypeReference<>() {
                        }) : null;
                if (objectGroupToDeleteReportEntries == null || objectGroupToDeleteReportEntries.isEmpty()) {
                    throw new IllegalStateException(
                        String.format("No objectGroup entries found for Object group %s in distribution file.",
                            objectId));
                }
                DbObjectGroupModel objectGroupToUpdate =
                    results.stream().filter(elmt -> elmt.getId().equals(objectId)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No objectGroup to update found in Database."));

                itemStatuses
                    .add(processDeleteGotVersions(objectId, objectGroupToUpdate, objectGroupToDeleteReportEntries,
                        deleteGotVersionsRequest, handler.getContainerName()));
            }
        } catch (ProcessingException | InvalidParseOperationException | VitamClientException e) {
            final String errorMsg = "A problem occured while processing the got versions delete.";
            ObjectNode error = createObjectNode().put("error", errorMsg);
            itemStatuses.add(buildItemStatus(PLUGIN_NAME, FATAL, error));
        }
        return itemStatuses;
    }

    private ItemStatus processDeleteGotVersions(String objectGroupId, DbObjectGroupModel dbObjectGroupModel,
        List<ObjectGroupToDeleteReportEntry> objectGroupToDeleteReportEntries,
        DeleteGotVersionsRequest deleteGotVersionsRequest, String containerName) {
        StatusCode status = OK;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            if (objectGroupToDeleteReportEntries.stream().anyMatch(elmt -> elmt.getStatus().equals(WARNING))) {
                status = WARNING;
            }

            List<ObjectGroupToDeleteReportEntry> okEntries =
                objectGroupToDeleteReportEntries.stream().filter(elmt -> elmt.getStatus().equals(OK)).collect(
                    Collectors.toList());
            if (!okEntries.isEmpty()) {
                List<DbQualifiersModel> qualifiers = dbObjectGroupModel.getQualifiers();

                Optional<DbQualifiersModel> optionalQualifierToUpdate = qualifiers.stream()
                    .filter(elmt -> elmt.getQualifier().equals(deleteGotVersionsRequest.getUsageName()))
                    .findFirst();
                if (optionalQualifierToUpdate.isEmpty()) {
                    LOGGER.warn(String.format("No qualifier of Object group matches with %s usage. Already deleted ?",
                        deleteGotVersionsRequest.getUsageName()));
                    return buildItemStatus(PLUGIN_NAME, OK);
                }

                DbQualifiersModel qualifierToUpdate = optionalQualifierToUpdate.get();
                if (qualifierToUpdate.getVersions() == null || qualifierToUpdate.getVersions().isEmpty()) {
                    LOGGER
                        .warn(String.format("No versions associated to the qualifier of Object group for the %s usage",
                            deleteGotVersionsRequest.getUsageName()));
                    return buildItemStatus(PLUGIN_NAME, OK);
                }

                List<VersionsModelCustomized> versionsToDelete =
                    okEntries.stream().map(ObjectGroupToDeleteReportEntry::getDeletedVersions).flatMap(List::stream)
                        .collect(Collectors.toList());

                if (!versionsToDelete.isEmpty()) {
                    List<String> dataObjectVersionsToDelete =
                        versionsToDelete.stream().map(VersionsModelCustomized::getDataObjectVersion)
                            .collect(Collectors.toList());
                    qualifierToUpdate.getVersions()
                        .removeAll(qualifierToUpdate.getVersions().stream()
                            .filter(elmt -> dataObjectVersionsToDelete.contains(elmt.getDataObjectVersion())).collect(
                                Collectors.toList()));
                    qualifierToUpdate.setNbc(qualifierToUpdate.getVersions().size());

                    final int totalNbc = qualifiers.stream()
                        .mapToInt(DbQualifiersModel::getNbc)
                        .sum();

                   qualifiers = qualifiers.stream().filter(qualifier -> qualifier.getNbc() > 0).collect(Collectors.toList());

                    Map<String, JsonNode> action = new HashMap<>();
                    action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));
                    SetAction setQualifier = new SetAction(action);

                    UpdateMultiQuery query = new UpdateMultiQuery();
                    query.addHintFilter(OBJECTGROUPS.exactToken());
                    query.addActions(
                        UpdateActionHelper.push(VitamFieldsHelper.operations(), containerName),
                        UpdateActionHelper.set(VitamFieldsHelper.nbobjects(), totalNbc),
                        setQualifier
                    );

                    metaDataClient.updateObjectGroupById(query.getFinalUpdate(), objectGroupId);
                }
            }
            return buildItemStatus(PLUGIN_NAME, status);
        } catch (InvalidParseOperationException | InvalidCreateOperationException |
                 MetaDataClientServerException | MetaDataExecutionException e) {
            LOGGER.error(String.format("Delete got versions action failed with status [%s]", FATAL), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, FATAL, error);
        }
    }

    private DeleteGotVersionsRequest loadRequest(HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException {
        JsonNode inputRequest = handler.getJsonFromWorkspace("deleteGotVersionsRequest");
        return getFromJsonNode(inputRequest, DeleteGotVersionsRequest.class);
    }
}
