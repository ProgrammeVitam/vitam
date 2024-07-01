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
package fr.gouv.vitam.securityInternal.integration.test;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamRestTestClient;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.IdentityMain;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.security.internal.common.model.CertificateBaseModel.STATUS_TAG;
import static fr.gouv.vitam.security.internal.rest.repository.IdentityRepository.CERTIFICATE_COLLECTION;
import static fr.gouv.vitam.security.internal.rest.repository.PersonalRepository.PERSONAL_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class SecurityInternalIT extends VitamRuleRunner {

    private static final String EMPTY_CRL_FILE = "/security-internal/signing-ca-no-revoked-cert-yet.crl";

    private static final String CRL_SIA_REVOKED_FILE = "/security-internal/signing-ca-with-revoked-cert.crl";

    private static final String IDENTITY_CERT_FILE = "/security-internal/my-sia.crt";

    private static final String PERSONAL_CERT_FILE = "/security-internal/my-personal-certificate.crt";

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        SecurityInternalIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(IdentityMain.class)
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
    }

    @AfterClass
    public static void shutdownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        // Only started in this IT test
        runner.stopIdentityServer(true);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_valid_certificate_transmitted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        //WHEN //THEN
        assertThatThrownBy(
            () ->
                InternalSecurityClientFactory.getInstance().getClient().checkPersonalCertificate(certificate, "tt:read")
        ).isInstanceOf(InternalSecurityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmitted() {
        // When / Then
        VitamThreadUtils.getVitamSession().setTenantId(0);
        assertThatThrownBy(
            () -> InternalSecurityClientFactory.getInstance().getClient().checkPersonalCertificate(null, "tt:read")
        ).isInstanceOf(InternalSecurityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_valid_certificate_transmitted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        should_create_certificate_transmitted();
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        //when
        InternalSecurityClientFactory.getInstance().getClient().checkPersonalCertificate(certificate, "status:read");
    }

    //Admin Test resources
    @Test
    @RunWithCustomExecutor
    public void should_create_certificate_transmitted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        String url = "http://localhost:" + runner.PORT_SERVICE_IDENTITY_ADMIN + "/v1/api/personalCertificate";
        // When
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        HttpEntity entity = new ByteArrayEntity(certificate);
        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        //Then
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(204);
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void should_delete_one_certificate() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        should_create_certificate_transmitted();
        String url = "http://localhost:" + runner.PORT_SERVICE_IDENTITY_ADMIN + "/v1/api/personalCertificate";
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(1);
        //WHEN
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost delete = new HttpPost(url) {
            @Override
            public String getMethod() {
                return "DELETE";
            }
        };

        HttpEntity entity = new ByteArrayEntity(certificate);
        delete.setEntity(entity);
        HttpResponse response = client.execute(delete);
        //THEN
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(204);
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(0);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldControlCertificateWithCRLTransmitted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(1);
        IdentityInsertModel identityInsertModel = new IdentityInsertModel();
        identityInsertModel.setContextId("CT-00001");
        identityInsertModel.setCertificate(toByteArray(getClass().getResourceAsStream(IDENTITY_CERT_FILE)));

        final TestVitamClientFactory<DefaultClient> testClientFactory = new TestVitamClientFactory<>(
            runner.PORT_SERVICE_IDENTITY_ADMIN,
            "/v1/api"
        );
        VitamRestTestClient restClient = new VitamRestTestClient(testClientFactory);

        restClient
            .given()
            .body(identityInsertModel, MediaType.APPLICATION_JSON_TYPE)
            .status(Response.Status.CREATED)
            .when()
            .post("/identity");

        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find()).size()).isEqualTo(1);

        restClient
            .given()
            .body(
                toByteArray(getClass().getResourceAsStream(PERSONAL_CERT_FILE)),
                MediaType.APPLICATION_OCTET_STREAM_TYPE
            )
            .status(Response.Status.NO_CONTENT)
            .post("/personalCertificate");

        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(1);

        // When empty CRL
        restClient
            .given()
            .body(toByteArray(getClass().getResourceAsStream(EMPTY_CRL_FILE)), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .status(Response.Status.NO_CONTENT)
            .post("/crl");
        // Then
        assertThat(
            Lists.newArrayList(
                mongoRule
                    .getMongoCollection(CERTIFICATE_COLLECTION)
                    .find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(1);

        //When Non empty CRL revoking sia certificate
        restClient
            .given()
            .body(
                toByteArray(getClass().getResourceAsStream(CRL_SIA_REVOKED_FILE)),
                MediaType.APPLICATION_OCTET_STREAM_TYPE
            )
            .status(Response.Status.NO_CONTENT)
            .post("/crl");

        // Then
        assertThat(
            Lists.newArrayList(
                mongoRule
                    .getMongoCollection(CERTIFICATE_COLLECTION)
                    .find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(0);
        assertThat(
            Lists.newArrayList(
                mongoRule.getMongoCollection(PERSONAL_COLLECTION).find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void shoulMigrateCertificateFromV7ToV8Model() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        // initial collections size
        int initialCertifCollectionSize = Lists.newArrayList(
            mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find()
        ).size();
        int initialCertifPersoCollectionSize = Lists.newArrayList(
            mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()
        ).size();

        // Given certifcates without Status field in DB
        mongoRule
            .getMongoCollection(CERTIFICATE_COLLECTION)
            .insertOne(buildCertificateDocument("01", IDENTITY_CERT_FILE));
        mongoRule.getMongoCollection(PERSONAL_COLLECTION).insertOne(buildCertificateDocument("01", IDENTITY_CERT_FILE));

        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find()).size()).isEqualTo(
            initialCertifCollectionSize + 1
        );
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(
            initialCertifPersoCollectionSize + 1
        );
        //with Status filter, no result
        assertThat(
            Lists.newArrayList(
                mongoRule
                    .getMongoCollection(CERTIFICATE_COLLECTION)
                    .find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(0);
        assertThat(
            Lists.newArrayList(
                mongoRule.getMongoCollection(PERSONAL_COLLECTION).find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(0);

        final TestVitamClientFactory<DefaultClient> testAdminClientFactory = new TestVitamClientFactory<>(
            runner.PORT_SERVICE_IDENTITY_ADMIN,
            "/v1/api"
        ), testBusinessClientFactory = new TestVitamClientFactory<>(runner.PORT_SERVICE_IDENTITY, "/v1/api");

        VitamRestTestClient adminRestClient = new VitamRestTestClient(testAdminClientFactory), businessRestClient =
            new VitamRestTestClient(testBusinessClientFactory);

        //Then, when requesting them, we must have no result (Status=VALID filter applied in repositories)
        businessRestClient
            .given()
            .body(
                toByteArray(getClass().getResourceAsStream(IDENTITY_CERT_FILE)),
                MediaType.APPLICATION_OCTET_STREAM_TYPE
            )
            .status(Response.Status.NOT_FOUND)
            .when()
            .get("identity");

        businessRestClient
            .given()
            .body(
                toByteArray(getClass().getResourceAsStream(PERSONAL_CERT_FILE)),
                MediaType.APPLICATION_OCTET_STREAM_TYPE
            )
            .status(Response.Status.UNAUTHORIZED)
            .get("/personalCertificate/personal-certificate-check/permission");

        //###################################
        //Calling security migration Endpoint to add the missing field
        adminRestClient
            .given()
            .status(Response.Status.ACCEPTED)
            .addHeader(HttpHeaders.AUTHORIZATION, getBasicAuthnToken())
            .post("security/migration");

        awaitTermination(adminRestClient);

        //Then after migration, we must have result with Status filtring
        assertThat(
            Lists.newArrayList(
                mongoRule
                    .getMongoCollection(CERTIFICATE_COLLECTION)
                    .find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(1);
        assertThat(
            Lists.newArrayList(
                mongoRule.getMongoCollection(PERSONAL_COLLECTION).find(eq(STATUS_TAG, CertificateStatus.VALID.name()))
            ).size()
        ).isEqualTo(1);
    }

    private Document buildCertificateDocument(String certId, String certFile) throws IOException, CertificateException {
        X509Certificate cert = X509PKIUtil.parseX509Certificate(toByteArray(getClass().getResourceAsStream(certFile)));

        Document identityModelDoc = new Document();

        identityModelDoc.put("_id", certId);
        identityModelDoc.put("SerialNumber", cert.getSerialNumber().intValue());
        identityModelDoc.put("IssuerDN", cert.getIssuerDN().getName());
        identityModelDoc.put("Certificate", cert.getEncoded());
        identityModelDoc.put("SubjectDN", cert.getSubjectDN().getName());

        return identityModelDoc;
    }

    private String getBasicAuthnToken() {
        return (
            "Basic " +
            Base64.getEncoder()
                .encodeToString((BASIC_AUTHN_USER + ":" + BASIC_AUTHN_PWD).getBytes(StandardCharsets.UTF_8))
        );
    }

    private void awaitTermination(VitamRestTestClient adminRestClient)
        throws InterruptedException, IOException, VitamClientInternalException {
        // Wait for 30 seconds max
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            int responseStatus = adminRestClient
                .given()
                .addHeader("Authorization", getBasicAuthnToken())
                .get("/security/migration/status");

            if (responseStatus == Response.Status.NOT_FOUND.getStatusCode()) return;
        }

        fail("Security migration termination took too long");
    }
}
