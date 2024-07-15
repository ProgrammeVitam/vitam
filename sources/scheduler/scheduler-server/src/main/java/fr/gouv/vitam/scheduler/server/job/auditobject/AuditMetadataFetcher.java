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

package fr.gouv.vitam.scheduler.server.job.auditobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

import java.util.Objects;

public class AuditMetadataFetcher {

    private static final long THRESHOLD = VitamConfiguration.getDistributionThreshold();

    private final MetaDataClientFactory metaDataClientFactory;

    public AuditMetadataFetcher(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    public String getLastUpdateDateFromLastUnitToAudit(int operationsDelayInMinutes, String lastAuditData) {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            SelectMultiQuery selectMultiQuery = buildFilterESQuery(operationsDelayInMinutes, lastAuditData);
            DatabaseCursor hits;
            JsonNode result;
            String scrollId;
            String lastUpdateDate = null;
            int size = 0;
            do {
                result = client.selectUnits(selectMultiQuery.getFinalSelect());
                RequestResponseOK<JsonNode> requestResponse = RequestResponseOK.getFromJsonNode(result, JsonNode.class);
                hits = requestResponse.getHits();
                scrollId = hits.getScrollId();
                selectMultiQuery.setScrollFilter(
                    scrollId,
                    GlobalDatasParser.DEFAULT_SCROLL_TIMEOUT,
                    VitamConfiguration.getElasticSearchScrollLimit()
                );
                size = (int) (size + hits.getSize());
                JsonNode last = Iterables.getLast(requestResponse.getResults(), null);
                if (last != null) {
                    lastUpdateDate = last.get(VitamFieldsHelper.approximateUpdateDate()).asText();
                }
            } while (
                hits.getSize() > 0 &&
                hits.getSize() >= VitamConfiguration.getElasticSearchScrollLimit() &&
                size < THRESHOLD
            );
            clearScrollWhenPagingHasNotFinished(scrollId, client);
            return lastUpdateDate;
        } catch (
            MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException
            | InvalidCreateOperationException
            | InvalidParseOperationException e
        ) {
            throw new VitamRuntimeException(e);
        }
    }

    private void clearScrollWhenPagingHasNotFinished(String scrollId, MetaDataClient client)
        throws MetaDataClientServerException {
        if (Objects.nonNull(scrollId)) {
            client.clearESScrollFilter(scrollId);
        }
    }

    private SelectMultiQuery buildFilterESQuery(int operationsDelayInMinutes, String lastAuditData)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        if (Objects.nonNull(lastAuditData)) {
            selectMultiQuery.addQueries(QueryHelper.gte(VitamFieldsHelper.approximateUpdateDate(), lastAuditData));
        }
        selectMultiQuery.addQueries(
            QueryHelper.lt(
                VitamFieldsHelper.approximateUpdateDate(),
                LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.now().minusMinutes(operationsDelayInMinutes))
            )
        );

        selectMultiQuery.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.approximateUpdateDate());
        selectMultiQuery.addOrderByAscFilter(VitamFieldsHelper.approximateUpdateDate());
        String scrollId = "START";
        selectMultiQuery.setScrollFilter(
            scrollId,
            GlobalDatasParser.DEFAULT_SCROLL_TIMEOUT,
            VitamConfiguration.getElasticSearchScrollLimit()
        );
        return selectMultiQuery;
    }
}
