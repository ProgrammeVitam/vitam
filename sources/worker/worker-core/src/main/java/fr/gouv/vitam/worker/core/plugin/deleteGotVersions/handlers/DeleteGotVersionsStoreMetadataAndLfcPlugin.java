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
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.StoreMetaDataObjectGroupActionPlugin;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class DeleteGotVersionsStoreMetadataAndLfcPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        DeleteGotVersionsStoreMetadataAndLfcPlugin.class
    );

    private static final String PLUGIN_NAME = "STORE_METADATA_AND_LFC_WORKSPACE";

    private final StoreMetaDataObjectGroupActionPlugin storeMetaDataObjectGroupActionPlugin;

    public DeleteGotVersionsStoreMetadataAndLfcPlugin() {
        this(new StoreMetaDataObjectGroupActionPlugin());
    }

    @VisibleForTesting
    public DeleteGotVersionsStoreMetadataAndLfcPlugin(
        StoreMetaDataObjectGroupActionPlugin storeMetaDataObjectGroupActionPlugin
    ) {
        this.storeMetaDataObjectGroupActionPlugin = storeMetaDataObjectGroupActionPlugin;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("Starting DeleteGotVersionsStoragePlugin... ");
        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<String> gotIds = params.getObjectNameList();
        List<JsonNode> objectGroupToDeleteReportEntriesNodes = params.getObjectMetadataList();
        List<String> gotsToStore = new ArrayList<>();
        try {
            for (int i = 0; i < objectGroupToDeleteReportEntriesNodes.size(); i++) {
                StatusCode statusCode = OK;
                List<ObjectGroupToDeleteReportEntry> objectGroupToDeleteReportEntries = getFromJsonNode(
                    objectGroupToDeleteReportEntriesNodes.get(i),
                    new TypeReference<>() {}
                );

                if (objectGroupToDeleteReportEntries.stream().anyMatch(elmt -> elmt.getStatus().equals(WARNING))) {
                    statusCode = WARNING;
                }
                if (objectGroupToDeleteReportEntries.stream().anyMatch(elmt -> elmt.getStatus().equals(OK))) {
                    gotsToStore.add(gotIds.get(i));
                }
                itemStatuses.add(buildItemStatus(PLUGIN_NAME, statusCode));
            }

            storeMetaDataObjectGroupActionPlugin.storeDocumentsWithLfc(params, handler, gotsToStore);
        } catch (VitamException e) {
            final String errorMsg = "No objectGroupToDelete entries found for Object group in distribution file.";
            ObjectNode error = createObjectNode().put("error", errorMsg);
            itemStatuses.add(buildItemStatus(PLUGIN_NAME, FATAL, error));
        }
        return itemStatuses;
    }
}
