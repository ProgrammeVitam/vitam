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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common implementation of compute graph for UNIT and GOT
 */
public abstract class AbstractGraphComputePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractGraphComputePlugin.class);

    private final MetaDataClientFactory metaDataClientFactory;

    public AbstractGraphComputePlugin() {
        this.metaDataClientFactory = MetaDataClientFactory.getInstance();
    }

    @VisibleForTesting
    public AbstractGraphComputePlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(getPluginKeyName());

        List<ItemStatus> aggregateItemStatus = new ArrayList<>();
        List<String> items = workerParameters.getObjectNameList();
        // Transform to Set? (Remove duplicates)
        Set<String> ids = new HashSet<>(items);

        int initialSize = ids.size();
        int finalSize = 0;
        GraphComputeResponse graphComputeResponse = null;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            graphComputeResponse = metaDataClient.computeGraph(getGraphComputeAction(), ids);

            switch (getGraphComputeAction()) {
                case UNIT:
                    finalSize = graphComputeResponse.getUnitCount();
                    break;
                case OBJECTGROUP:
                    finalSize = graphComputeResponse.getGotCount();
                    break;
                default:
                    throw new IllegalStateException("Unexpected graph compute action " + getGraphComputeAction());
            }
        } catch (VitamClientException e) {
            LOGGER.error("Processing exception", e);
            itemStatus.increment(StatusCode.FATAL);
        } finally {
            handler.setCurrentObjectId(null);
        }

        int fatal = Math.abs(initialSize - finalSize);

        // The ko should be not blocking
        if (0 == fatal) {
            itemStatus.increment(StatusCode.OK, finalSize);
        } else {
            if (null != graphComputeResponse && !Strings.isNullOrEmpty(graphComputeResponse.getErrorMessage())) {
                ObjectNode infoNode = JsonHandler.createObjectNode();
                infoNode.put(SedaConstants.EV_DET_TECH_DATA, graphComputeResponse.getErrorMessage());
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
            }

            // When fatal occurs, we have the index from where we restart workflow distribution after resolving FATAL cause
            itemStatus.increment(StatusCode.OK, finalSize).increment(StatusCode.FATAL, fatal);
        }
        aggregateItemStatus.add(new ItemStatus(getPluginKeyName()).setItemsStatus(getPluginKeyName(), itemStatus));
        return aggregateItemStatus;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}

    abstract GraphComputeResponse.GraphComputeAction getGraphComputeAction();

    abstract String getPluginKeyName();
}
