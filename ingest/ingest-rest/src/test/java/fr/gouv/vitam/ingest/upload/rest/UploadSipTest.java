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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.ingest.model.UploadResponseDTO;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * TODO refactor copy/paste!
 */
public class UploadSipTest extends JerseyTest {

    private static Logger LOG = LoggerFactory.getLogger(UploadSipTest.class);

    private Client client;
    private WebTarget webTarget;

    @Before
    public void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public javax.ws.rs.core.Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.CONTAINER_FACTORY);
        enable(TestProperties.CONTAINER_PORT);
        set(TestProperties.CONTAINER_PORT, "8082");
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("fr.gouv.vitam.ingest.upload");
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(MOXyJsonProvider.class);
        resourceConfig.register(UploadServiceImpl.class);

        return resourceConfig;
    }


    @Test
    public void shouldGetStatusWithoutArgs() throws URISyntaxException {
        webTarget = client.target(new URI(getBaseUri() + "ingest/v1/status"));
        final Invocation.Builder builder = webTarget.request();
        final Response response = builder.get();
        final String status = response.readEntity(String.class);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals("", status);
    }


    private Response processUploadSip(String sipFile) throws URISyntaxException, FileNotFoundException {
        final Client clientUpload = ClientBuilder.newBuilder()
                // .register(UploadResponseMessageBodyReader.class)
                // .register(StatusResponseMessageBodyReader.class)
                // .register(StatusRequestMessageBodyWriter.class)
                .register(MultiPartFeature.class)
                .build();
        final WebTarget webTargetUpload = clientUpload.target(new URI(getBaseUri() + "ingest/v1/upload"));

        final MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        final File file = PropertiesUtils.findFile(sipFile);

        final FileDataBodyPart filePart = new FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(filePart);

        final Builder builder = webTargetUpload
                .request(MediaType.APPLICATION_JSON_TYPE);
        final Entity entity = Entity.entity(multiPart, multiPart.getMediaType());
        final Response response = builder.post(entity);

        return response;
    }

    @Test
    public void shouldUploadErrorSip() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("test.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(response.getStatus(), 200);
    }

    @Test
    public void shouldUploadSip() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("SIP_bordereau_avec_objet_OK.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldUploadSipOnErrorObjectNumSize() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("SIP_nombre_objets_sup_SEDA.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(response.getStatus(), 200);
    }

    @Test
    public void shouldUploadSipOnErrorObjectNumSizeSedaKo() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("SIP_nombre_objets_sup_SEDA_KO.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(response.getStatus(), 200);
    }

    @Test
    public void shouldUploadSipOnErrorWithoutSedaWithObjectNum() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("SIP_sans_bordereau_avec_objet.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(response.getStatus(), 200);
    }

    @Test
    public void shouldUploadSipOnErrorEmpty() throws URISyntaxException, FileNotFoundException {

        final Response response = processUploadSip("SIP_vide.zip");
        final UploadResponseDTO responseDTO = response.readEntity(UploadResponseDTO.class);

        Assert.assertNotNull(responseDTO);
        Assert.assertEquals("500", responseDTO.getVitamCode());
        Assert.assertEquals("error ingest", responseDTO.getEngineStatus());
        Assert.assertEquals(response.getStatus(), 200);
    }

    public void shouldForwardStreamSIPtoWorkspace_onEmptyStream() {

    }

    public void shouldForwardStreamSIPtoWorkspace_onSuccess() {

    }

    public void shouldUnzipByWorkspace_onError() {
        // not possible the ingest-ext unzip object and check them
        // error on unzip by workspace
    }

    public void shouldLaunchWorkflow() {}


}
