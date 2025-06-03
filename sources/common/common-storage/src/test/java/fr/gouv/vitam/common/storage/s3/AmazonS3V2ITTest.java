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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FixedPatternFakeInputStream;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.ShellStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Integration tests using docker instances with storage s3 API : minio or
 * openio.
 */
// docker run -p 9999:9000 --name minio -e "MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T" -e "MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd" -e "MINIO_HTTP_TRACE=/tmp/minio.log" minio/minio server /data
// docker run -ti --tty -p 127.0.0.1:6007:6007 --name openio1 -e REGION="us-west-1" openio/sds:18.10
// docker run -p 9000:9000 --name minio_ssl -v "path/to/src/test/resources/s3/tls:/root/.minio" -e "MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T" -e "MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd" -e "MINIO_HTTP_TRACE=/tmp/minio.log" minio/minio server /data --certs-dir /root/.minio
public class AmazonS3V2ITTest {

    private static final String PROVIDER = "amazon-s3-v2";
    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2020-04-15T00-39-01Z";
    private static final String OPENIO_IMAGE = "openio/sds:18.10";
    private static final String MINIO_SECRET_KEY = "aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd";
    private static final String MINIO_ACCESS_KEY = "MKU4HW1K9HSST78MDY3T";
    private static final String OPENIO_ACCESS_KEY = "demo:demo";
    private static final String OPENIO_SECRET_KEY = "DEMO_PASS";
    private static final String MINIO_SSL_TRUSTSTORE = "src/test/resources/s3/tls/s3TrustStore.jks";
    private static final String MINIO_TRUSTSTORE_PASSWORD = "s3pass";
    private static final String BASE_LOCALHOST_HTTPS_URL = "https://127.0.0.1:";
    private static final String BASE_LOCALHOST_HTTP_URL = "http://127.0.0.1:";

    private static GenericContainer<?> minio;
    private static GenericContainer<?> minioSSL;
    private static GenericContainer<?> openio;

    private StorageConfiguration configurationMinio;
    private StorageConfiguration configurationOpenio;
    private StorageConfiguration configurationMinioSsl;

    private String containerName;
    private String objectName1;
    private String objectName2;

    private long largeObjectSize;
    private long largeObjectSize2;
    public String largeObjectDigest;

    private static final String FILE1_DIGEST =
        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418";

    @BeforeClass
    public static void setUpClass() {
        System.out.println("Starting containers");
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger(AmazonS3V2ITTest.class));

        openio = new GenericContainer<>(DockerImageName.parse(OPENIO_IMAGE))
            .withEnv("REGION", "us-west-1")
            .withExposedPorts(6007)
            .waitingFor(
                new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
                    .withStrategy(new DockerHealthcheckWaitStrategy())
                    .withStrategy(new HostPortWaitStrategy())
                    .withStrategy(
                        new ShellStrategy()
                            .withCommand("openio --oio-account health --oio-ns=OPENIO container create test")
                    )
            );

        openio.start();
        openio.followOutput(logConsumer);

        minio = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withCommand("server", "/data")
            .withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
            .withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
            .withExposedPorts(9000)
            .waitingFor(new HttpWaitStrategy().forPath("/minio/health/live"));
        minio.start();
        minio.followOutput(logConsumer);

        minioSSL = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withClasspathResourceMapping("s3/tls", "/root/.minio", BindMode.READ_ONLY)
            .withCommand("server", "/data", "--certs-dir", "/root/.minio")
            .withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
            .withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
            .withExposedPorts(9000)
            .waitingFor(new HttpWaitStrategy().allowInsecure().usingTls().forPath("/minio/health/live"));
        minioSSL.start();
        minioSSL.followOutput(logConsumer);

