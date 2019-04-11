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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;

import java.io.File;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;

/**
 * EvidenceAuditPrepare class
 */
public class EvidenceAuditPrepare extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditPrepare.class);

    private static final String EVIDENCE_AUDIT_LIST_OBJECT = "EVIDENCE_AUDIT_LIST_OBJECT";
    private static final String FIELDS_KEY = "$fields";
    private MetaDataClientFactory metaDataClientFactory;

    public EvidenceAuditPrepare() {
        this.metaDataClientFactory = MetaDataClientFactory.getInstance();
    }

    @VisibleForTesting
    EvidenceAuditPrepare(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT);
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

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createUnitScrollSplitIterator(client, select);

            StreamSupport.stream(scrollRequest, false).forEach(
                item -> {
                    ObjectNode itemUnit = createObjectNode();
                    itemUnit.put("id", item.get("#id").textValue());
                    itemUnit.put("metadaType", MetadataType.UNIT.name());

                    if (item.get("#object") != null) {
                        ObjectNode itemGot = createObjectNode();
                        itemGot.put("id", item.get("#object").textValue());
                        itemGot.put("metadaType", MetadataType.OBJECTGROUP.name());
                        saveItemToWorkSpace(itemGot, handlerIO);
                    }
                    saveItemToWorkSpace(itemUnit, handlerIO);
                });
            if (ScrollSpliteratorHelper.checkNumberOfResultQuery(itemStatus, scrollRequest.estimateSize())) {
                return new ItemStatus(EVIDENCE_AUDIT_LIST_OBJECT)
                    .setItemsStatus(EVIDENCE_AUDIT_LIST_OBJECT, itemStatus);
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

            String identifier = item.get("id").asText();
            file = handlerIO.getNewLocalFile(identifier);
            JsonHandler.writeAsFile(item, file);

            handlerIO.transferFileToWorkspace("Object" + "/" + identifier, file, true, false);

        } catch (ProcessingException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }

    }

    @Override public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException { /* Nothing todo */  }
}
