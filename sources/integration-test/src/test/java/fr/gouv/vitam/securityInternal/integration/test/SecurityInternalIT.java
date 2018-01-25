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
package fr.gouv.vitam.securityInternal.integration.test;

import static com.google.common.io.ByteStreams.toByteArray;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.security.internal.rest.repository.PersonalRepository.PERSONAL_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.InputStream;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.rest.IdentityMain;
import fr.gouv.vitam.security.internal.rest.server.InternalSecurityConfiguration;

public class SecurityInternalIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecurityInternalIT.class);
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    static JunitHelper junitHelper;

    final static String CLUSTER_NAME = "vitam-cluster";
    static MongodProcess mongod;
    static int mongoPort;
    static MongodExecutable mongodExecutable;

    private static IdentityMain identityMain;
    private static final String IDENTITY_CONF = "security-internal/security-internal.conf";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static InternalSecurityClient internalSecurityClient;
    private static MongoClient mongoClient;
    private static MongoCollection<Document> mongoCollection;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        File vitamTempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());

        mongod = mongodExecutable.start();
        mongoClient = new MongoClient(new ServerAddress("localhost", mongoPort), getMongoClientOptions());

        File securityInternalConfigurationFile = PropertiesUtils.findFile(IDENTITY_CONF);
        final InternalSecurityConfiguration internalSecurityConfiguration =
            PropertiesUtils.readYaml(securityInternalConfigurationFile, InternalSecurityConfiguration.class);
        internalSecurityConfiguration.getMongoDbNodes().get(0).setDbPort(mongoPort);
        PropertiesUtils.writeYaml(securityInternalConfigurationFile, internalSecurityConfiguration);

        identityMain = new IdentityMain(IDENTITY_CONF);

        identityMain.start();
        internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void shutdownAfterClass() throws Exception {
        junitHelper.releasePort(mongoPort);
        if (identityMain != null) {
            identityMain.stop();
        }
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        if (internalSecurityClient != null) {
            internalSecurityClient.close();
        }
    }


    @Before
    public void setUp() {
        mongoCollection = mongoClient.getDatabase(CLUSTER_NAME).getCollection(PERSONAL_COLLECTION);
        mongoCollection.deleteMany(new Document());
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
            () -> internalSecurityClient.checkPersonalCertificate(certificate, "tt:read"))
            .isInstanceOf(InternalSecurityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmitted() throws Exception {
        // When / Then
        VitamThreadUtils.getVitamSession().setTenantId(0);
        assertThatThrownBy(
            () -> internalSecurityClient.checkPersonalCertificate(null, "tt:read"))
            .isInstanceOf(InternalSecurityException.class);
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
        internalSecurityClient.checkPersonalCertificate(certificate, "status:read");
    }

    //Admin Test resources
    @Test
    @RunWithCustomExecutor
    public void should_create_certificate_transmitted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        String url = "http://localhost:29003/v1/api/personalCertificate";
        // When
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        HttpEntity entity = new ByteArrayEntity(certificate);
        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        //Then
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(204);
        assertThat(Lists.newArrayList(mongoCollection.find()).size()).isEqualTo(1);

    }

    @Test
    @RunWithCustomExecutor
    public void should_delete_one_certificate() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        should_create_certificate_transmitted();
        String url = "http://localhost:29003/v1/api/personalCertificate";
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        assertThat(Lists.newArrayList(mongoCollection.find()).size()).isEqualTo(1);
        //WHEN
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost delete = new HttpPost(url) {
            @Override public String getMethod() {
                return "DELETE";
            }
        };

        HttpEntity entity = new ByteArrayEntity(certificate);
        delete.setEntity(entity);
        HttpResponse response = client.execute(delete);
        //THEN
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(204);
        assertThat(Lists.newArrayList(mongoCollection.find()).size()).isEqualTo(0);
    }
}
