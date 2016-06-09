/**
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
 */
package fr.gouv.vitam.ingest.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.ingest.util.PropertyUtil;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

public class UploadServiceImplTest {

    private UploadServiceImpl uploadServiceImpl;
    private final FormDataContentDisposition formDataContentDisposition =
        FormDataContentDisposition.name("file").fileName("SIP").build();

    private WorkspaceClient workspaceClient;
    private PropertyUtil propertyUtil;

    @Before
    public void setUp() throws Exception {
        uploadServiceImpl = new UploadServiceImpl();
        workspaceClient = mock(WorkspaceClient.class);
        propertyUtil = mock(PropertyUtil.class);

    }

    @After
    public void tearDown() throws Exception {}

    /**
     * gets the inputstream of a file
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private InputStream getInputStream(String file) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
    }

    @Ignore
    public void givenWorkspaceExistWhenUploadSipAsStreamThenReturnOK() throws IOException, Exception {
        Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
            formDataContentDisposition, "SIP");
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Ignore
    public void givenWorkspaceNotExistWhenUploadSipAsStreamThenReturnKO() throws IOException, Exception {
        Mockito.doThrow(new ContentAddressableStorageServerException("")).when(workspaceClient)
            .unzipSipObject(anyObject(), anyObject());
        Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
            formDataContentDisposition, "SIP");
        assertThat(response).isNotNull();
    }

    // @Test(expected = IngestException.class)
    // public void givenFileConfNotExistParameterWhenUploadSipAsStreamThenRaiseAnException() throws Exception {
    // when(propertyUtil.loadProperties(anyObject(), anyObject())).thenReturn(null);
    // Response response = uploadServiceImpl.uploadSipAsStream(null,
    // formDataContentDisposition, "SIP");
    // }

    //@Test(expected = Exception.class)
    @Ignore
    public void givenInputStreamNullParameterWhenUploadSipAsStreamThenRaiseAnException() throws Exception {
        Response response = uploadServiceImpl.uploadSipAsStream(null,
            formDataContentDisposition, "SIP");
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(500);
    }
}
