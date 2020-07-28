/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    private StorageConfiguration configuration;

    private static JunitHelper junitHelper = JunitHelper.getInstance();

    private static int swiftPort = junitHelper.findAvailablePort();
    private static int keystonePort = junitHelper.findAvailablePort();
    private static final String CONTAINER_NAME = "containerName";
    private static final String OBJECT_NAME = "3500.txt";

    @ClassRule
    public static WireMockClassRule swiftWireMockRule = new WireMockClassRule(swiftPort);

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

        String bodyResponse =
            JsonHandler.prettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile("keystone.json")));

        keystoneInstanceRule.stubFor(post(urlMatching("/v3/auth/tokens")).willReturn
            (aResponse().withStatus(201)
                .withHeader(X_SUBJECT_TOKEN, token)
                .withHeader(X_OPENSTACK_REQUEST_ID, "req-ac910733-aa54-465a-a59f-3cfd699dd1ab")
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(bodyResponse)));

        swiftInstanceRule.stubFor(head(urlMatching("/swift/v1/containerName/3500.txt"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Length", "1300")
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withHeader(ETAG, "4804428b3615ee40120eeef15169d71b")
                .withHeader("Last-Modified", "Mon, 26 Feb 2018 11:33:40 GMT"))
        );

        swiftInstanceRule.stubFor(put(urlMatching("/swift/v1(.*)")).willReturn(aResponse().withStatus(201)));
    }

    @Test
    public void should_call_smallFile_method_then_swift_offer_doesNotThrowAnyException() throws Exception {
        // Given
        swiftInstanceRule.stubFor(post(urlMatching("/swift/v1(.*)")).willReturn(
            aResponse().withStatus(202)));

        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1(.*)")).willReturn(
                aResponse().withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME)))));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        InputStream stream = PropertiesUtils.getResourceAsStream(OBJECT_NAME);
        // When / Then
        assertThatCode(() -> swift.putObject(CONTAINER_NAME, OBJECT_NAME, stream,
                VitamConfiguration.getDefaultDigestType(), 3_500L)).doesNotThrowAnyException();
    }

    @Test
    public void should_call_smallFile__with_non_matching_digest_recompute_return_error() throws Exception {
        // Given
        swiftInstanceRule.stubFor(post(urlMatching("/swift/v1(.*)")).willReturn(
            aResponse().withStatus(202)));

        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1(.*)")).willReturn(
                aResponse().withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withBody("BAD_FILE_HASH".getBytes())));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        InputStream stream = PropertiesUtils.getResourceAsStream(OBJECT_NAME);
        // When / Then
        assertThatThrownBy(() ->
            swift.putObject(CONTAINER_NAME, OBJECT_NAME, stream, VitamConfiguration.getDefaultDigestType(), 3_500L)
        ).isInstanceOf(ContentAddressableStorageException.class)
            .hasMessageContaining(" is not equal to computed digest ");
    }

    @Test
    public void should_call_smallFile_method_then_swift_offer_doesThrow_ContentAddressableStorageServerException()
        throws Exception {

        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1(.*)")).willReturn(
                aResponse().withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME)))));

        //        // Given
        swiftInstanceRule.stubFor(post(urlMatching("/swift/v1/containerName/3500.txt")).willReturn(
            aResponse().withStatus(500)));
        swiftInstanceRule.stubFor(delete(urlMatching("/swift/v1/containerName/3500.txt")).willReturn(
            aResponse().withStatus(200)));
        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 3_500L);
        InputStream stream = PropertiesUtils.getResourceAsStream(OBJECT_NAME);
        File file = PropertiesUtils.getResourceFile(OBJECT_NAME);
        // When / Then
        assertThatThrownBy(() ->
            swift.putObject(CONTAINER_NAME, OBJECT_NAME, stream, VitamConfiguration.getDefaultDigestType(), file.length())
        ).hasMessage("Cannot put object " + OBJECT_NAME + " on container " + CONTAINER_NAME);
    }

    @Test
    public void should_segmented_inputstream_to_swift_offer() throws Exception {
        // Given
        swiftInstanceRule.stubFor(post(urlMatching("/swift/v1(.*)")).willReturn(
            aResponse().withStatus(202)));

        swiftInstanceRule.stubFor(
            get(urlMatching("/swift/v1(.*)")).willReturn(
                aResponse().withStatus(200)
                    .withHeader(CONTENT_TYPE, "application/octet-stream")
                    .withBody(IOUtils.toByteArray(PropertiesUtils.getResourceAsStream(OBJECT_NAME)))));

        this.swift = new Swift(new SwiftKeystoneFactoryV3(configuration), configuration, 1_500L);
        InputStream stream = PropertiesUtils.getResourceAsStream(OBJECT_NAME);
        // When / Then
        
        assertThatCode(() -> swift.putObject(CONTAINER_NAME, OBJECT_NAME, stream,
                VitamConfiguration.getDefaultDigestType(), 3_500L)).doesNotThrowAnyException();
    }
}
