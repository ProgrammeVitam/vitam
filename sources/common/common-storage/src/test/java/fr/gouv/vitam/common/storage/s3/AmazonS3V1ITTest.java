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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
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
// docker run -p 9999:9000 --name minio1 -e "MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T" -e "MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd" -e "MINIO_HTTP_TRACE=/tmp/minio.log" minio/minio server /data
// docker run -ti --tty -p 127.0.0.1:6007:6007 --name openio1 -e REGION="us-west-1" openio/sds:18.10
// To add ssl to minio : in docker container, add s3/tls/private.key et s3/tls/public.crt  in folder /root/.minio/certs, "docker restart minio1" and use -p 9000:9000
public class AmazonS3V1ITTest {

    private static final String PROVIDER = "amazon-s3-v1";
    private static final String S3_CONF_FILE = "s3/s3-conf.json";
    private StorageConfiguration configurationMinio;
    private StorageConfiguration configurationOpenio;
    private StorageConfiguration configurationMinioSsl;

    private String containerName;
    private String objectName;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        JsonNode jsonNode = JsonHandler.getFromFile(PropertiesUtils.findFile(S3_CONF_FILE));
        configurationMinio = new StorageConfiguration();
        configurationMinio.setProvider(PROVIDER);
        configurationMinio.setS3RegionName("");
        configurationMinio.setS3Endpoint(jsonNode.findValue("S3_MINIO_ENDPOINT").textValue());
        configurationMinio.setS3AccessKey(jsonNode.findValue("S3_MINIO_ACCESSKEY").textValue());
        configurationMinio.setS3SecretKey(jsonNode.findValue("S3_MINIO_SECRETKEY").textValue());
        configurationMinio.setS3PathStyleAccessEnabled(true);
        configurationMinio.setS3ConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
        configurationMinio.setS3SocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
        configurationMinio.setS3MaxConnections(ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        configurationMinio.setS3RequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT);
        configurationMinio.setS3ClientExecutionTimeout(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT);

        configurationMinioSsl = new StorageConfiguration();
        configurationMinioSsl.setProvider(PROVIDER);
        configurationMinioSsl.setS3RegionName("");
        configurationMinioSsl.setS3Endpoint(jsonNode.findValue("S3_MINIO_ENDPOINT_SSL").textValue());
        configurationMinioSsl.setS3AccessKey(jsonNode.findValue("S3_MINIO_ACCESSKEY").textValue());
        configurationMinioSsl.setS3SecretKey(jsonNode.findValue("S3_MINIO_SECRETKEY").textValue());
        configurationMinioSsl.setS3PathStyleAccessEnabled(true);
        configurationMinioSsl.setS3TrustStore(jsonNode.findValue("S3_MINIO_TRUSTSTORE").textValue());
        configurationMinioSsl.setS3TrustStorePassword(jsonNode.findValue("S3_MINIO_TRUSTSTORE_PASS").textValue());
        configurationMinioSsl.setS3ConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
        configurationMinioSsl.setS3SocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
        configurationMinioSsl.setS3MaxConnections(ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        configurationMinioSsl.setS3RequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT);
        configurationMinioSsl.setS3ClientExecutionTimeout(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT);

        configurationOpenio = new StorageConfiguration();
        configurationOpenio.setProvider(PROVIDER);
        configurationOpenio.setS3RegionName(Regions.US_WEST_1.getName());
        configurationOpenio.setS3Endpoint(jsonNode.findValue("S3_OPENIO_ENDPOINT").textValue());
        configurationOpenio.setS3AccessKey(jsonNode.findValue("S3_OPENIO_ACCESSKEY").textValue());
        configurationOpenio.setS3SecretKey(jsonNode.findValue("S3_OPENIO_SECRETKEY").textValue());
        configurationOpenio.setS3ConnectionTimeout(200);
        configurationOpenio.setS3PathStyleAccessEnabled(true);
        configurationOpenio.setS3ConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
        configurationOpenio.setS3SocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
        configurationOpenio.setS3MaxConnections(ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        configurationOpenio.setS3RequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT);
        configurationOpenio.setS3ClientExecutionTimeout(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT);