        System.out.println("Containers ready !");
    }

    @AfterClass
    public static void tearDownClass() {
        if (minio != null) {
            minio.stop();
        }
        if (minioSSL != null) {
            minioSSL.stop();
        }
        if (openio != null) {
            openio.stop();
        }
    }

    @Before
    public void setUp() throws Exception {
        configurationMinio = new StorageConfiguration();
        configurationMinio.setProvider(PROVIDER);
        configurationMinio.setS3RegionName("");
        configurationMinio.setS3Endpoint("http://localhost:" + minio.getMappedPort(9000));
        configurationMinio.setS3AccessKey(MINIO_ACCESS_KEY);
        configurationMinio.setS3SecretKey(MINIO_SECRET_KEY);
        configurationMinio.setS3PathStyleAccessEnabled(true);
        configurationMinio.setS3MaxUploadPartSizeMB(5);
        // Relatively small bulk size for test
        configurationMinio.setS3ListObjectBulkSize(100);

        configurationMinioSsl = new StorageConfiguration();
        configurationMinioSsl.setProvider(PROVIDER);
        configurationMinioSsl.setS3RegionName(Region.US_EAST_1.id());
        configurationMinioSsl.setS3Endpoint(BASE_LOCALHOST_HTTPS_URL + minioSSL.getMappedPort(9000));
        configurationMinioSsl.setS3AccessKey(MINIO_ACCESS_KEY);
        configurationMinioSsl.setS3SecretKey(MINIO_SECRET_KEY);
        configurationMinioSsl.setS3PathStyleAccessEnabled(true);
        configurationMinioSsl.setS3TrustStore(MINIO_SSL_TRUSTSTORE);
        configurationMinioSsl.setS3TrustStorePassword(MINIO_TRUSTSTORE_PASSWORD);
        configurationMinioSsl.setS3MaxUploadPartSizeMB(5);
        // Relatively small bulk size for test
        configurationMinioSsl.setS3ListObjectBulkSize(100);

        configurationOpenio = new StorageConfiguration();
        configurationOpenio.setProvider(PROVIDER);
        configurationOpenio.setS3RegionName(Region.US_WEST_1.id());
        configurationOpenio.setS3Endpoint(BASE_LOCALHOST_HTTP_URL + openio.getMappedPort(6007));
        configurationOpenio.setS3AccessKey(OPENIO_ACCESS_KEY);
        configurationOpenio.setS3SecretKey(OPENIO_SECRET_KEY);
        configurationOpenio.setS3ConnectionTimeout(200);
        configurationOpenio.setS3PathStyleAccessEnabled(true);
        configurationOpenio.setS3MaxUploadPartSizeMB(5);
        // Relatively small bulk size for test
        configurationOpenio.setS3ListObjectBulkSize(100);

        containerName = RandomStringUtils.randomNumeric(1) + "_" + RandomStringUtils.randomAlphabetic(10);
        objectName1 = GUIDFactory.newGUID().getId();
        objectName2 = GUIDFactory.newGUID().getId();
        largeObjectSize = 10 * 1024 * 1024 - 500 + RandomUtils.nextInt(1000);
        largeObjectSize2 = largeObjectSize + 1000;
        largeObjectDigest = computeDigest(new FixedPatternFakeInputStream(largeObjectSize));
    }

    @Test
    public void minio_ssl_minio_main_scenario() throws Exception {
        AmazonS3V2 amazonS3V1 = new AmazonS3V2(configurationMinioSsl);
        mainScenario(amazonS3V1);
    }

    @Test
    public void minio_main_scenario() throws Exception {
        AmazonS3V2 amazonS3V1 = new AmazonS3V2(configurationMinio);
        mainScenario(amazonS3V1);
    }

    @Test
    public void openio_main_scenario() throws Exception {
        AmazonS3V2 amazonS3V1 = new AmazonS3V2(configurationOpenio);
        mainScenario(amazonS3V1);
    }

    @Test
    public void minio_listing_scenario() throws Exception {
        AmazonS3V2 amazonS3V1 = new AmazonS3V2(configurationMinio);
        listingScenario(amazonS3V1);
    }

    @Test
    public void openio_listing_scenario() throws Exception {
        AmazonS3V2 amazonS3V1 = new AmazonS3V2(configurationOpenio);
        listingScenario(amazonS3V1);
    }

    private void mainScenario(AmazonS3V2 amazonS3V1) throws Exception {
        // check container that does not exist
        assertThat(amazonS3V1.isExistingContainer(containerName)).isFalse();

        // check object in a container that does not exist
        assertThat(amazonS3V1.isExistingObject(containerName, objectName1)).isFalse();

        // delete object in a container that does not exist
        assertThatThrownBy(
            () -> {
                amazonS3V1.deleteObject(containerName, objectName1);
            },
            "Delete object in a container that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to upload a file in a container that does not exist
        assertThatThrownBy(
            () -> writeSmallObject(amazonS3V1, objectName1, "file1.pdf", 6_906L),
            "Try to upload a file in a container that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to download a file from a container that does not exist
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObject(containerName, objectName1);
            },
            "Try to download a file from a container that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // compute digest of object from a container that does not exist
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObjectDigest(containerName, objectName1, DigestType.SHA512, false);
            },
            "Compute digest of object from a container that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // create a container
        assertThatCode(() -> {
            amazonS3V1.createContainer(containerName);
        }).doesNotThrowAnyException();

        // check container that exists
        assertThat(amazonS3V1.isExistingContainer(containerName)).isTrue();

        // re-create a container > idempotent
        assertThatCode(() -> {
            amazonS3V1.createContainer(containerName);
        }).doesNotThrowAnyException();

        // check container that exists (cache)
        assertThat(amazonS3V1.isExistingContainer(containerName)).isTrue();

        // check object that does not exist
        assertThat(amazonS3V1.isExistingObject(containerName, objectName1)).isFalse();

        // compute digest of object that does not exist
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObjectDigest(containerName, objectName1, DigestType.SHA512, false);
            },
            "Compute digest of object that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to download a file that does not exist
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObject(containerName, objectName1);
            },
            "Try to download a file that does not exist"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // upload a file
        assertThatCode(() -> writeSmallObject(amazonS3V1, objectName1, "file1.pdf", 6_906L)).doesNotThrowAnyException();

        // Check object content & metadata
        checkObjectContent(amazonS3V1, objectName1, FILE1_DIGEST);
        checkObjectMetadata(amazonS3V1, objectName1, false, 6_906L, FILE1_DIGEST);
        checkObjectMetadata(amazonS3V1, objectName1, true, 6_906L, FILE1_DIGEST);

        // check object that does exist
        assertThat(amazonS3V1.isExistingObject(containerName, objectName1)).isTrue();
        // compute digest of object that does exist
        String computedDigest = amazonS3V1.getObjectDigest(containerName, objectName1, DigestType.SHA512, false);
        assertThat(computedDigest).isEqualTo(FILE1_DIGEST);

        // try to upload a file on an existing file with an invalid size length (size <
        // filesize)
        assertThatThrownBy(
            () -> writeSmallObject(amazonS3V1, objectName1, "file1.pdf", 0L),
            "Try to upload a file on an existing file with an invalid size length (size < filesize)"
        ).isInstanceOf(ContentAddressableStorageServerException.class);

        // try to upload a file on an existing file with an invalid size length (size >
        // filesize)
        assertThatThrownBy(
            () -> writeSmallObject(amazonS3V1, objectName1, "file1.pdf", 1_000_000L),
            "Try to upload a file on an existing file with an invalid size length (size > filesize)"
        ).isInstanceOf(ContentAddressableStorageServerException.class);

        // Upload a large object
        assertThatCode(() -> writeLargeObject(amazonS3V1, objectName2, largeObjectSize)).doesNotThrowAnyException();

        // Check large object content & metadata
        checkObjectContent(amazonS3V1, objectName2, largeObjectDigest);
        checkObjectMetadata(amazonS3V1, objectName2, false, largeObjectSize, largeObjectDigest);
        checkObjectMetadata(amazonS3V1, objectName2, true, largeObjectSize, largeObjectDigest);

        // Override large object with a small one
        writeSmallObject(amazonS3V1, objectName2, "file1.pdf", 6_906L);

        // Check new object content & metadata
        checkObjectContent(amazonS3V1, objectName2, FILE1_DIGEST);
        checkObjectMetadata(amazonS3V1, objectName2, false, 6_906L, FILE1_DIGEST);
        checkObjectMetadata(amazonS3V1, objectName2, true, 6_906L, FILE1_DIGEST);

        // Override small object with a large one
        writeLargeObject(amazonS3V1, objectName2, largeObjectSize);

        // Check large object content & metadata
        checkObjectContent(amazonS3V1, objectName2, largeObjectDigest);
        checkObjectMetadata(amazonS3V1, objectName2, false, largeObjectSize, largeObjectDigest);
        checkObjectMetadata(amazonS3V1, objectName2, true, largeObjectSize, largeObjectDigest);

        // delete an existing file
        assertThatCode(() -> {
            amazonS3V1.deleteObject(containerName, objectName1);
        }).doesNotThrowAnyException();

        // delete an non existing file > idempotent
        assertThatCode(() -> {
            amazonS3V1.deleteObject(containerName, objectName1);
        }).doesNotThrowAnyException();

        // Delete large object
        assertThatCode(() -> amazonS3V1.deleteObject(containerName, objectName2)).doesNotThrowAnyException();

        // Ensure large object no more exists
        assertThat(amazonS3V1.isExistingObject(containerName, objectName2)).isFalse();

        amazonS3V1.close();
    }

    private void listingScenario(AmazonS3V2 amazonS3V1) throws Exception {
        // Given
        int bulkSize = amazonS3V1.getConfiguration().getS3ListObjectBulkSize();
        int nbSmallObjects = 2 * bulkSize + 10;
        int nbLargeObjects = 10;
        int overriddenSmallObjectIndex = 123;
        int overriddenLargeObjectIndex = 1;
        int deletedSmallObjectIndex = 136;
        int deletedLargeObjectIndex = 3;

        // create a container
        assertThatCode(() -> {
            amazonS3V1.createContainer(containerName);
        }).doesNotThrowAnyException();

        // upload multiple times the same file
        for (int i = 0; i < nbSmallObjects; i++) {
            writeSmallObject(amazonS3V1, objectName1 + i, "file1.pdf", 6_906L);
        }
        // upload a few large objects
        for (int i = 0; i < nbLargeObjects; i++) {
            writeLargeObject(amazonS3V1, objectName2 + i, largeObjectSize);
        }

        // override some objects
        writeSmallObject(amazonS3V1, objectName1 + overriddenSmallObjectIndex, "3500.txt", 3500L);
        writeLargeObject(amazonS3V1, objectName2 + overriddenLargeObjectIndex, largeObjectSize2);

        // delete some objects
        amazonS3V1.deleteObject(containerName, objectName1 + deletedSmallObjectIndex);
        amazonS3V1.deleteObject(containerName, objectName2 + deletedLargeObjectIndex);

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        // WHEN
        amazonS3V1.listContainer(containerName, objectListingListener);

        // THEN - Expect :
        //   -  (nbSmallObjects - 2)x small objects with size 6906L
        //   -                     1x small object with size 3500L
        //   -                     1x small object deleted
        //   - (largeObjectName - 2)x large objects with size 12_000_000L
        //   -                     1x large object with size 12_000_001L
        //   -                     1x large object deleted
        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(nbSmallObjects + nbLargeObjects - 2)).handleObjectEntry(
            objectEntryArgumentCaptor.capture()
        );

        assertThat(
            objectEntryArgumentCaptor.getAllValues().stream().filter(entry -> 6906L == entry.getSize()).count()
        ).isEqualTo(nbSmallObjects - 2);

        assertThat(
            objectEntryArgumentCaptor.getAllValues().stream().filter(entry -> 3500L == entry.getSize()).count()
        ).isEqualTo(1);

        // One large object overridden, one deleted
        assertThat(
            objectEntryArgumentCaptor
                .getAllValues()
                .stream()
                .filter(entry -> largeObjectSize == entry.getSize())
                .count()
        ).isEqualTo(nbLargeObjects - 2);

        assertThat(
            objectEntryArgumentCaptor
                .getAllValues()
                .stream()
                .filter(entry -> largeObjectSize2 == entry.getSize())
                .count()
        ).isEqualTo(1);

        Set<String> capturedFileNames = objectEntryArgumentCaptor
            .getAllValues()
            .stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedSmallFileNames = IntStream.range(0, nbSmallObjects)
            .filter(i -> i != deletedSmallObjectIndex)
            .mapToObj(i -> objectName1 + i)
            .collect(Collectors.toSet());
        Set<String> expectedLargeFileNames = IntStream.range(0, nbLargeObjects)
            .filter(i -> i != deletedLargeObjectIndex)
            .mapToObj(i -> objectName2 + i)
            .collect(Collectors.toSet());
        assertThat(capturedFileNames).isEqualTo(SetUtils.union(expectedSmallFileNames, expectedLargeFileNames));
    }

    private void writeSmallObject(AmazonS3V2 amazonS3V1, String objectName, String resourceFile, long size)
        throws IOException, ContentAddressableStorageException {
        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(resourceFile)) {
            amazonS3V1.putObject(containerName, objectName, inputStream, DigestType.SHA512, size);
        }
    }

    private void writeLargeObject(AmazonS3V2 amazonS3V1, String objectName, long size)
        throws IOException, ContentAddressableStorageException {
        try (InputStream inputStream = new FixedPatternFakeInputStream(size)) {
            amazonS3V1.putObject(containerName, objectName, inputStream, DigestType.SHA512, size);
        }
    }

    private void checkObjectContent(AmazonS3V2 amazonS3V1, String objectName, String expectedDigest)
        throws ContentAddressableStorageException, IOException {
        ObjectContent response = amazonS3V1.getObject(containerName, objectName);
        try (InputStream is = response.getInputStream()) {
            assertThat(computeDigest(is)).isEqualTo(expectedDigest);
        }
    }

    private static String computeDigest(InputStream is) throws IOException {
        return new Digest(DigestType.SHA512).update(is).digestHex();
    }

    private void checkObjectMetadata(
        AmazonS3V2 amazonS3V1,
        String object,
        boolean noCache,
        long objectSize,
        String expectedDigest
    ) throws ContentAddressableStorageException {
        MetadatasObject largeObjectMetadata = amazonS3V1.getObjectMetadata(containerName, object, noCache);
        assertThat(largeObjectMetadata.getFileSize()).isEqualTo(objectSize);
        assertThat(largeObjectMetadata.getDigest()).isEqualTo(expectedDigest);
        assertThat(largeObjectMetadata.getObjectName()).isEqualTo(object);
        assertThat(largeObjectMetadata.getType()).isEqualTo(containerName.split("_")[1]);
    }
}
