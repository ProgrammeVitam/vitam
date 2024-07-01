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
package fr.gouv.vitam.common.storage.swift;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_MANIFEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class SwiftTest {

    private static final String token =
        "gAAAAABakCfriBIVmHYGRAB0TmrjM2w3kX_2bthU1RsENMjKDh_ME9vuaKTC0w291OAbiMDkVu_G4Htq84GodrG1pFbjpV5fEGOXdWBoAa4mSO_" +
        "Liv-BbZZKPDu2w7Z7FH7He6rKHfh9LUjJ4qapO6vH99zMzGJhzYHseQxw8CigYDrfsI2InAk";

    private static final String PROVIDER = "openstack-swift-v3";
    private static final String STORAGE_PATH = "/vitam/data/offer";
    private static final String SWIFT_UID = "tls.oshimae";
    private static final String SWIFT_SUB_USER = "pic.vitam";
    private static final String CREDENTIAL = "pouet";
    private static final String PROJECT_NAME = "Vitam-Env";
    private static final String X_SUBJECT_TOKEN = "X-Subject-Token";
    private static final String X_OPENSTACK_REQUEST_ID = "x-openstack-request-id";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ETAG = "Etag";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String VITAM_CUSTOMIZED_HEADER_KEY = "Cookie";
    public static final String VITAM_CUSTOMIZED_HEADER_VALUE = "Origin=vitam";

    private StorageConfiguration configuration;

    private static final JunitHelper junitHelper = JunitHelper.getInstance();

    private static final int swiftPort = junitHelper.findAvailablePort();
    private static final int keystonePort = junitHelper.findAvailablePort();

    private static final String CONTAINER_NAME = "0_object";
    private static final String OBJECT_NAME = "3500.txt";

    @ClassRule
    public static WireMockClassRule swiftWireMockRule = new WireMockClassRule(
        new WireMockConfiguration()
            .port(swiftPort)
            // Disable chunked responses & gzip (Content-Length required)
            .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
            .gzipDisabled(true)
    );

    @ClassRule
    public static WireMockClassRule keystoneWireMockRule = new WireMockClassRule(keystonePort);

    @Rule
    public WireMockClassRule swiftInstanceRule = swiftWireMockRule;

    @Rule
    public WireMockClassRule keystoneInstanceRule = keystoneWireMockRule;

    private Swift swift;

    @Before
    public void setUp() throws Exception {
        configuration = new StorageConfiguration();
        configuration.setProvider(PROVIDER);
        configuration.setStoragePath(STORAGE_PATH);
        configuration.setSwiftKeystoneAuthUrl("http://localhost:" + keystonePort + "/v3");
        configuration.setSwiftDomain(SWIFT_UID);
        configuration.setSwiftUrl("http://localhost:" + swiftPort + "/swift/v1");
        configuration.setSwiftUser(SWIFT_SUB_USER);
        configuration.setSwiftPassword(CREDENTIAL);
        configuration.setSwiftProjectName(PROJECT_NAME);
        configuration.setEnableCustomHeaders(true);
        configuration.setCustomHeaders(
            Collections.singletonList(
                new VitamCustomizedHeader(VITAM_CUSTOMIZED_HEADER_KEY, VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        String bodyResponse = JsonHandler.prettyPrint(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("keystone.json"))
        );

        keystoneInstanceRule.stubFor(
            post(urlMatching("/v3/auth/tokens")).willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader(X_SUBJECT_TOKEN, token)
                    .withHeader(X_OPENSTACK_REQUEST_ID, "req-ac910733-aa54-465a-a59f-3cfd699dd1ab")
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(bodyResponse)
            )
        );
    }

    @Test
    public void when_put_small_file_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutObjectReturns20x();
        givenGetObjectReturns20x(data);
        givenPostObjetReturns20x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatCode(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).doesNotThrowAnyException();

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
                .withRequestBody(WireMock.binaryEqualTo(data))
        );

        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        verifySwiftRequest(
            postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_META_DIGEST, equalTo(sha512sum(data)))
                .withHeader(X_OBJECT_META_DIGEST_TYPE, equalTo("SHA-512"))
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        // Expected PUT (upload) + GET (read to check digest) + POST (update metadata)
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withRequestBody(WireMock.binaryEqualTo(data))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        verifySwiftRequest(
            postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_META_DIGEST_TYPE, equalTo("SHA-512"))
                .withHeader(X_OBJECT_META_DIGEST, equalTo(sha512sum(data)))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        assertSwiftRequestCountEqualsTo(3);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(3);
    }

    @Test
    public void when_put_small_file_on_unknown_container_then_throw_not_found_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutObjectReturns404();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // Expected PUT (upload) only
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_put_small_file_with_upload_failed_then_throw_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutObjectReturns50x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).isInstanceOf(ContentAddressableStorageException.class);

        // Expected PUT (upload) only
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_put_small_file_with_read_failed_then_throw_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutObjectReturns20x();
        givenGetObjectReturns404();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).isInstanceOf(ContentAddressableStorageException.class);

        // Expected PUT (upload) + GET (read to recompute digest)
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
                .withRequestBody(WireMock.binaryEqualTo(data))
        );
        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_put_small_file_with_inconsistent_digest_read_then_throw_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        byte[] bad_data = "BAD_DATA_DIGEST".getBytes(StandardCharsets.UTF_8);

        givenPutObjectReturns20x();
        givenGetObjectReturns20x(bad_data);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        )
            .isInstanceOf(ContentAddressableStorageException.class)
            .hasMessageContaining(" is not equal to computed digest ");

        // Expected PUT (upload) + GET (read to recompute digest)
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withoutHeader(X_OBJECT_MANIFEST)
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
                .withRequestBody(WireMock.binaryEqualTo(data))
        );
        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_put_large_file_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutLargeObjectPartReturns20x("/swift/v1/0_object/3500.txt/\\d{8}");
        givenPutObjectReturns20x();
        givenGetObjectReturns20x(data);
        givenPostObjetReturns20x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 300L);

        // When / Then
        assertThatCode(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).doesNotThrowAnyException();

        // Expected 13x PUT (12x parts + 1x manifest) + GET (read to recompute digest) + POST (update metadata)
        for (int i = 0; i < 11; i++) {
            verifySwiftRequest(
                putRequestedFor(
                    WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/" + String.format("%08d", i + 1))
                ).withRequestBody(WireMock.binaryEqualTo(Arrays.copyOfRange(data, i * 300, (i + 1) * 300)))
            );
        }

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/00000012"))
                .withRequestBody(WireMock.binaryEqualTo(Arrays.copyOfRange(data, 3_300, 3_500)))
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_MANIFEST, equalTo("0_object/3500.txt/"))
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        verifySwiftRequest(
            postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_META_DIGEST_TYPE, equalTo("SHA-512"))
                .withHeader(X_OBJECT_META_DIGEST, equalTo(sha512sum(data)))
                .withHeader(X_OBJECT_MANIFEST, equalTo("0_object/3500.txt/"))
                .withHeader(VITAM_CUSTOMIZED_HEADER_KEY, equalTo(VITAM_CUSTOMIZED_HEADER_VALUE))
        );

        assertSwiftRequestCountEqualsTo(15);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(15);
    }

    @Test
    public void when_put_large_file_with_upload_failed_then_throw_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutLargeObjectPartReturns20x("/swift/v1/0_object/3500.txt/00000001");
        givenPutLargeObjectPartReturns50x("/swift/v1/0_object/3500.txt/00000002");

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 1_500L);

        // When / Then
        assertThatThrownBy(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).isInstanceOf(ContentAddressableStorageException.class);

        // Expected 2x PUT
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/00000001")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );
        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/00000002")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_get_object_metadata_and_cache_activated_then_return_metadata() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        givenHeadObjectReturns20x(data);

        // When
        MetadatasObject objectMetadata = swift.getObjectMetadata(CONTAINER_NAME, OBJECT_NAME, false);

        // Then
        assertThat(objectMetadata.getObjectName()).isEqualTo(OBJECT_NAME);
        assertThat(objectMetadata.getDigest()).isEqualTo(sha512sum(data));
        assertThat(objectMetadata.getType()).isEqualTo("object");

        // Expected 1x HEAD
        verifySwiftRequests(
            headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            ),
            2
        );

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_get_object_metadata_with_deactivated_cache_and_lowercase_headers_response_then_return_metadata()
        throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        givenHeadObjectReturns20xWithLowerCaseHeaders(data);
        givenGetObjectReturns20x(data);

        // When
        MetadatasObject objectMetadata = swift.getObjectMetadata(CONTAINER_NAME, OBJECT_NAME, true);

        // Then
        assertThat(objectMetadata.getObjectName()).isEqualTo(OBJECT_NAME);
        assertThat(objectMetadata.getDigest()).isEqualTo(sha512sum(data));
        assertThat(objectMetadata.getType()).isEqualTo("object");

        // Expected 1x HEAD
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(2);
    }

    @Test
    public void when_get_object_metadata_of_not_found_object_then_throw_not_found_exception() throws Exception {
        // Given
        String containerName = "0_object";
        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        givenHeadObjectReturns404();

        // When/Then
        assertThatThrownBy(() -> swift.getObjectMetadata(containerName, OBJECT_NAME, false)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );

        // Expected 1x HEAD
        verifySwiftRequest(
            headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_metadata_and_server_error_then_throw_exception() throws Exception {
        // Given
        String containerName = "0_object";
        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        givenHeadObjectReturns50x();

        // When/Then
        assertThatThrownBy(() -> swift.getObjectMetadata(containerName, OBJECT_NAME, false)).isInstanceOf(
            ContentAddressableStorageException.class
        );

        // Expected 1x HEAD
        verifySwiftRequest(
            headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_file_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenGetObjectReturns20x(data);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        ObjectContent swiftObject = swift.getObject(CONTAINER_NAME, OBJECT_NAME);

        // Then
        assertThat(swiftObject.getInputStream()).hasSameContentAs(new ByteArrayInputStream(data));
        assertThat(swiftObject.getSize()).isEqualTo(3_500L);

        // Expected GET
        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_file_not_found_then_throw_not_found_exception() throws Exception {
        // Given
        givenGetObjectReturns404();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(() -> swift.getObject(CONTAINER_NAME, OBJECT_NAME)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );

        // Expected GET
        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_file_with_server_error_then_throw_exception() throws Exception {
        // Given
        givenGetObjectReturns50x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(() -> swift.getObject(CONTAINER_NAME, OBJECT_NAME)).isInstanceOf(
            ContentAddressableStorageException.class
        );

        // Expected GET
        verifySwiftRequest(
            getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                VITAM_CUSTOMIZED_HEADER_KEY,
                equalTo(VITAM_CUSTOMIZED_HEADER_VALUE)
            )
        );

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_with_cache_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenHeadObjectReturns20x(data);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        String objectDigest = swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false);

        // Then
        assertThat(objectDigest).isEqualTo(sha512sum(data));

        // Expected HEAD
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_with_cache_miss_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        String digest = sha512sum(data);

        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(
                aResponse()
                    .withStatus(201)
                    .withBody(data)
                    .withHeader(ETAG, "etag")
                    .withHeader("Last-Modified", "Mon, 26 Feb 2018 11:33:40 GMT")
            )
        );

        givenGetObjectReturns20x(data);
        // This POST REquest is used to update metadata object in case it does not exist ( even when getting the object )
        givenPostObjetReturns20x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        String objectDigest = swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false);

        // Then
        assertThat(objectDigest).isEqualTo(digest);

        // Expected HEAD + GET
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));
        verifySwiftRequest(postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(3);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(3);
    }

    @Test
    public void when_get_object_digest_without_cache_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        String digest = sha512sum(data);

        givenGetObjectReturns20x(data);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        String objectDigest = swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, true);

        // Then
        assertThat(objectDigest).isEqualTo(digest);

        // Expected GET
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_with_cache_not_found_then_throw_not_found_exception() throws Exception {
        // Given
        givenHeadObjectReturns404();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () -> swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false)
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // Expected HEAD
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_with_cache_with_server_error_then_throw_exception() throws Exception {
        // Given
        givenHeadObjectReturns50x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () -> swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false)
        ).isInstanceOf(ContentAddressableStorageException.class);

        // Expected HEAD
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_without_cache_not_found_then_throw_not_found_exception() throws Exception {
        // Given
        givenGetObjectReturns404();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () -> swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, true)
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // Expected GET
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_get_object_digest_without_cache_with_server_error_then_throw_exception() throws Exception {
        // Given
        givenGetObjectReturns50x();

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(
            () -> swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, true)
        ).isInstanceOf(ContentAddressableStorageException.class);

        // Expected GET
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_delete_object_then_ok() throws Exception {
        // Given
        givenDeleteObjectReturns20x();
        swiftInstanceRule.stubFor(
            get(urlPathEqualTo("/swift/v1/0_object"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("marker", absent())
                .willReturn(aResponse().withStatus(200).withBody(JsonHandler.fromPojoToBytes(Collections.emptyList())))
        );

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatCode(() -> swift.deleteObject(CONTAINER_NAME, OBJECT_NAME)).doesNotThrowAnyException();

        // Expected DELETE
        verifySwiftRequest(deleteRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        // GET + DELETE
        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_delete_object_not_found_then_throw_not_found_exception() throws Exception {
        // Given
        givenDeleteObjectReturns404();
        swiftInstanceRule.stubFor(
            get(urlPathEqualTo("/swift/v1/0_object"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("marker", absent())
                .willReturn(aResponse().withStatus(200).withBody(JsonHandler.fromPojoToBytes(Collections.emptyList())))
        );

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(() -> swift.deleteObject(CONTAINER_NAME, OBJECT_NAME)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );

        // Expected DELETE
        verifySwiftRequest(deleteRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_delete_object_with_server_error_then_throw_exception() throws Exception {
        // Given
        givenDeleteObjectReturns50x();
        swiftInstanceRule.stubFor(
            get(urlPathEqualTo("/swift/v1/0_object"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("marker", absent())
                .willReturn(aResponse().withStatus(200).withBody(JsonHandler.fromPojoToBytes(Collections.emptyList())))
        );

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        assertThatThrownBy(() -> swift.deleteObject(CONTAINER_NAME, OBJECT_NAME)).isInstanceOf(
            ContentAddressableStorageException.class
        );

        // Expected DELETE
        verifySwiftRequest(deleteRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(2);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(2);
    }

    @Test
    public void when_list_container_objects_of_empty_container_then_ok() throws Exception {
        // Given

        swiftInstanceRule.stubFor(
            get(urlPathEqualTo("/swift/v1/0_object"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("marker", absent())
                .willReturn(aResponse().withStatus(200).withBody(JsonHandler.fromPojoToBytes(Collections.emptyList())))
        );

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        List<ObjectEntry> objectEntries = new ArrayList<>();
        swift.listContainer(CONTAINER_NAME, objectEntries::add);

        // Then
        assertThat(objectEntries).isEmpty();

        // Expected 1x GET
        verifySwiftRequest(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_list_container_objects_then_ok() throws Exception {
        // Given
        List<String> objectNames = IntStream.range(1000, 1250).mapToObj(i -> "obj" + i).collect(Collectors.toList());

        Map<String, Long> objectSizes = objectNames
            .stream()
            .collect(
                Collectors.toMap(objectName -> objectName, objectName -> RandomUtils.nextLong(0L, 4_000_000_000_000L))
            );

        List<List<String>> objectNameBulks = ListUtils.partition(objectNames, 100);
        for (int i = 0; i < 4; i++) {
            List<String> bulkObjectNames = i < objectNameBulks.size()
                ? objectNameBulks.get(i)
                : Collections.emptyList();

            swiftInstanceRule.stubFor(
                get(urlPathEqualTo("/swift/v1/0_object"))
                    .withQueryParam("format", equalTo("json"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam(
                        "marker",
                        i == 0
                            ? absent()
                            : equalTo(objectNameBulks.get(i - 1).get(objectNameBulks.get(i - 1).size() - 1))
                    )
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withBody(
                                JsonHandler.fromPojoToBytes(
                                    bulkObjectNames
                                        .stream()
                                        .map(
                                            objectName ->
                                                JsonHandler.createObjectNode()
                                                    .put("name", objectName)
                                                    .put("bytes", objectSizes.get(objectName))
                                        )
                                        .collect(Collectors.toList())
                                )
                            )
                    )
            );
        }

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        List<ObjectEntry> objectEntries = new ArrayList<>();
        swift.listContainer(CONTAINER_NAME, objectEntries::add);

        // Then
        assertThat(objectEntries).hasSize(250);

        assertThat(objectEntries)
            .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
            .containsExactly(
                IntStream.range(1000, 1250)
                    .mapToObj(i -> "obj" + i)
                    .map(objectName -> tuple(objectName, objectSizes.get(objectName)))
                    .toArray(Tuple[]::new)
            );

        // Ensure all object names found
        assertThat(objectEntries)
            .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
            .containsExactly(
                IntStream.range(1000, 1250)
                    .mapToObj(i -> "obj" + i)
                    .map(objectName -> tuple(objectName, objectSizes.get(objectName)))
                    .toArray(Tuple[]::new)
            );

        // Expected 4x GET
        verifySwiftRequests(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object")), 4);

        assertSwiftRequestCountEqualsTo(4);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(4);
    }

    @Test
    public void when_list_container_objects_with_large_object_segments_then_ok() throws Exception {
        // Given
        List<String> objectNames = Stream.concat(
            IntStream.range(1000, 1250).mapToObj(i -> "obj" + i),
            IntStream.range(0, 5).mapToObj(segmentIndex -> "obj1050/0000000" + segmentIndex)
        )
            .sorted()
            .collect(Collectors.toList());

        Map<String, Long> rawObjectSizes = objectNames
            .stream()
            .collect(
                Collectors.toMap(
                    objectName -> objectName,
                    objectName -> objectName.equals("obj1050") ? 0L : RandomUtils.nextLong(0L, 4_000_000_000_000L)
                )
            );

        List<List<String>> objectNameBulks = ListUtils.partition(objectNames, 100);
        for (int i = 0; i < 4; i++) {
            List<String> bulkObjectNames = i < objectNameBulks.size()
                ? objectNameBulks.get(i)
                : Collections.emptyList();

            swiftInstanceRule.stubFor(
                get(urlPathEqualTo("/swift/v1/0_object"))
                    .withQueryParam("format", equalTo("json"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam(
                        "marker",
                        i == 0
                            ? absent()
                            : equalTo(objectNameBulks.get(i - 1).get(objectNameBulks.get(i - 1).size() - 1))
                    )
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withBody(
                                JsonHandler.fromPojoToBytes(
                                    bulkObjectNames
                                        .stream()
                                        .map(
                                            objectName ->
                                                JsonHandler.createObjectNode()
                                                    .put("name", objectName)
                                                    .put("bytes", rawObjectSizes.get(objectName))
                                        )
                                        .collect(Collectors.toList())
                                )
                            )
                    )
            );
        }

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        List<ObjectEntry> objectEntries = new ArrayList<>();
        swift.listContainer(CONTAINER_NAME, objectEntries::add);

        // Then
        assertThat(objectEntries).hasSize(250);

        // Ensure segment names are ignored
        assertThat(objectEntries)
            .extracting(ObjectEntry::getObjectId)
            .doesNotContainAnyElementsOf(
                IntStream.range(0, 5)
                    .mapToObj(segmentIndex -> "obj1050/0000000" + segmentIndex)
                    .collect(Collectors.toList())
            );

        // Ensure all object names found
        long obj1050TotalSize = IntStream.range(0, 5)
            .mapToObj(segmentIndex -> "obj1050/0000000" + segmentIndex)
            .mapToLong(rawObjectSizes::get)
            .sum();

        Map<String, Long> totalObjectSizes = IntStream.range(1000, 1250)
            .mapToObj(i -> "obj" + i)
            .collect(
                Collectors.toMap(
                    objectName -> objectName,
                    objectName -> objectName.equals("obj1050") ? obj1050TotalSize : rawObjectSizes.get(objectName)
                )
            );

        assertThat(objectEntries)
            .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
            .containsExactly(
                IntStream.range(1000, 1250)
                    .mapToObj(i -> "obj" + i)
                    .map(objectName -> tuple(objectName, totalObjectSizes.get(objectName)))
                    .toArray(Tuple[]::new)
            );

        // Expected 4x GET
        verifySwiftRequests(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object")), 4);

        assertSwiftRequestCountEqualsTo(4);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(4);
    }

    @Test
    public void when_list_container_objects_and_server_error_then_throw_exception() throws Exception {
        // Given
        swiftInstanceRule.stubFor(get(urlPathEqualTo("/swift/v1/0_object")).willReturn(aResponse().withStatus(500)));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When / Then
        List<ObjectEntry> objectEntries = new ArrayList<>();

        assertThatThrownBy(() -> swift.listContainer(CONTAINER_NAME, objectEntries::add)).isInstanceOf(
            ContentAddressableStorageException.class
        );

        assertThat(objectEntries).isEmpty();

        // Expected 1x GET
        verifySwiftRequest(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object")));

        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void when_put_large_file_and_disabled_vitam_cookie_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutLargeObjectPartReturns20x("/swift/v1/0_object/3500.txt/\\d{8}");
        givenPutObjectReturns20x();
        givenGetObjectReturns20x(data);
        givenPostObjetReturns20x();

        // Disable vitam cookie
        configuration.setEnableCustomHeaders(false);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 300L);

        // When / Then
        assertThatCode(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).doesNotThrowAnyException();

        // Expected 13x PUT (12x parts + 1x manifest) + GET (read to recompute digest) + POST (update metadata)
        for (int i = 0; i < 11; i++) {
            verifySwiftRequest(
                putRequestedFor(
                    WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/" + String.format("%08d", i + 1))
                ).withRequestBody(WireMock.binaryEqualTo(Arrays.copyOfRange(data, i * 300, (i + 1) * 300)))
            );
        }

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/00000012")).withRequestBody(
                WireMock.binaryEqualTo(Arrays.copyOfRange(data, 3_300, 3_500))
            )
        );

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                X_OBJECT_MANIFEST,
                equalTo("0_object/3500.txt/")
            )
        );

        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        verifySwiftRequest(
            postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_META_DIGEST_TYPE, equalTo("SHA-512"))
                .withHeader(X_OBJECT_META_DIGEST, equalTo(sha512sum(data)))
                .withHeader(X_OBJECT_MANIFEST, equalTo("0_object/3500.txt/"))
        );

        assertSwiftRequestCountEqualsTo(15);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(0);
    }

    @Test
    public void when_get_object_digest_with_used_cache_and_disabled_vitam_cookie_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        String digest = sha512sum(data);

        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(
                aResponse()
                    .withStatus(201)
                    .withBody(data)
                    .withHeader(ETAG, "etag")
                    .withHeader("Last-Modified", "Mon, 26 Feb 2018 11:33:40 GMT")
            )
        );

        givenGetObjectReturns20x(data);
        // This POST Request is used to update metadata object in case it does not exist ( even when getting the object )
        givenPostObjetReturns20x();

        // Disable vitam cookie
        configuration.setEnableCustomHeaders(false);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        String objectDigest = swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false);

        // Then
        assertThat(objectDigest).isEqualTo(digest);

        // Expected HEAD + GET
        verifySwiftRequest(headRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));
        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));
        verifySwiftRequest(postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        assertSwiftRequestCountEqualsTo(3);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(0);
    }

    @Test
    public void when_get_object_digest_with_cache_miss_and_null_vitam_cookie_property_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));

        givenPutLargeObjectPartReturns20x("/swift/v1/0_object/3500.txt/\\d{8}");
        givenPutObjectReturns20x();
        givenGetObjectReturns20x(data);
        givenPostObjetReturns20x();

        // Simulate Inexistant vitam cookie property
        configuration.setEnableCustomHeaders(null);

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 300L);

        // When / Then
        assertThatCode(
            () ->
                swift.putObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    new ByteArrayInputStream(data),
                    VitamConfiguration.getDefaultDigestType(),
                    3_500L
                )
        ).doesNotThrowAnyException();

        // Expected 13x PUT (12x parts + 1x manifest) + GET (read to recompute digest) + POST (update metadata)
        for (int i = 0; i < 11; i++) {
            verifySwiftRequest(
                putRequestedFor(
                    WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/" + String.format("%08d", i + 1))
                ).withRequestBody(WireMock.binaryEqualTo(Arrays.copyOfRange(data, i * 300, (i + 1) * 300)))
            );
        }

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt/00000012")).withRequestBody(
                WireMock.binaryEqualTo(Arrays.copyOfRange(data, 3_300, 3_500))
            )
        );

        verifySwiftRequest(
            putRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")).withHeader(
                X_OBJECT_MANIFEST,
                equalTo("0_object/3500.txt/")
            )
        );

        verifySwiftRequest(getRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt")));

        verifySwiftRequest(
            postRequestedFor(WireMock.urlEqualTo("/swift/v1/0_object/3500.txt"))
                .withHeader(X_OBJECT_META_DIGEST_TYPE, equalTo("SHA-512"))
                .withHeader(X_OBJECT_META_DIGEST, equalTo(sha512sum(data)))
                .withHeader(X_OBJECT_MANIFEST, equalTo("0_object/3500.txt/"))
        );

        assertSwiftRequestCountEqualsTo(15);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(0);
    }

    @Test
    public void get_object_digest_with_cache_and_object_not_found_then_ok() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        givenHeadObjectReturns20x(data);
        givenGetObjectReturns404();
        Swift swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        String objectDigest = swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, false);

        // Then
        assertThat(objectDigest).isEqualTo(sha512sum(data));
        verifySwiftRequests(headRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object/3500.txt")), 1);
        verifySwiftRequests(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object/3500.txt")), 0);
        assertSwiftRequestCountEqualsTo(1);
        assertThat(getAllRequestsWithVitamCustomizedHeadersSize()).isEqualTo(1);
    }

    @Test
    public void get_object_digest_without_cache_and_object_not_found_then_throw_exception() throws Exception {
        // Given
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME));
        givenHeadObjectReturns20x(data);
        givenGetObjectReturns404();
        Swift swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);

        // When
        assertThatThrownBy(
            () -> swift.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, DigestType.SHA512, true)
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // Then
        verifySwiftRequests(headRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object/3500.txt")), 0);
        verifySwiftRequests(getRequestedFor(WireMock.urlPathEqualTo("/swift/v1/0_object/3500.txt")), 1);
        assertSwiftRequestCountEqualsTo(1);
    }

    private void givenPutObjectReturns20x() {
        swiftInstanceRule.stubFor(
            put(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(201))
        );
    }

    private void givenPutObjectReturns404() {
        swiftInstanceRule.stubFor(
            put(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(404))
        );
    }

    private void givenPutObjectReturns50x() {
        swiftInstanceRule.stubFor(
            put(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(502))
        );
    }

    private void givenPutLargeObjectPartReturns20x(String path) {
        swiftInstanceRule.stubFor(put(urlMatching(path)).willReturn(aResponse().withStatus(202)));
    }

    private void givenPutLargeObjectPartReturns50x(String path) {
        swiftInstanceRule.stubFor(put(urlMatching(path)).willReturn(aResponse().withStatus(502)));
    }

    private void givenHeadObjectReturns20x(byte[] data) {
        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(
                aResponse()
                    .withStatus(201)
                    .withBody(data)
                    .withHeader(ETAG, "etag")
                    .withHeader(X_OBJECT_META_DIGEST, sha512sum(data))
                    .withHeader(X_OBJECT_META_DIGEST_TYPE, "SHA-512")
                    .withHeader("Last-Modified", "Mon, 26 Feb 2018 11:33:40 GMT")
            )
        );
    }

    private void givenHeadObjectReturns20xWithLowerCaseHeaders(byte[] data) {
        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(
                aResponse()
                    .withStatus(201)
                    .withBody(data)
                    .withHeader(ETAG.toLowerCase(), "etag")
                    .withHeader(X_OBJECT_META_DIGEST.toLowerCase(), sha512sum(data))
                    .withHeader(X_OBJECT_META_DIGEST_TYPE.toLowerCase(), "SHA-512")
                    .withHeader("Last-Modified".toLowerCase(), "Mon, 26 Feb 2018 11:33:40 GMT")
            )
        );
    }

    private void givenHeadObjectReturns404() {
        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(404))
        );
    }

    private void givenHeadObjectReturns50x() {
        swiftInstanceRule.stubFor(
            head(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(502))
        );
    }

    private void givenGetObjectReturns20x(byte[] data) {
        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(
                aResponse().withStatus(200).withHeader(CONTENT_TYPE, "application/octet-stream").withBody(data)
            )
        );
    }

    private void givenGetObjectReturns404() {
        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(404))
        );
    }

    private void givenGetObjectReturns50x() {
        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(502))
        );
    }

    private void givenPostObjetReturns20x() {
        swiftInstanceRule.stubFor(
            post(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(202))
        );
    }

    private void givenDeleteObjectReturns20x() {
        swiftInstanceRule.stubFor(
            delete(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(204))
        );
    }

    private void givenDeleteObjectReturns404() {
        swiftInstanceRule.stubFor(
            delete(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(404))
        );
    }

    private void givenDeleteObjectReturns50x() {
        swiftInstanceRule.stubFor(
            delete(urlMatching("/swift/v1/0_object/3500.txt")).willReturn(aResponse().withStatus(502))
        );
    }

    private void verifySwiftRequest(RequestPatternBuilder requestPatternBuilder) {
        assertThat(swiftInstanceRule.findRequestsMatching(requestPatternBuilder.build()).getRequests()).hasSize(1);
    }

    private void verifySwiftRequests(RequestPatternBuilder requestPatternBuilder, int count) {
        assertThat(swiftInstanceRule.findRequestsMatching(requestPatternBuilder.build()).getRequests()).hasSize(count);
    }

    private void assertSwiftRequestCountEqualsTo(int expectedCount) {
        try {
            assertThat(swiftInstanceRule.countRequestsMatching(RequestPattern.everything()).getCount()).isEqualTo(
                expectedCount
            );
        } catch (AssertionError e) {
            for (ServeEvent serveEvent : swiftInstanceRule.getAllServeEvents()) {
                System.out.println(serveEvent.getRequest().toString());
            }
            throw e;
        }
    }

    private String sha512sum(byte[] data) {
        return new Digest(DigestType.SHA512).update(data).digestHex();
    }

    private Long getAllRequestsWithVitamCustomizedHeadersSize() {
        return swiftInstanceRule
            .findAll(RequestPatternBuilder.allRequests())
            .stream()
            .map(elmt -> elmt.getHeaders().getHeader(VITAM_CUSTOMIZED_HEADER_KEY))
            .filter(elmt -> elmt.containsValue(VITAM_CUSTOMIZED_HEADER_VALUE))
            .count();
    }
}
