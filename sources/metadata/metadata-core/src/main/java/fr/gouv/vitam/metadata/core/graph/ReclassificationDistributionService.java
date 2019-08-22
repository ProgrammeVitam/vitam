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
package fr.gouv.vitam.metadata.core.graph;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FileUtils;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Service for distributing
 */
public class ReclassificationDistributionService {

    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String UNIT_LIST_TMP_FILE_PREFIX = "unit_export_";
    private static final String OBJECT_GROUP_LIST_TMP_FILE_PREFIX = "object_group_export_";
    private final WorkspaceClientFactory workspaceClientFactory;
    private final VitamRepositoryProvider vitamRepositoryProvider;


    public ReclassificationDistributionService(VitamRepositoryProvider vitamRepositoryProvider) {
        this(WorkspaceClientFactory.getInstance(),
            vitamRepositoryProvider);
    }

    @VisibleForTesting
    ReclassificationDistributionService(
        WorkspaceClientFactory workspaceClientFactory,
        VitamRepositoryProvider vitamRepositoryProvider) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.vitamRepositoryProvider = vitamRepositoryProvider;
    }


    public void exportReclassificationChildNodes(Set<String> unitIds, String unitsToUpdateJsonLineFileName,
        String objectGroupsToUpdateJsonLineFileName) throws IOException {

        VitamMongoRepository vitamMongoRepository =
            vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection());

        FindIterable<Document> query = vitamMongoRepository.findDocuments(
            Filters.or(
                Filters.in(Unit.ID, unitIds),
                Filters.in(Unit.UNITUPS, unitIds))
            , VitamConfiguration.getBatchSize())
            .projection(Projections.include(Unit.ID, Unit.OG));

        File tempUnitReportFile = null;
        File tempObjectGroupReportFile = null;

        try {

            tempUnitReportFile = File.createTempFile(UNIT_LIST_TMP_FILE_PREFIX, JSONL_EXTENSION,
                new File(VitamConfiguration.getVitamTmpFolder()));
            tempObjectGroupReportFile = File.createTempFile(OBJECT_GROUP_LIST_TMP_FILE_PREFIX, JSONL_EXTENSION,
                new File(VitamConfiguration.getVitamTmpFolder()));

            try (MongoCursor<Document> iterator = query.iterator();
                JsonLineWriter unitReportWriter = new JsonLineWriter(new FileOutputStream(tempUnitReportFile));
                JsonLineWriter objectGroupReportWriter = new JsonLineWriter(
                    new FileOutputStream(tempObjectGroupReportFile))) {

                while (iterator.hasNext()) {
                    Document doc = iterator.next();

                    String id = doc.get(Unit.ID, String.class);
                    String objectGroupId = doc.get(Unit.OG, String.class);

                    unitReportWriter.addEntry(new JsonLineModel(id));
                    if (objectGroupId != null) {
                        objectGroupReportWriter.addEntry(new JsonLineModel(objectGroupId));
                    }
                }
            }

            storeIntoWorkspace(unitsToUpdateJsonLineFileName, tempUnitReportFile);
            storeIntoWorkspace(objectGroupsToUpdateJsonLineFileName, tempObjectGroupReportFile);

        } finally {
            FileUtils.deleteQuietly(tempUnitReportFile);
            FileUtils.deleteQuietly(tempObjectGroupReportFile);
        }
    }

    private void storeIntoWorkspace(String filename, File file) throws IOException {

        try (WorkspaceClient client = workspaceClientFactory.getClient();
            InputStream inputStream = new FileInputStream(file)) {

            String containerName = VitamThreadUtils.getVitamSession().getRequestId();
            client.putObject(containerName, filename, inputStream);

        } catch (ContentAddressableStorageServerException e) {
            throw new IOException("Could not store file to workspace", e);
        }
    }
}
