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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditParameters;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EvidenceAuditDatabaseCheckTest class
 */
public class EvidenceAuditDatabaseCheckTest {

    public static final String UNITTYPE =
        "{\n" + "  \"id\" : \"aeaqaaaaaaebta56aam5ualcdnzc4wiaaabq\",\n" + "  \"metadaType\" : \"UNIT\"\n" + "}";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    public HandlerIO handlerIO;

    @Mock
    private EvidenceService evidenceService;

    private EvidenceAuditDatabaseCheck evidenceAuditDatabaseCheck;

    @Mock
    private EvidenceAuditReportService evidenceAuditReportService;

    @Before
    public void setUp() throws Exception {
        evidenceAuditDatabaseCheck = new EvidenceAuditDatabaseCheck(evidenceService, evidenceAuditReportService);
    }

    @Test
    public void should_check_audit_data_base() throws Exception {
        WorkerParameters defaultWorkerParameters = mock(WorkerParameters.class);

        when(defaultWorkerParameters.getObjectName()).thenReturn("test");

        File file = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        writeAsFile(getFromString(UNITTYPE), file);

        when(handlerIO.getFileFromWorkspace("Object/test")).thenReturn(file);
        EvidenceAuditParameters evidenceAuditParameters = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile("evidenceAudit/parameters.json"),
            EvidenceAuditParameters.class
        );
        when(handlerIO.getNewLocalFile("test.tmp")).thenReturn(file2);
        when(handlerIO.getInput(0)).thenReturn(PropertiesUtils.getResourceFile("evidenceAudit/strategies.json"));

        when(evidenceService.evidenceAuditsChecks(eq("test"), eq(MetadataType.UNIT), any())).thenReturn(
            evidenceAuditParameters
        );

        ItemStatus execute = evidenceAuditDatabaseCheck.execute(defaultWorkerParameters, handlerIO);
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        EvidenceAuditParameters evidenceAuditParameters2 = JsonHandler.getFromFile(
            file2,
            EvidenceAuditParameters.class
        );

        assertThat(evidenceAuditParameters.getAuditMessage()).isEqualTo(evidenceAuditParameters2.getAuditMessage());
        assertThat(evidenceAuditParameters.getObjectStorageMetadataResultMap()).isEqualTo(
            evidenceAuditParameters2.getObjectStorageMetadataResultMap()
        );
        assertThat(evidenceAuditParameters.getEvidenceStatus()).isEqualTo(evidenceAuditParameters2.getEvidenceStatus());

        when(handlerIO.getFileFromWorkspace(any())).thenThrow(IOException.class);

        execute = evidenceAuditDatabaseCheck.execute(defaultWorkerParameters, handlerIO);
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }
}
