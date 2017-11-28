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

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.rest.IdentityMain;
import fr.gouv.vitam.security.internal.rest.SimpleMongoDBAccess;
import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.jhades.JHades;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;

import static com.google.common.io.ByteStreams.toByteArray;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.security.internal.rest.repository.IdentityRepository.CERTIFICATE_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecurityInternalIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecurityInternalIT.class);
    private static int logbookPort;
    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MongoRule mongoRule = new MongoRule(getMongoClientOptions(), CLUSTER_NAME, CERTIFICATE_COLLECTION);

    private static final Integer TENANT_ID = 1;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    final static String CLUSTER_NAME = "vitam-cluster";
    static MongodProcess mongod;
    static MongoClient client;
    static int mongoPort;
    static MongodExecutable mongodExecutable;

    private static LogbookMain logbookMain;
    private static IdentityMain identityMain;
    private static final String LOGBOOK_CONF = "security-internal/logbook.conf";
    private static final String IDENTITY_CONF = "security-internal/security-internal.conf";
    private static String TMP_FOLDER;
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static InternalSecurityClient internalSecurityClient;



    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();
        try {
            TMP_FOLDER = temporaryFolder.newFolder().getAbsolutePath();
        } catch (IOException e) {
            TMP_FOLDER = "/vitam/temp";
        }

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongoPort = 12346;
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();


        identityMain = new IdentityMain(IDENTITY_CONF);
        identityMain.start();
        internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
    }



    @Test(expected = InternalSecurityException.class)
    @RunWithCustomExecutor
    public void should_fail_when_no_valid_certificate_transmited()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException,
        InternalSecurityException, VitamClientInternalException {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        internalSecurityClient.checkPersonalCertificate(certificate, "tt:read");
        // When /then

    }

    @Test(expected = InternalSecurityException.class)
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmitted()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException,
        InternalSecurityException, VitamClientInternalException {
        // Given

        internalSecurityClient.checkPersonalCertificate(null, "tt:read");
        // When /then

    }

    @Test
    @RunWithCustomExecutor
    public void should_check_valid_certificate_transmitted()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        String url = "http://localhost:29003/v1/api/personalCertificate";

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        HttpEntity entity = new ByteArrayEntity(certificate);
        post.setEntity(entity);
        HttpResponse response = client.execute(post);

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        internalSecurityClient.checkPersonalCertificate(certificate, "status:read");

    }

    @Test
    public void should_create_certificate_transmitted()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        ArgumentCaptor<PersonalCertificateModel> argumentCaptor = forClass(PersonalCertificateModel.class);

        // When
        ///then

        // assertThat(argumentCaptor.getValue().getCertificateHash())
        //  .isEqualTo("2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7");
    }
}