        containerName = RandomStringUtils.randomNumeric(1) + "_" + RandomStringUtils.randomAlphabetic(10);
        objectName = GUIDFactory.newGUID().getId();
    }

    @Ignore("There is a problem with minio ssl when sending an object with a wrong size")
    @Test
    public void minio_ssl_minio_main_scenario() throws Exception {
        AmazonS3V1 amazonS3V1 = new AmazonS3V1(configurationMinioSsl);
        mainScenario(amazonS3V1);
    }

    @Test
    public void minio_main_scenario() throws Exception {
        AmazonS3V1 amazonS3V1 = new AmazonS3V1(configurationMinio);
        mainScenario(amazonS3V1);
    }

    @Test
    public void openio_main_scenario() throws Exception {
        AmazonS3V1 amazonS3V1 = new AmazonS3V1(configurationOpenio);
        mainScenario(amazonS3V1);
    }

    @Test
    public void minio_listing_scenario() throws Exception {
        AmazonS3V1 amazonS3V1 = new AmazonS3V1(configurationMinio);
        listingScenario(amazonS3V1);
    }

    @Test
    public void openio_listing_scenario() throws Exception {
        AmazonS3V1 amazonS3V1 = new AmazonS3V1(configurationOpenio);
        listingScenario(amazonS3V1);
    }

    private void mainScenario(AmazonS3V1 amazonS3V1) throws Exception {
        // check container that does not exists
        assertThat(amazonS3V1.isExistingContainer(containerName)).isFalse();

        // check object in a container that does not exists
        assertThat(amazonS3V1.isExistingObject(containerName, objectName)).isFalse();

        // delete object in a container that does not exists
        assertThatThrownBy(
            () -> {
                amazonS3V1.deleteObject(containerName, objectName);
            },
            "Delete object in a container that does not exists"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to upload a file in a container that does not exists
        assertThatThrownBy(
            () -> {
                InputStream stream = getInputStream("file1.pdf");
                amazonS3V1.putObject(containerName, objectName, stream, DigestType.SHA512, 6_906L);
            },
            "Try to upload a file in a container that does not exists"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to download a file from a container that does not exists
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObject(containerName, objectName);
            },
            "Try to download a file from a container that does not exists"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // compute digest of object from a container that does not exists
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObjectDigest(containerName, objectName, DigestType.SHA512, false);
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

        // check object that does not exists
        assertThat(amazonS3V1.isExistingObject(containerName, objectName)).isFalse();

        // compute digest of object that does not exists
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObjectDigest(containerName, objectName, DigestType.SHA512, false);
            },
            "Compute digest of object that does not exists"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // try to download a file that does not exists
        assertThatThrownBy(
            () -> {
                amazonS3V1.getObject(containerName, objectName);
            },
            "Try to download a file that does not exists"
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // upload a file
        InputStream file1Stream = getInputStream("file1.pdf");
        amazonS3V1.putObject(containerName, objectName, file1Stream, DigestType.SHA512, 6_906L);
        file1Stream.close();

        // download an existing file
        ObjectContent response = amazonS3V1.getObject(containerName, objectName);
        try (InputStream is = response.getInputStream()) {
            File fileDownloaded = tempFolder.newFile();
            FileOutputStream fileOutputStream = new FileOutputStream(fileDownloaded);
            IOUtils.copy(is, fileOutputStream);
            File resourceFile = PropertiesUtils.getResourceFile("file1.pdf");
            assertThat(fileDownloaded.length()).isEqualTo(resourceFile.length());
        }

        // get an existing file's metadata
        MetadatasObject metadatasObject = amazonS3V1.getObjectMetadata(containerName, objectName, false);
        assertThat(metadatasObject.getFileSize()).isEqualTo(6_906L);
        assertThat(metadatasObject.getDigest()).isEqualTo(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418"
        );
        assertThat(metadatasObject.getObjectName()).isEqualTo(objectName);
        assertThat(metadatasObject.getType()).isEqualTo(containerName.split("_")[1]);

        // check object that does exists
        assertThat(amazonS3V1.isExistingObject(containerName, objectName)).isTrue();

        // compute digest of object that does exists
        String computedDigest = amazonS3V1.getObjectDigest(containerName, objectName, DigestType.SHA512, false);
        assertThat(computedDigest).isEqualTo(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418"
        );

        // try to upload a file on an existing file with an invalid size length (size <
        // filesize)
        assertThatThrownBy(
            () -> {
                InputStream file2Stream = getInputStream("file2.pdf");
                amazonS3V1.putObject(containerName, objectName, file2Stream, DigestType.SHA512, 0L);
            },
            "Try to upload a file on an existing file with an invalid size length (size < filesize)"
        ).isInstanceOf(ContentAddressableStorageServerException.class);

        // try to upload a file on an existing file with an invalid size length (size >
        // filesize)
        assertThatThrownBy(
            () -> {
                InputStream file2Stream = getInputStream("file2.pdf");
                amazonS3V1.putObject(containerName, objectName, file2Stream, DigestType.SHA512, 1_000_000L);
            },
            "Try to upload a file on an existing file with an invalid size length (size > filesize)"
        ).isInstanceOf(ContentAddressableStorageServerException.class);

        // delete an existing file
        assertThatCode(() -> {
            amazonS3V1.deleteObject(containerName, objectName);
        }).doesNotThrowAnyException();

        // delete an non existing file > idempotent
        assertThatCode(() -> {
            amazonS3V1.deleteObject(containerName, objectName);
        }).doesNotThrowAnyException();

        amazonS3V1.close();
    }

    private void listingScenario(AmazonS3V1 amazonS3V1) throws Exception {
        int nbIter = 2;
        // create a container
        assertThatCode(() -> {
            amazonS3V1.createContainer(containerName);
        }).doesNotThrowAnyException();

        // upload multiple times the same file
        for (int i = 0; i < (nbIter * 100 + 50); i++) {
            InputStream file1Stream = getInputStream("file1.pdf");
            amazonS3V1.putObject(containerName, objectName + i, file1Stream, DigestType.SHA512, 6_906L);
            file1Stream.close();
        }

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        amazonS3V1.listContainer(containerName, objectListingListener);

        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(nbIter * 100 + 50)).handleObjectEntry(objectEntryArgumentCaptor.capture());

        objectEntryArgumentCaptor
            .getAllValues()
            .forEach(capturedObjectEntry -> assertThat(capturedObjectEntry.getSize()).isEqualTo(6906L));

        Set<String> capturedFileNames = objectEntryArgumentCaptor
            .getAllValues()
            .stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedFileNames = IntStream.range(0, nbIter * 100 + 50)
            .mapToObj(i -> objectName + i)
            .collect(Collectors.toSet());
        assertThat(capturedFileNames).isEqualTo(expectedFileNames);
    }

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }
}
