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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;

public class EvidenceAuditPrepare extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditPrepare.class);

    private static final String EVIDENCE_AUDIT_LIST_OBJECT = "EVIDENCE_AUDIT_LIST_OBJECT";
    private static final String FIELDS_KEY = "$fields";
    private static final String OPERATION = "operation";
    private static final String METADA_TYPE = "metadaType";
    private static final String OBJECT = "#object";
    private static final String ID = "id";

    private final MetaDataClientFactory metaDataClientFactory;
    private final StorageClientFactory storageClientFactory;

    public EvidenceAuditPrepare() {
        this.metaDataClientFactory = MetaDataClientFactory.getInstance();
        this.storageClientFactory = StorageClientFactory.getInstance();
    }

    @VisibleForTesting
    EvidenceAuditPrepare(MetaDataClientFactory metaDataClientFactory, StorageClientFactory storageClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT);
        JsonNode options = handlerIO.getJsonFromWorkspace("evidenceOptions");
        boolean correctiveAudit = options.get("correctiveOption").booleanValue();

        if (!correctiveAudit) {
            return handleEvidenceAudit(handlerIO, itemStatus);
        }

        String operationId = options.get(OPERATION).textValue();

        return handleRectificationAudit(handlerIO, itemStatus, operationId);
    }

    private ItemStatus handleRectificationAudit(HandlerIO handlerIO, ItemStatus itemStatus, String operationId)
        throws ProcessingException {
        InputStream inputStream = null;
        Response response = null;
        try (StorageClient client = storageClientFactory.getClient()) {
            String name = operationId + ".jsonl";
            response = client.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                name,
                DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog()
            );
            inputStream = (InputStream) response.getEntity();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            int skip = 3; // skip the three first lines of the report ( report head, report summary and report context)
            while (reader.ready()) {
                if (skip != 0) {
                    reader.readLine();
                    skip--;
                    continue;
                }
                String line = reader.readLine();
                EvidenceAuditReportLine pojo = JsonHandler.getFromString(line, EvidenceAuditReportLine.class);
                ObjectNode item = createObjectNode();
                item.put(ID, pojo.getIdentifier());
                item.put(METADA_TYPE, pojo.getObjectType().name());
                saveItemToWorkSpace(item, handlerIO);
            }
            reader.close();
        } catch (
            StorageServerClientException
            | StorageNotFoundException
            | IOException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.KO);
        } finally {
            StreamUtils.closeSilently(inputStream);
            consumeAnyEntityAndClose(response);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT).setItemsStatus(EVIDENCE_AUDIT_LIST_OBJECT, itemStatus);
    }

    private ItemStatus handleEvidenceAudit(HandlerIO handlerIO, ItemStatus itemStatus) throws ProcessingException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            JsonNode queryNode = handlerIO.getJsonFromWorkspace("query.json");

            SelectParserMultiple parser = new SelectParserMultiple();

            parser.parse(queryNode);

            SelectMultiQuery select = parser.getRequest();
            ObjectNode objectNode = createObjectNode();
            objectNode.put(VitamFieldsHelper.id(), 1);
            objectNode.put(VitamFieldsHelper.object(), 1);
            JsonNode projection = createObjectNode().set(FIELDS_KEY, objectNode);
            select.setProjection(projection);

            ScrollSpliterator<JsonNode> scrollRequest = new ScrollSpliterator<>(
                select,
                query -> {
                    try {
                        JsonNode jsonNode = client.selectUnits(query.getFinalSelect());
                        return RequestResponseOK.getFromJsonNode(jsonNode);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                },
                VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
                VitamConfiguration.getElasticSearchScrollLimit()
            );

            StreamSupport.stream(scrollRequest, false).forEach(item -> {
                ObjectNode itemUnit = createObjectNode();
                itemUnit.put(ID, item.get("#id").textValue());
                itemUnit.put(METADA_TYPE, MetadataType.UNIT.name());

                if (item.get(OBJECT) != null) {
                    ObjectNode itemGot = createObjectNode();
                    itemGot.put(ID, item.get(OBJECT).textValue());
                    itemGot.put(METADA_TYPE, MetadataType.OBJECTGROUP.name());
                    saveItemToWorkSpace(itemGot, handlerIO);
                }
                saveItemToWorkSpace(itemUnit, handlerIO);
            });
            if (ScrollSpliteratorHelper.checkNumberOfResultQuery(itemStatus, scrollRequest.estimateSize())) {
                return new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT).setItemsStatus(
                    EVIDENCE_AUDIT_LIST_OBJECT,
                    itemStatus
                );
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT).setItemsStatus(EVIDENCE_AUDIT_LIST_OBJECT, itemStatus);
    }

    private void saveItemToWorkSpace(JsonNode item, HandlerIO handlerIO) {
        File file = null;
        try {
            String identifier = item.get(ID).asText();
            file = handlerIO.getNewLocalFile(identifier);
            JsonHandler.writeAsFile(item, file);

            handlerIO.transferFileToWorkspace("Object" + "/" + identifier, file, true, false);
        } catch (ProcessingException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }
    }
}
