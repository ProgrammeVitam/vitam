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
package fr.gouv.vitam.ingest.internal.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class IngestInternalClientMockTest {
    private static final String CONTEXTID = "contextId";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @RunWithCustomExecutor
    @Test
    public void givenMockExistsWhenPostSipThenReturnOK()
        throws VitamException, XMLStreamException, FileNotFoundException {
        IngestInternalClientFactory.changeMode(null);

        final IngestInternalClient client =
            IngestInternalClientFactory.getInstance().getClient();

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =

            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        final InputStream inputstreamMockATR =
            StreamUtils.toInputStream(IngestInternalClientMock.MOCK_INGEST_INTERNAL_RESPONSE_STREAM);
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        client.uploadInitialLogbook(operationList);
        client.upload(inputStream, CommonMediaType.ZIP_TYPE, CONTEXTID);

    }

    @Test
    public void givenNonEmptyStreamWhenDownloadSuccess() throws Exception {        
        IngestInternalClientFactory.changeMode(null);

        final IngestInternalClient client =
            IngestInternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final InputStream firstStream = StreamUtils.toInputStream("test");
        final InputStream responseStream =
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);

        assertNotNull(responseStream);
        try {
            assertTrue(IOUtils.contentEquals(responseStream, firstStream));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
