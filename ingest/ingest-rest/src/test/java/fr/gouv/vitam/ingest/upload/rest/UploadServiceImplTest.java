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
package fr.gouv.vitam.ingest.upload.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import fr.gouv.vitam.ingest.model.UploadResponseDTO;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadServiceImplTest {

    private UploadServiceImpl uploadServiceImpl;
    private final FormDataContentDisposition formDataContentDisposition =
            FormDataContentDisposition.name("file").fileName("SIP").build();
    private WorkspaceClientFactory workspaceClientFactory;
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private LogbookClient logbookClient;
    private LogbookParameters parameters;
    
    

    @Before
    public void setUp() throws Exception {
    	 workspaceClientFactory = mock(WorkspaceClientFactory.class);
         workspaceClient = mock(WorkspaceClient.class);
         logbookClient = mock(LogbookClient.class);
         processingClient = mock(ProcessingManagementClient.class);
         when(workspaceClientFactory.create(anyObject())).thenReturn(workspaceClient);
         parameters = mock(LogbookParameters.class);
         uploadServiceImpl = new UploadServiceImpl(logbookClient, processingClient, workspaceClient);
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

    @Test
    public void givenWorkspaceExistWhenUploadSipAsStreamThenReturnOK() throws Exception {
        final Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
                formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void givenWorkspaceNotExistWhenUploadSipAsStreamThenReturnKO() throws Exception {
        Mockito.doThrow(new ContentAddressableStorageServerException("")).when(workspaceClient)
                .unzipObject(Matchers.anyObject(), Matchers.anyObject(),  Matchers.anyObject());
        final Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
                formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
    }

    @Test(expected = Exception.class)
    public void givenInputStreamNullParameterWhenUploadSipAsStreamThenRaiseAnException() throws Exception {
        final Response response = uploadServiceImpl.uploadSipAsStream(null,
                formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(500);
    }
    
    @Test
    public void givenWorkspaceContentAddressableStorageAlreadyExistException_whenUploadSipAsStream_thenRaiseAnException_ContentAddressableStorageAlreadyExistException() throws Exception {
    	when(parameters.putParameterValue(anyObject(), anyObject())).thenReturn(parameters);
        Mockito.doThrow(new ContentAddressableStorageServerException("")).when(workspaceClient).unzipObject(anyObject(), anyObject(), anyObject());
        final Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
            formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(UploadResponseDTO.class);
        UploadResponseDTO uploadResponseDTO = (UploadResponseDTO) response.getEntity();
        assertThat(uploadResponseDTO.getVitamStatus()).isEqualTo("workspace failed");
    }
    
    @Test
    public void givenLogBookUnavailable_whenUploadSipAsStream_thenRaiseAnException_LogbookClientNotFoundException() throws Exception {

        Mockito.doThrow(new LogbookClientNotFoundException("")).when(logbookClient).update(anyObject());
        final Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
            formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(UploadResponseDTO.class);
        UploadResponseDTO uploadResponseDTO = (UploadResponseDTO) response.getEntity();
        assertThat(uploadResponseDTO.getVitamStatus()).isEqualTo("upload failed");
    }
    
    @Test
    public void givenProcessUnavailable_whenUploadSipAsStream_thenRaiseAnException_ProcessingException() throws Exception {

        Mockito.doThrow(new ProcessingException("")).when(processingClient).executeVitamProcess(anyObject(), anyObject());
        final Response response = uploadServiceImpl.uploadSipAsStream(getInputStream("SIP_bordereau_avec_objet_OK.zip"),
            formDataContentDisposition, "SIP");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(UploadResponseDTO.class);
        UploadResponseDTO uploadResponseDTO = (UploadResponseDTO) response.getEntity();
        assertThat(uploadResponseDTO.getVitamStatus()).isEqualTo("upload failed");
    }
    
    
}
