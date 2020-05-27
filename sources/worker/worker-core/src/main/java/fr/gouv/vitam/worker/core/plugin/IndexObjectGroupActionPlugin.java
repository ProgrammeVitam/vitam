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
package fr.gouv.vitam.worker.core.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.StepAlreadyExecutedException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.NBCHILD;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.QUALIFIERS;

/**
 * IndexObjectGroupAction Plugin
 */
public class IndexObjectGroupActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionPlugin.class);
    private static final String OG_INDEXATION = "OG_INDEXATION";
    private static final String AGENCY_CHECK = "AGENCY_CHECK";

    private static final int OG_INPUT_RANK = 0;
    private final MetaDataClientFactory metaDataClientFactory;

    public IndexObjectGroupActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    public IndexObjectGroupActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {

        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            List<ItemStatus> aggregateItemStatus = new ArrayList<>();
            List<JsonNode> objectGroups = new ArrayList<>();
            for (String objectId : workerParameters.getObjectNameList()) {

                workerParameters.setObjectName(objectId);
                handler.setCurrentObjectId(objectId);

                checkMandatoryParameters(workerParameters);

                final ItemStatus itemStatus = new ItemStatus(OG_INDEXATION);

                try {
                    checkMandatoryIOParameter(handler);

                    JsonNode objectNode = indexObjectGroup(handler, workerParameters, itemStatus);
                    if (objectNode != null) {
                        objectGroups.add(objectNode);
                    }

                } catch (final StepAlreadyExecutedException e) {
                    LOGGER.warn(e);
                    // FIXME (US 5769) : StepAlreadyExecutedException not thrown
                    itemStatus.increment(StatusCode.ALREADY_EXECUTED);
                } catch (final ProcessingInternalServerException exc) {
                    LOGGER.error(exc);
                    itemStatus.increment(StatusCode.FATAL);
                } catch (final ProcessingException e) {
                    LOGGER.error(e);
                    itemStatus.increment(StatusCode.KO);
                }

                if (StatusCode.UNKNOWN.equals(itemStatus.getGlobalStatus())) {
                    itemStatus.increment(StatusCode.WARNING);
                }

                aggregateItemStatus.add(new ItemStatus(OG_INDEXATION).setItemsStatus(OG_INDEXATION, itemStatus));
            }

            if (!objectGroups.isEmpty()) {
                try {
                    metadataClient.insertObjectGroups(objectGroups);
                } catch (final MetaDataException | InvalidParseOperationException e) {
                    LOGGER.error(e);

                    for (ItemStatus itemStatus : aggregateItemStatus) {
                        itemStatus.increment(StatusCode.FATAL);
                    }

                }
            }
            return aggregateItemStatus;

        } finally {
            handler.setCurrentObjectId(null);
        }
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO param) {
        throw new RuntimeException();
    }


    /**
     * The function is used for retrieving ObjectGroup in workspace and use metadata client to index ObjectGroup
     *
     * @param params work parameters
     * @param itemStatus item status
     * @throws ProcessingException when error in execution
     */
    private ObjectNode indexObjectGroup(HandlerIO handlerIO, WorkerParameters params, ItemStatus itemStatus)
        throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);

        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            final ObjectNode json = (ObjectNode) handlerIO.getInput(OG_INPUT_RANK);

            return handleExistingObjectGroup(json, metadataClient, params, itemStatus);
        } catch (final MetaDataException | VitamClientException e) {
            throw new ProcessingInternalServerException("Metadata Server Error", e);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingException("Json wrong format", e);
        }
    }

    private ObjectNode handleExistingObjectGroup(ObjectNode json, MetaDataClient metadataClient,
        WorkerParameters params, ItemStatus itemStatus)
        throws MetaDataExecutionException,
        MetaDataClientServerException, InvalidParseOperationException,
        InvalidCreateOperationException, VitamClientException {
        JsonNode work = json.remove(SedaConstants.PREFIX_WORK);
        if (work != null && work.get(SedaConstants.TAG_DATA_OBJECT_GROUP_EXISTING_REFERENCEID) != null &&
            !work.get(SedaConstants.TAG_DATA_OBJECT_GROUP_EXISTING_REFERENCEID).asText().isEmpty()) {
            // this means object group is existing, so we will update and not insert
            String existingOg = work.get(SedaConstants.TAG_DATA_OBJECT_GROUP_EXISTING_REFERENCEID).asText();
            RequestResponse<JsonNode> requestResponse = metadataClient.getObjectGroupByIdRaw(existingOg);
            JsonNode ogInDB = null;
            if (requestResponse.isOk()) {
                ogInDB = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            if (ogInDB != null) {
                ObjectNode infoNode = JsonHandler.createObjectNode();
                ArrayNode originQualifiers = (ArrayNode) ogInDB.get(QUALIFIERS);
                ArrayNode newQualifiers = (ArrayNode) json.get(QUALIFIERS);
                // lets create an update query
                UpdateMultiQuery query = new UpdateMultiQuery();
                query.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
                JsonNode newUpdateQuery =
                    query
                        .addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), params.getContainerName()),
                            generateQualifiersUpdate(originQualifiers, newQualifiers, infoNode))
                        .getFinalUpdate();

                metadataClient.updateObjectGroupById(newUpdateQuery, ogInDB.get(ID).asText());

                String evDevDetailData = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evDevDetailData);
                itemStatus.increment(StatusCode.OK);
                return null;
            }
        }

        final InsertMultiQuery insertRequest = new InsertMultiQuery().addData(json);
        ObjectNode finalInsert = insertRequest.getFinalInsert();
        itemStatus.increment(StatusCode.OK);

        return finalInsert;
    }

    private Action generateQualifiersUpdate(ArrayNode originQualifiers, ArrayNode newQualifiers, ObjectNode infoNode)
        throws InvalidCreateOperationException {
        ArrayNode finalQualifiers = originQualifiers.deepCopy();
        Map<String, JsonNode> action = new HashMap<>();
        HashMap<String, ArrayNode> listOrigin = new HashMap<String, ArrayNode>();
        ObjectNode updatedQualifiers = JsonHandler.createObjectNode();
        for (int i = 0; i < originQualifiers.size(); i++) {
            JsonNode qualifierNode = originQualifiers.get(i);
            listOrigin.put(qualifierNode.get(SedaConstants.PREFIX_QUALIFIER).asText(),
                (ArrayNode) qualifierNode.get(SedaConstants.TAG_VERSIONS));
        }
        for (int i = 0; i < newQualifiers.size(); i++) {
            JsonNode qualifierNode = newQualifiers.get(i);
            String qualifType = qualifierNode.get(SedaConstants.PREFIX_QUALIFIER).asText();
            if (listOrigin.containsKey(qualifType)) {
                for (int j = 0; j < finalQualifiers.size(); j++) {
                    ObjectNode current = (ObjectNode) finalQualifiers.get(j);
                    if (current.get(SedaConstants.PREFIX_QUALIFIER).asText()
                        .equals(qualifierNode.get(SedaConstants.PREFIX_QUALIFIER).asText())) {
                        int nbCopy = current.get(NBCHILD).asInt() + 1;
                        current.put(NBCHILD, current.get(NBCHILD).asInt() + 1);
                        ArrayNode currentArray = (ArrayNode) current.get(SedaConstants.TAG_VERSIONS);
                        ((ObjectNode) qualifierNode.get(SedaConstants.TAG_VERSIONS).get(0)).put(
                            SedaConstants.TAG_DO_VERSION,
                            current.get(SedaConstants.PREFIX_QUALIFIER).asText() + "_" + nbCopy);
                        currentArray.add(qualifierNode.get(SedaConstants.TAG_VERSIONS).get(0));

                        IntNode version = new IntNode(nbCopy);
                        updatedQualifiers.set(current.get(SedaConstants.PREFIX_QUALIFIER).asText(), version);
                        break;
                    }
                }
            } else {
                finalQualifiers.add(qualifierNode);

                updatedQualifiers.set(qualifType, new IntNode(1));
            }
        }
        infoNode.set("updatedQualifiers", updatedQualifiers);
        action.put(PROJECTIONARGS.QUALIFIERS.exactToken(), finalQualifiers);
        return new SetAction(action);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add objectGroup.json add input and check it

    }

}
