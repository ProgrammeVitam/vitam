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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationUnitPrepareTest {

    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters defaultWorkerParameters = mock(WorkerParameters.class);

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
    }

    @Test
    public void should_prepare_Linked_units_files() throws Exception {
        //GIVEN
        given(metaDataClient.selectUnits(any(JsonNode.class))).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/migration/resultMetadata.json"))
        );

        File jsonLineFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile("Units.jsonl")).willReturn(jsonLineFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(("report.json"))).willReturn(reportFile);

        MigrationUnitPrepare migrationUnitPrepare = new MigrationUnitPrepare(metaDataClientFactory, 3);
        File file = tempFolder.newFile();
        when(handlerIO.getNewLocalFile("migrationUnitsListIds")).thenReturn(file);

        //WHEN
        ItemStatus execute = migrationUnitPrepare.execute(defaultWorkerParameters, handlerIO);

        //THEN

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        try (InputStream is = new FileInputStream(jsonLineFile)) {
            JsonLineGenericIterator<JsonLineModel> lineGenericIterator = new JsonLineGenericIterator<>(
                is,
                TYPE_REFERENCE
            );
            List<String> unitIds = IteratorUtils.toList(lineGenericIterator)
                .stream()
                .map(JsonLineModel::getId)
                .collect(Collectors.toList());

            assertThat(unitIds).containsExactly(
                "aeaqaaaaaadf6mc4aathcak7tmtgdnaaaaba",
                "aeaqaaaaaadf6mc4aathcak7tmtgdmyaaaba",
                "aeaqaaaaaadf6mc4aathcak7tmtgdayaaaca",
                "aeaqaaaaaadf6mc4aathcak7tmtgdniaaaba"
            );
        }
    }
}
