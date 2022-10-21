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

package fr.gouv.vitam.collect.internal.service;

import fr.gouv.vitam.collect.internal.model.CollectUnitModel;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CollectServiceTest {


    private static final String SAMPLE_ARCHIVE_UNIT = "archive_unit_from_metadata.json";
    private static final String SAMPLE_OBJECT_GROUP = "object_group_from_metadata.json";
    // this guid was generated with tenant = 0
    private static final String ID = "aeaqaaaaaaevelkyaa6teak73hlewtiaaabq";
    private static final String REQUEST_ID = "aeaqaaaaaitxll67abarqaktftcfyniaaaaq";

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    private CollectService collectService;
    @Mock private MetadataService metadataService;
    @Mock private WorkspaceClientFactory workspaceClientFactory;
    @Mock private WorkspaceClient workspaceClient;

    @Mock private FormatIdentifierFactory formatIdentifierFactory;


    @Before
    public void setUp() throws Exception {
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        collectService = new CollectService(metadataService, workspaceClientFactory, formatIdentifierFactory);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createRequestId() {
    }

    @Test
    public void checkParameters() {
    }

    @Test
    public void getArchiveUnitModel() throws Exception {
        String unitId = "aeeaaaaaacfm6tqsaawpgamadc4j5baaaaaq";
        when(metadataService.selectUnitById(any())).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_ARCHIVE_UNIT)));
        CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
        Assertions.assertThat(archiveUnitModel).isNotNull();
        Assertions.assertThat(archiveUnitModel.getId()).isEqualTo(unitId);
    }

    @Test
    public void saveObjectGroupInMetaData() {
    }

    @Test
    public void getDbObjectGroup() {
    }

    @Test
    public void addBinaryInfoToQualifier() {
    }

    @Test
    public void pushStreamToWorkspace() {
    }

    @Test
    public void getInputStreamFromWorkspace() {
    }

    @Test
    public void getBinaryByUsageAndVersion() {
    }
}