/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.external.client;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IngestExternalClientMockTest {

    private static final String MOCK_INPUT_STREAM = "VITAM-Ingest External Client Mock InputStream";
    final int TENANT_ID = 0;
    private static final String CONTEXT_ID = "defaultContext";
    private static final String EXECUTION_MODE = "defaultContext";

    @Test(expected = IngestExternalException.class)
    public void givenNullStreamThenThrowIngestExternalException() throws IngestExternalException, XMLStreamException {
        IngestExternalClientFactory.changeMode((SecureClientConfiguration) null);

        final IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof IngestExternalClientMock);
        client.ingest(new VitamContext(TENANT_ID), null, CONTEXT_ID, EXECUTION_MODE);
    }

    @Test
    public void givenNonEmptyStreamThenUploadWithSuccess() throws IngestExternalException, XMLStreamException {
        IngestExternalClientFactory.changeMode((SecureClientConfiguration) null);

        final IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final InputStream firstStream = IOUtils.toInputStream(MOCK_INPUT_STREAM, CharsetUtils.UTF8);
        RequestResponse<Void> requestResponse = client.ingest(
            new VitamContext(TENANT_ID),
            firstStream,
            CONTEXT_ID,
            EXECUTION_MODE
        );

        assertEquals(requestResponse.getHttpCode(), 202);
    }

    @Test
    public void givenNonEmptyLocalFileThenUploadWithSuccess() throws IngestExternalException, XMLStreamException {
        IngestExternalClientFactory.changeMode((SecureClientConfiguration) null);

        final IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        RequestResponse<Void> requestResponse = client.ingestLocal(
            new VitamContext(TENANT_ID),
            new LocalFile("path"),
            CONTEXT_ID,
            EXECUTION_MODE
        );

        assertEquals(requestResponse.getHttpCode(), 202);
    }

    @Test
    public void givenNonEmptyStreamWhenDownloadSuccess() throws VitamClientException {
        IngestExternalClientFactory.changeMode((SecureClientConfiguration) null);

        final IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final InputStream firstStream = StreamUtils.toInputStream("test");
        final InputStream responseStream = client
            .downloadObjectAsync(new VitamContext(TENANT_ID), "1", IngestCollection.MANIFESTS)
            .readEntity(InputStream.class);

        assertNotNull(responseStream);
        try {
            assertTrue(IOUtils.contentEquals(responseStream, firstStream));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
