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

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

public class AmazonS3V1MockedClientTest {

    private static final String CONTAINER_0 = "0_Unit";
    private static final String BUCKET_0 = "0.unit";
    private static final String OBJECT_ID_0 = "object0id";

    private static AmazonS3V1 amazonS3V1;
    private static AmazonS3 amazonS3Client;

    @BeforeClass
    public static void setUpClass() throws Exception {
        amazonS3Client = mock(AmazonS3.class);
        amazonS3V1 = new AmazonS3V1(new StorageConfiguration(), amazonS3Client);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.reset(amazonS3Client);
    }

    @Test
    public void generate_bucket_name_should_return_valid_name_when_uppercase() throws Exception {
        String containerName = "UNIT";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_prefix_not_alphanumeric() throws Exception {
        String containerName = "#int-0_UNIT";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("int.0.unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_last_char_not_alphanumeric()
        throws Exception {
        String containerName = "UNIT)";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void exists_container_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.doesBucketExistV2(BUCKET_0)).thenThrow(new SdkBaseException("Client error"));
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existance of container");
    }

    @Test
    public void exists_object_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.doesObjectExist(BUCKET_0, OBJECT_ID_0)).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existance of object");
    }

    @Test
    public void create_container_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.createBucket(BUCKET_0)).thenThrow(new SdkBaseException("Client error"));
        assertThatCode(() -> {
            amazonS3V1.createContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to create container");
    }

    @Test
    public void delete_object_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.doThrow(new SdkBaseException("Client error"))
            .when(amazonS3Client)
            .deleteObject(any(DeleteObjectRequest.class));
        assertThatCode(() -> {
            amazonS3V1.deleteObject(BUCKET_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to delete object object0id");
    }

    @Test
    public void get_object_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatCode(() -> {
            amazonS3V1.getObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to download object");
    }

    @Test
    public void get_object_digest_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectDigest(CONTAINER_0, OBJECT_ID_0, DigestType.SHA512, false);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to compute digest of object");
    }

    @Test
    public void get_object_metadatas_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectMetadata(CONTAINER_0, OBJECT_ID_0, false);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to get metadatas of object");
    }

    @Test
    public void upload_object_should_throw_exception_when_client_putobject_throws_exception() throws Exception {
        FakeInputStream fakeInputStream = new FakeInputStream(3500L);
        Mockito.when(
            amazonS3Client.putObject(eq(BUCKET_0), eq(OBJECT_ID_0), any(InputStream.class), any(ObjectMetadata.class))
        ).thenThrow(new SdkBaseException("Client error"));

        assertThatCode(() -> {
            amazonS3V1.putObject(CONTAINER_0, OBJECT_ID_0, fakeInputStream, DigestType.SHA512, 3_500L);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to upload object");
    }

    @Test
    public void upload_object_should_throw_exception_when_client_copyobject_throws_exception() throws Exception {
        InputStream stream = new FakeInputStream(0);
        Mockito.when(
            amazonS3Client.putObject(eq(BUCKET_0), eq(OBJECT_ID_0), any(InputStream.class), any(ObjectMetadata.class))
        ).thenReturn(new PutObjectResult());

        FakeInputStream delegateInputStream = new FakeInputStream(0);
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        S3ObjectInputStream objectContent = mock(S3ObjectInputStream.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(objectContent);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(0L);
        Mockito.when(objectContent.getDelegateStream()).thenReturn(delegateInputStream);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);
        Mockito.when(amazonS3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );

        assertThatCode(() -> {
            amazonS3V1.putObject(CONTAINER_0, OBJECT_ID_0, stream, DigestType.SHA512, 0L);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to update metadatas of object");
    }

    @Test
    public void list_container_should_throw_exception_when_client_throws_exception() throws Exception {
        Mockito.when(amazonS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.listContainer(CONTAINER_0, mock(ObjectListingListener.class));
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to list objects");
    }
}
