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
package fr.gouv.vitam.metadata.core.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.model.MetadataResult;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Service for distributing
 */
public class ReclassificationDistributionService {

    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String UNIT_LIST_TMP_FILE_PREFIX = "unit_export_";
    private static final String OBJECT_GROUP_LIST_TMP_FILE_PREFIX = "object_group_export_";
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataImpl metaData;

    public ReclassificationDistributionService(MetaDataImpl metaData) {
        this(WorkspaceClientFactory.getInstance(), metaData);
    }

    @VisibleForTesting
    ReclassificationDistributionService(WorkspaceClientFactory workspaceClientFactory, MetaDataImpl metaData) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.metaData = metaData;
    }

    public void exportReclassificationChildNodes(
        Set<String> unitIds,
        String unitsToUpdateJsonLineFileName,
        String objectGroupsToUpdateJsonLineFileName
    ) throws IOException, InvalidParseOperationException, InvalidCreateOperationException {
        SelectMultiQuery select = createSelectRequest(unitIds);
        Iterator<JsonNode> iterator = executeSelectAsIterator(select);

        File tempUnitReportFile = null;
        File tempObjectGroupReportFile = null;

        try {
            tempUnitReportFile = File.createTempFile(
                UNIT_LIST_TMP_FILE_PREFIX,
                JSONL_EXTENSION,
                new File(VitamConfiguration.getVitamTmpFolder())
            );
            tempObjectGroupReportFile = File.createTempFile(
                OBJECT_GROUP_LIST_TMP_FILE_PREFIX,
                JSONL_EXTENSION,
                new File(VitamConfiguration.getVitamTmpFolder())
            );

            try (
                JsonLineWriter unitReportWriter = new JsonLineWriter(new FileOutputStream(tempUnitReportFile));
                JsonLineWriter objectGroupReportWriter = new JsonLineWriter(
                    new FileOutputStream(tempObjectGroupReportFile)
                )
            ) {
                appendEntries(iterator, unitReportWriter, objectGroupReportWriter);
            }

            storeIntoWorkspace(unitsToUpdateJsonLineFileName, tempUnitReportFile);
            storeIntoWorkspace(objectGroupsToUpdateJsonLineFileName, tempObjectGroupReportFile);
        } finally {
            FileUtils.deleteQuietly(tempUnitReportFile);
            FileUtils.deleteQuietly(tempObjectGroupReportFile);
        }
    }

    private SelectMultiQuery createSelectRequest(Set<String> unitIds)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        SelectMultiQuery select = new SelectMultiQuery();

        // Select units
        String[] parentUnitIds = unitIds.toArray(new String[0]);
        InQuery childrenUnitsQuery = QueryHelper.in(VitamFieldsHelper.allunitups(), parentUnitIds);
        InQuery parentsUnitsQuery = QueryHelper.in(VitamFieldsHelper.id(), parentUnitIds);
        BooleanQuery parentsAndProgenyUnits = QueryHelper.or().add(childrenUnitsQuery, parentsUnitsQuery);
        select.setQuery(parentsAndProgenyUnits);

        // Order by : object group Id (to be able to detect / skip duplicates)
        select.addOrderByAscFilter(VitamFieldsHelper.object());

        // Projection
        select.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.object());

        return select;
    }

    private Iterator<JsonNode> executeSelectAsIterator(SelectMultiQuery select) {
        ScrollSpliterator<JsonNode> scrollRequest = new ScrollSpliterator<>(
            select,
            query -> {
                try {
                    final MetadataResult metadataResult = metaData.selectUnitsByQuery(query.getFinalSelect());
                    return new RequestResponseOK<JsonNode>(metadataResult.getQuery())
                        .addAllResults(metadataResult.getResults())
                        .addAllFacetResults(metadataResult.getFacetResults())
                        .setHits(metadataResult.getHits());
                } catch (
                    MetaDataExecutionException
                    | MetaDataDocumentSizeException
                    | InvalidParseOperationException
                    | VitamDBException
                    | BadRequestException
                    | MetaDataNotFoundException e
                ) {
                    throw new IllegalStateException(e);
                }
            },
            VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            VitamConfiguration.getElasticSearchScrollLimit()
        );

        return new SpliteratorIterator<>(scrollRequest);
    }

    private void appendEntries(
        Iterator<JsonNode> iterator,
        JsonLineWriter unitReportWriter,
        JsonLineWriter objectGroupReportWriter
    ) throws IOException {
        // Used to avoid duplicates
        String lastObjectGroupId = null;

        while (iterator.hasNext()) {
            JsonNode doc = iterator.next();

            String id = doc.get(VitamFieldsHelper.id()).textValue();
            String objectGroupId = doc.has(VitamFieldsHelper.object())
                ? doc.get(VitamFieldsHelper.object()).textValue()
                : null;

            unitReportWriter.addEntry(new JsonLineModel(id));

            // Only report object group Id if not duplicate
            if (objectGroupId != null && !objectGroupId.equals(lastObjectGroupId)) {
                objectGroupReportWriter.addEntry(new JsonLineModel(objectGroupId));
            }
            lastObjectGroupId = objectGroupId;
        }
    }

    private void storeIntoWorkspace(String filename, File file) throws IOException {
        try (
            WorkspaceClient client = workspaceClientFactory.getClient();
            InputStream inputStream = new FileInputStream(file)
        ) {
            String containerName = VitamThreadUtils.getVitamSession().getRequestId();
            client.putObject(containerName, filename, inputStream);
        } catch (ContentAddressableStorageServerException e) {
            throw new IOException("Could not store file to workspace", e);
        }
    }
}
