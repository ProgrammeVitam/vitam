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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

/**
 * ScrollSpliteratorHelper class
 */
public class ScrollSpliteratorHelper {

    /***
     * Create  units  ScrollSpliterator from a query that can iterate millions  of units
     * @param client metadataClient
     * @param selectMultiQuery query
     * @return the ScrollSpliterator
     */
    public static ScrollSpliterator<JsonNode> createUnitScrollSplitIterator(
        final MetaDataClient client,
        final SelectMultiQuery selectMultiQuery
    ) {
        return createUnitScrollSplitIterator(
            client,
            selectMultiQuery,
            VitamConfiguration.getElasticSearchScrollLimit()
        );
    }

    /***
     * Create  units  ScrollSpliterator from a query that can iterate millions  of units with given bachSize
     * @param client metadataClient
     * @param selectMultiQuery query
     * @param bachSize bachSize
     * @return the ScrollSpliterator
     */
    public static ScrollSpliterator<JsonNode> createUnitScrollSplitIterator(
        final MetaDataClient client,
        final SelectMultiQuery selectMultiQuery,
        int bachSize
    ) {
        return new ScrollSpliterator<>(
            selectMultiQuery,
            query -> {
                try {
                    JsonNode jsonNode = client.selectUnits(query.getFinalSelect());
                    return RequestResponseOK.getFromJsonNode(jsonNode);
                } catch (
                    MetaDataExecutionException
                    | MetaDataDocumentSizeException
                    | MetaDataClientServerException
                    | InvalidParseOperationException e
                ) {
                    throw new IllegalStateException(e);
                }
            },
            VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            bachSize
        );
    }

    /***
     * Create  objectGroups   ScrollSpliterator from a query that can iterate millions  of objectsGroups with given bachSize
     * @param client metadataClient
     * @param selectMultiQuery query
     * @param bachSize bachSize
     * @return the ScrollSpliterator
     */
    public static ScrollSpliterator<JsonNode> createObjectGroupScrollSplitIterator(
        final MetaDataClient client,
        final SelectMultiQuery selectMultiQuery,
        int bachSize
    ) {
        return new ScrollSpliterator<>(
            selectMultiQuery,
            query -> {
                try {
                    JsonNode jsonNode = client.selectObjectGroups(query.getFinalSelect());
                    return RequestResponseOK.getFromJsonNode(jsonNode);
                } catch (
                    MetaDataExecutionException
                    | MetaDataDocumentSizeException
                    | MetaDataClientServerException
                    | InvalidParseOperationException e
                ) {
                    throw new IllegalStateException(e);
                }
            },
            VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            bachSize
        );
    }

    /***
     * Create  objectGroups   ScrollSpliterator from a query that can iterate millions  of objectsGroups with default bachSize
     * @param client metadataClient
     * @param selectMultiQuery query
     * @return the ScrollSpliterator
     */
    public static ScrollSpliterator<JsonNode> createObjectGroupScrollSplitIterator(
        final MetaDataClient client,
        final SelectMultiQuery selectMultiQuery
    ) {
        return createObjectGroupScrollSplitIterator(
            client,
            selectMultiQuery,
            VitamConfiguration.getElasticSearchScrollLimit()
        );
    }

    public static ScrollSpliterator<JsonNode> getUnitWithInheritedRulesScrollSpliterator(
        SelectMultiQuery request,
        MetaDataClient client
    ) {
        return new ScrollSpliterator<>(
            request,
            query -> {
                try {
                    JsonNode jsonNode = client.selectUnitsWithInheritedRules(query.getFinalSelect());
                    return RequestResponseOK.getFromJsonNode(jsonNode);
                } catch (
                    InvalidParseOperationException
                    | MetaDataExecutionException
                    | MetaDataDocumentSizeException
                    | MetaDataClientServerException e
                ) {
                    throw new IllegalStateException(e);
                }
            },
            VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            VitamConfiguration.getElasticSearchScrollLimit()
        );
    }

    /**
     * Check number of result
     *
     * @param itemStatus itemStatus
     * @param total total of elements
     * @return boolean
     */
    public static boolean checkNumberOfResultQuery(ItemStatus itemStatus, long total) {
        if (total == 0) {
            itemStatus.increment(StatusCode.KO);
            ObjectNode infoNode = createObjectNode();
            infoNode.put("Reason", "the DSL query has no result");
            String evdev = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evdev);
            return true;
        }
        return false;
    }
}
