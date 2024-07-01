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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaIngestParams;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static fr.gouv.vitam.worker.common.utils.SedaUtilsFactory.SEDA_INGEST_PARAMS_FILE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckNoObjectsActionHandlerTest {

    private static final String GUID = "aeaaaaaaaaaaaaababz4aakxtykbybyaaaaq2203";
    private static final String CURRENT_STEP = "currentStep";
    private static final String OBJECT_NAME = "objectName.json";
    private static final String HTTP_LOCALHOST = "http://localhost:8080";
    private static final String PLAN_MANIFEST = "CheckNoObjectsActionHandler/manifest.xml";
    private static final String KO_MANIFEST = "CheckNoObjectsActionHandler/manifestKO.xml";

    private HandlerIO handlerIO = mock(HandlerIO.class);
    private WorkerParameters params;

    private InputStream sedaOK;
    private InputStream sedaKO;

    private SedaUtilsFactory sedaUtilsFactory = mock(SedaUtilsFactory.class);

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException, ProcessingException {
        final GUID guid = GUIDFactory.newGUID();
        params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(HTTP_LOCALHOST)
            .setUrlMetadata(HTTP_LOCALHOST)
            .setObjectName(OBJECT_NAME)
            .setCurrentStep(CURRENT_STEP)
            .setContainerName(guid.getId())
            .setProcessId(GUID);
        sedaOK = PropertiesUtils.getResourceAsStream(PLAN_MANIFEST);
        sedaKO = PropertiesUtils.getResourceAsStream(KO_MANIFEST);
    }

    @Test
    public void checkManifestHavingObjectOrNot() throws Exception {
        final CheckNoObjectsActionHandler handler = new CheckNoObjectsActionHandler();
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(sedaOK);
        when(handlerIO.isExistingFileInWorkspace(SEDA_INGEST_PARAMS_FILE)).thenReturn(true);
        when(handlerIO.getJsonFromWorkspace(SEDA_INGEST_PARAMS_FILE)).thenReturn(
            JsonHandler.toJsonNode(
                new SedaIngestParams(
                    SupportedSedaVersions.SEDA_2_1.getVersion(),
                    SupportedSedaVersions.SEDA_2_1.getNamespaceURI()
                )
            )
        );
        WorkerParameters parameters = params
            .putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.OK.name())
            .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name())
            .setObjectNameList(Lists.newArrayList("objectName.json"));
        final ItemStatus response = handler.execute(parameters, handlerIO);
        handler.close();
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(sedaKO);
        final ItemStatus responseKO = handler.execute(parameters, handlerIO);
        handler.close();
        assertEquals(responseKO.getGlobalStatus(), StatusCode.KO);
    }
}
