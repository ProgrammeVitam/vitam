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
package fr.gouv.vitam.common.storage.s3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AmazonS3V1MockedServerTest {

    private static final String PROVIDER = "amazon-s3-v1";
    private static final String S3_CONF_FILE = "s3/s3-conf-mock.json";

    private StorageConfiguration configuration;
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int s3Port = junitHelper.findAvailablePort();

    private AmazonS3V1 amazonS3V1;

    @ClassRule
    public static WireMockClassRule s3WireMockRule = new WireMockClassRule(s3Port);

    private static final String CONTAINER_0 = "0_Unit";
    private static final String CONTAINER_1 = "1_Unit";
    private static final String BUCKET_0 = "/0.unit";
    private static final String BUCKET_1 = "/1.unit";
    private static final String FILE_0 = "3500.txt";
    private static final String FILE_1 = "file1.pdf";
    private static final String OBJECT_ID_0 = "object0id";
    private static final String OBJECT_ID_1 = "object1id";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AMZ_REQUEST_ID = "X-Amz-Request-Id";

    @Before
    public void setUp() throws Exception {
        JsonNode jsonNode = JsonHandler.getFromFile(PropertiesUtils.findFile(S3_CONF_FILE));
        configuration = new StorageConfiguration();
        configuration.setProvider(PROVIDER);
        configuration.setStoragePath(jsonNode.findValue("STORAGE_PATH").textValue());
        configuration.setS3RegionName(Regions.DEFAULT_REGION.name());
        configuration.setS3Endpoint(jsonNode.findValue("S3_ENDPOINT").textValue() + s3Port);
        configuration.setS3AccessKey(jsonNode.findValue("S3_ACCESSKEY").textValue());
        configuration.setS3SecretKey(jsonNode.findValue("S3_SECRETKEY").textValue());
        configuration.setS3PathStyleAccessEnabled(true);
        configuration.setS3ConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
        configuration.setS3SocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
        configuration.setS3MaxConnections(ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        configuration.setS3RequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT);
        configuration.setS3ClientExecutionTimeout(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT);
        amazonS3V1 = new AmazonS3V1(configuration);
        s3WireMockRule.resetAll();
    }

    @Test
    public void exists_container_should_return_false_when_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?acl").willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );
        boolean exists = amazonS3V1.isExistingContainer(CONTAINER_1);
        assertThat(exists).isFalse();
    }

    @Test
    public void exists_container_should_return_true_when_bucket_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/?acl").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_200.xml")))
            )
        );
        boolean exists = amazonS3V1.isExistingContainer(CONTAINER_0);
        assertThat(exists).isTrue();
    }

    @Test
    public void exists_container_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(get(BUCKET_1 + "/?acl").willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingContainer(CONTAINER_1);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void exists_object_should_return_false_when_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?acl").willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );
        s3WireMockRule.stubFor(head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(aResponse().withStatus(404)));
        boolean exists = amazonS3V1.isExistingObject(CONTAINER_1, OBJECT_ID_1);
        assertThat(exists).isFalse();
    }

    @Test
    public void exists_object_should_return_false_when_object_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/?acl").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_200.xml")))
            )
        );
        s3WireMockRule.stubFor(head(urlMatching(BUCKET_0 + "/" + OBJECT_ID_1)).willReturn(aResponse().withStatus(404)));
        boolean exists = amazonS3V1.isExistingObject(CONTAINER_0, OBJECT_ID_1);
        assertThat(exists).isFalse();
    }

    @Test
    public void exists_object_should_return_true_when_object_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/?acl").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_200.xml")))
            )
        );
        s3WireMockRule.stubFor(
            head(urlMatching(BUCKET_0 + "/" + OBJECT_ID_0)).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(
                        "X-Amz-Meta-Digest",
                        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418"
                    )
                    .withHeader("X-Amz-Meta-Digest-Type", "SHA-512")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        boolean exists = amazonS3V1.isExistingObject(CONTAINER_0, OBJECT_ID_0);
        assertThat(exists).isTrue();
    }

    @Test
    public void exists_object_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(get(BUCKET_1 + "/?acl").willReturn(aResponse().withStatus(500)));
        s3WireMockRule.stubFor(head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingObject(CONTAINER_1, OBJECT_ID_1);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void create_container_should_not_throw_exception_when_bucket_already_exists() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/").willReturn(
                aResponse()
                    .withStatus(409)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(
                        IOUtils.toByteArray(
                            PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_already_exists_409.xml")
                        )
                    )
            )
        );
        assertThatCode(() -> {
            amazonS3V1.createContainer(CONTAINER_1);
        }).doesNotThrowAnyException();
    }

    @Test
    public void create_container_should_not_throw_exception_when_bucket_is_created() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_0 + "/").willReturn(aResponse().withStatus(200).withHeader(AMZ_REQUEST_ID, "XXXXXX"))
        );
        assertThatCode(() -> {
            amazonS3V1.createContainer(CONTAINER_0);
        }).doesNotThrowAnyException();
    }

    @Test
    public void create_container_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(put(BUCKET_1 + "/").willReturn(aResponse().withStatus(500)));
        assertThatCode(() -> {
            amazonS3V1.createContainer(CONTAINER_1);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void delete_object_should_not_throw_exception_when_object_does_exists_or_not() throws Exception {
        s3WireMockRule.stubFor(
            delete(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse().withStatus(204).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        assertThatCode(() -> {
            amazonS3V1.deleteObject(CONTAINER_1, OBJECT_ID_1);
        }).doesNotThrowAnyException();
    }

    @Test
    public void delete_object_should_throw_exception_when_container_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            delete(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );
        assertThatCode(() -> {
            amazonS3V1.deleteObject(CONTAINER_1, OBJECT_ID_1);
        })
            .isInstanceOf(ContentAddressableStorageNotFoundException.class)
            .hasMessageContaining(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
    }

    @Test
    public void delete_object_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(delete(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(aResponse().withStatus(500)));
        assertThatCode(() -> {
            amazonS3V1.deleteObject(CONTAINER_1, OBJECT_ID_1);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void get_object_should_not_throw_exception_when_object_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(FILE_0)))
            )
        );
        assertThatCode(() -> {
            amazonS3V1.getObject(CONTAINER_0, OBJECT_ID_0);
        }).doesNotThrowAnyException();
    }

    @Test
    public void get_object_should_return_object_when_object_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withHeader("Content-Length", "3500")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(FILE_0)))
            )
        );

        ObjectContent object = amazonS3V1.getObject(CONTAINER_0, OBJECT_ID_0);
        assertThat(object.getSize()).isEqualTo(3_500L);
        String downloadedFile = IOUtils.toString(object.getInputStream(), StandardCharsets.UTF_8);
        String file = IOUtils.toString(PropertiesUtils.getResourceAsStream(FILE_0), StandardCharsets.UTF_8);
        assertThat(downloadedFile).isEqualTo(file);
    }

    @Test
    public void get_object_should_throw_exception_when_object_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_object_0unit_404.xml")))
            )
        );

        assertThatThrownBy(() -> {
            amazonS3V1.getObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageNotFoundException.class)
            .hasMessageContaining(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
    }

    @Test
    public void get_object_should_throw_exception_when_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );

        assertThatThrownBy(() -> {
            amazonS3V1.getObject(CONTAINER_1, OBJECT_ID_1);
        })
            .isInstanceOf(ContentAddressableStorageNotFoundException.class)
            .hasMessageContaining(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
    }

    @Test
    public void get_object_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(aResponse().withStatus(500)));
        assertThatCode(() -> {
            amazonS3V1.getObject(CONTAINER_0, OBJECT_ID_0);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void get_object_digest_should_not_throw_exception_when_object_exists() throws Exception {
        s3WireMockRule.stubFor(
            head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withHeader("Content-Length", "6906")
                    .withHeader(
                        "X-Amz-Meta-Digest",
                        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418"
                    )
                    .withHeader("X-Amz-Meta-Digest-Type", "SHA-512")
            )
        );
        assertThatCode(() -> {
            amazonS3V1.getObjectDigest(CONTAINER_1, OBJECT_ID_1, DigestType.SHA512, false);
        }).doesNotThrowAnyException();
    }

    @Test
    public void get_object_digest_should_throw_exception_when_object_or_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(
                aResponse().withStatus(404).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectDigest(CONTAINER_1, OBJECT_ID_1, DigestType.SHA512, false);
        }).isInstanceOf(ContentAddressableStorageNotFoundException.class);
    }

    @Test
    public void get_object_digest_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectDigest(CONTAINER_1, OBJECT_ID_1, DigestType.SHA512, false);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void get_object_metadatas_should_not_throw_exception_when_object_exists() throws Exception {
        s3WireMockRule.stubFor(
            head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withHeader("Content-Length", "6906")
                    .withHeader("Content-Type", "application/octet-stream")
                    .withHeader("Last-Modified", "Tue, 15 Jan 2019 14:51:19 GMT")
                    .withHeader(
                        "X-Amz-Meta-Digest",
                        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418"
                    )
                    .withHeader("X-Amz-Meta-Digest-Type", "SHA-512")
            )
        );
        assertThatCode(() -> {
            amazonS3V1.getObjectMetadata(CONTAINER_1, OBJECT_ID_1, false);
        }).doesNotThrowAnyException();
    }

    @Test
    public void get_object_metadatas_should_throw_exception_when_object_or_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(
                aResponse().withStatus(404).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectMetadata(CONTAINER_1, OBJECT_ID_1, false);
        }).isInstanceOf(ContentAddressableStorageNotFoundException.class);
    }

    @Test
    public void get_object_metadatas_should_throw_exception_when_s3_has_server_error() throws Exception {
        s3WireMockRule.stubFor(head(urlMatching(BUCKET_1 + "/" + OBJECT_ID_1)).willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectMetadata(CONTAINER_1, OBJECT_ID_1, false);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void upload_object_should_not_throw_exception_when_bucket_exists() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse().withStatus(200).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/" + OBJECT_ID_1)
                .withHeader("X-Amz-Metadata-Directive", equalTo("REPLACE"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                        .withHeader(CONTENT_TYPE, "application/xml")
                        .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_put_replace.xml")))
                )
        );

        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withHeader("Content-Length", "6906")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(FILE_1)))
            )
        );

        assertThatCode(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_1);
            amazonS3V1.putObject(CONTAINER_1, OBJECT_ID_1, stream, DigestType.SHA512, 6_906L);
        }).doesNotThrowAnyException();
    }

    @Test
    public void upload_object_should_throw_exception_when_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );

        assertThatThrownBy(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_1);
            amazonS3V1.putObject(CONTAINER_1, OBJECT_ID_1, stream, DigestType.SHA512, 6_906L);
        }).isInstanceOf(ContentAddressableStorageNotFoundException.class);
    }

    @Test
    public void upload_object_should_throw_exception_when_when_s3_put_has_server_error() throws Exception {
        s3WireMockRule.stubFor(put(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_1);
            amazonS3V1.putObject(CONTAINER_1, OBJECT_ID_1, stream, DigestType.SHA512, 6_906L);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void upload_object_should_throw_exception_when_when_s3_get_has_server_error() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(
                aResponse().withStatus(200).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        s3WireMockRule.stubFor(
            put(BUCKET_1 + "/" + OBJECT_ID_1)
                .withHeader("X-Amz-Metadata-Directive", equalTo("REPLACE"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                        .withHeader(CONTENT_TYPE, "application/xml")
                        .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_put_replace.xml")))
                )
        );

        s3WireMockRule.stubFor(get(BUCKET_1 + "/" + OBJECT_ID_1).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_1);
            amazonS3V1.putObject(CONTAINER_1, OBJECT_ID_1, stream, DigestType.SHA512, 6_906L);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void upload_object_should_throw_exception_when_created_object_was_not_found() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse().withStatus(200).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        s3WireMockRule.stubFor(
            put(BUCKET_0 + "/" + OBJECT_ID_0)
                .withHeader("X-Amz-Metadata-Directive", equalTo("REPLACE"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                        .withHeader(CONTENT_TYPE, "application/xml")
                        .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_put_replace.xml")))
                )
        );

        s3WireMockRule.stubFor(
            get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_object_0unit_404.xml")))
            )
        );

        assertThatThrownBy(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_0);
            amazonS3V1.putObject(CONTAINER_0, OBJECT_ID_0, stream, DigestType.SHA512, 3_500L);
        }).isInstanceOf(ContentAddressableStorageNotFoundException.class);
    }

    @Test
    public void upload_object_should_throw_exception_when_get_object_s3_server_error() throws Exception {
        s3WireMockRule.stubFor(
            put(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(
                aResponse().withStatus(200).withHeader(AMZ_REQUEST_ID, "XXXXXX")
            )
        );
        s3WireMockRule.stubFor(
            put(BUCKET_0 + "/" + OBJECT_ID_0)
                .withHeader("X-Amz-Metadata-Directive", equalTo("REPLACE"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                        .withHeader(CONTENT_TYPE, "application/xml")
                        .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_put_replace.xml")))
                )
        );

        s3WireMockRule.stubFor(get(BUCKET_0 + "/" + OBJECT_ID_0).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> {
            InputStream stream = PropertiesUtils.getResourceAsStream(FILE_0);
            amazonS3V1.putObject(CONTAINER_0, OBJECT_ID_0, stream, DigestType.SHA512, 3_500L);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    @Test
    public void list_container_should_not_throw_exception_when_objects_available() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?list-type=2&max-keys=100&fetch-owner=false").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_list.xml")))
            )
        );

        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?list-type=2&continuation-token=object_53&max-keys=100&fetch-owner=false").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withBody(
                        IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_list_next.xml"))
                    )
            )
        );

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        amazonS3V1.listContainer(CONTAINER_1, objectListingListener);

        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(150)).handleObjectEntry(objectEntryArgumentCaptor.capture());

        assertThat(objectEntryArgumentCaptor.getAllValues().get(0).getObjectId()).isEqualTo("object_0");
        assertThat(objectEntryArgumentCaptor.getAllValues().get(0).getSize()).isEqualTo(6906L);

        assertThat(objectEntryArgumentCaptor.getAllValues().get(149).getObjectId()).isEqualTo("object_99");
        assertThat(objectEntryArgumentCaptor.getAllValues().get(149).getSize()).isEqualTo(6906L);
    }

    @Test
    public void list_container_should_throw_exception_when_bucket_does_not_exists() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?list-type=2&max-keys=100&fetch-owner=false").willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE, "application/xml")
                    .withHeader(AMZ_REQUEST_ID, "XXXXXX")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("s3/s3_bucket_1unit_404.xml")))
            )
        );

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        assertThatThrownBy(() -> {
            amazonS3V1.listContainer(CONTAINER_1, objectListingListener);
        })
            .isInstanceOf(ContentAddressableStorageNotFoundException.class)
            .hasMessageContaining(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());

        verifyZeroInteractions(objectListingListener);
    }

    @Test
    public void list_container_should_throw_exception_when_s3_error() throws Exception {
        s3WireMockRule.stubFor(
            get(BUCKET_1 + "/?list-type=2&max-keys=100&fetch-owner=false").willReturn(aResponse().withStatus(500))
        );
        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);
        assertThatThrownBy(() -> {
            amazonS3V1.listContainer(CONTAINER_1, objectListingListener);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
        verifyZeroInteractions(objectListingListener);
    }

    @Test
    public void get_container_informations_should_return_default_value() throws Exception {
        ContainerInformation infos = amazonS3V1.getContainerInformation(CONTAINER_0);
        assertThat(infos).isNotNull();
        assertThat(infos.getUsableSpace()).isEqualTo(-1);
    }
}
