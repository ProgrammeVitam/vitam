/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.securityInternal.migration.test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamRestTestClient;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.IdentityMain;
import fr.gouv.vitam.security.internal.rest.server.InternalSecurityConfiguration;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.security.internal.common.model.CertificateBaseModel.STATUS_TAG;
import static fr.gouv.vitam.security.internal.rest.repository.IdentityRepository.CERTIFICATE_COLLECTION;
import static fr.gouv.vitam.security.internal.rest.repository.PersonalRepository.PERSONAL_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SecurityInternalMigrationIT extends VitamRuleRunner {

    private static final String IDENTITY_CONF = "security-internal/security-internal-test.conf";

    private final static String IDENTITY_CERT_FILE = "/security-internal/sia-client-external.crt";

    private final static String PERSONAL_CERT_FILE = "/security-internal/personal-client-external.crt";

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    private static InternalSecurityClient internalSecurityClient;
    private static IdentityMain identityMain;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        File securityInternalConfigurationFile = PropertiesUtils.findFile(IDENTITY_CONF);
        final InternalSecurityConfiguration internalSecurityConfiguration =
            PropertiesUtils.readYaml(securityInternalConfigurationFile, InternalSecurityConfiguration.class);
        internalSecurityConfiguration.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        PropertiesUtils.writeYaml(securityInternalConfigurationFile, internalSecurityConfiguration);

        identityMain = new IdentityMain(securityInternalConfigurationFile.getAbsolutePath());

        identityMain.start();
        internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void shutdownAfterClass() throws Exception {
        runAfter();
        if (identityMain != null) {
            identityMain.stop();
        }
        if (internalSecurityClient != null) {
            internalSecurityClient.close();
        }
    }


    @Before
    public void setUp() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void shoulMigrateCertificateFromV7ToV8Model() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(1);

        // Given certifcates without Status field in DB
        mongoRule.getMongoCollection(CERTIFICATE_COLLECTION)
            .insertOne(buildCertificateDocument("01", IDENTITY_CERT_FILE));
        mongoRule.getMongoCollection(PERSONAL_COLLECTION).insertOne(buildCertificateDocument("01", IDENTITY_CERT_FILE));

        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find()).size()).isEqualTo(1);
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find()).size()).isEqualTo(1);
        //with Status filter, no result
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find(eq(STATUS_TAG,
            CertificateStatus.VALID.name()))).size()).isEqualTo(0);
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find(eq(STATUS_TAG,
            CertificateStatus.VALID.name()))).size()).isEqualTo(0);

        final TestVitamClientFactory<DefaultClient> testAdminClientFactory =
            new TestVitamClientFactory<>(29003, "/v1/api"),
            testBusinessClientFactory =
                new TestVitamClientFactory<>(8208, "/v1/api");

        VitamRestTestClient adminRestClient = new VitamRestTestClient(testAdminClientFactory),
            businessRestClient = new VitamRestTestClient(testBusinessClientFactory);

        //Then, when requesting them, we must have no result (Status=VALID filter applied in repositories)
        businessRestClient.given().body(toByteArray(getClass().getResourceAsStream(IDENTITY_CERT_FILE)),
            MediaType.APPLICATION_OCTET_STREAM_TYPE).status(
            Response.Status.NOT_FOUND).when().get("identity");

        businessRestClient.given().body(toByteArray(getClass().getResourceAsStream(PERSONAL_CERT_FILE)),
            MediaType.APPLICATION_OCTET_STREAM_TYPE).status(
            Response.Status.UNAUTHORIZED).get("/personalCertificate/personal-certificate-check/permission");

        //###################################
        //Calling security migration Endpoint to add the missing field
        adminRestClient.given().status(Response.Status.ACCEPTED).addHeader(HttpHeaders.AUTHORIZATION, getBasicAuthnToken())
            .post("security/migration");

        awaitTermination(adminRestClient);

        //Then after migration, we must have result with Status filtring
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(CERTIFICATE_COLLECTION).find(eq(STATUS_TAG,
            CertificateStatus.VALID.name()))).size()).isEqualTo(1);
        assertThat(Lists.newArrayList(mongoRule.getMongoCollection(PERSONAL_COLLECTION).find(eq(STATUS_TAG,
            CertificateStatus.VALID.name()))).size()).isEqualTo(1);

    }

    private Document buildCertificateDocument(String certId, String certFile)
        throws IOException, CertificateException {

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
        return "Basic " + Base64.getEncoder()
            .encodeToString((BASIC_AUTHN_USER + ":" + BASIC_AUTHN_PWD).getBytes(StandardCharsets.UTF_8));
    }

    private void awaitTermination(VitamRestTestClient adminRestClient)
        throws InterruptedException, IOException, VitamClientInternalException {

        // Wait for 30 seconds max
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            int responseStatus = adminRestClient.given().addHeader("Authorization", getBasicAuthnToken())
                .get("/security/migration/status");

            if (responseStatus == Response.Status.NOT_FOUND.getStatusCode())
                return;
        }

        fail("Security migration termination took too long");
    }
}
