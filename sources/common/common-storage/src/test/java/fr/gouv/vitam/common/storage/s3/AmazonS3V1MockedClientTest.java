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
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.FixedPatternFakeInputStream;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AmazonS3V1MockedClientTest {

    private static final String CONTAINER_0 = "0_Unit";
    private static final String BUCKET_0 = "0.unit";
    private static final String OBJECT_ID_0 = "object0id";

    private static AmazonS3V1 amazonS3V1;
    private static AmazonS3 amazonS3Client;

    @BeforeClass
    public static void setUpClass() {
        amazonS3Client = mock(AmazonS3.class);
        StorageConfiguration configuration = new StorageConfiguration();
        configuration.setS3MaxUploadPartSizeMB(5);
        amazonS3V1 = new AmazonS3V1(configuration, amazonS3Client);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.reset(amazonS3Client);
    }

    @Test
    public void generate_bucket_name_should_return_valid_name_when_uppercase() {
        String containerName = "UNIT";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_prefix_not_alphanumeric() {
        String containerName = "#int-0_UNIT";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("int.0.unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_last_char_not_alphanumeric() {
        String containerName = "UNIT)";
        String buckName = amazonS3V1.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void exists_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.doesBucketExistV2(BUCKET_0)).thenThrow(new SdkBaseException("Client error"));
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existence of container");
    }

    @Test
    public void exists_object_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.doesObjectExist(BUCKET_0, OBJECT_ID_0)).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.isExistingObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existence of object");
    }

    @Test
    public void create_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.createBucket(BUCKET_0)).thenThrow(new SdkBaseException("Client error"));
        assertThatCode(() -> {
            amazonS3V1.createContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to create container");
    }

    @Test
    public void delete_object_should_throw_exception_when_client_throws_exception() {
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
    public void get_object_should_throw_exception_when_client_throws_exception() {
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
    public void get_object_digest_should_throw_exception_when_client_throws_exception() {
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
    public void get_object_metadata_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.getObjectMetadata(CONTAINER_0, OBJECT_ID_0, false);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to get metadata of object");
    }

    @Test
    public void upload_object_should_throw_exception_when_client_putobject_throws_exception() {
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
    public void upload_object_should_throw_exception_when_client_copyobject_throws_exception() {
        InputStream stream = new FakeInputStream(0);
        Mockito.when(
            amazonS3Client.putObject(eq(BUCKET_0), eq(OBJECT_ID_0), any(InputStream.class), any(ObjectMetadata.class))
        ).thenReturn(new PutObjectResult());

        S3ObjectInputStream inputStream = new S3ObjectInputStream(new FakeInputStream(0), mock(HttpRequestBase.class));
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(0L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);
        Mockito.when(amazonS3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );

        assertThatCode(() -> {
            amazonS3V1.putObject(CONTAINER_0, OBJECT_ID_0, stream, DigestType.SHA512, 0L);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to update metadata of object");
    }

    @Test
    public void list_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(
            new SdkBaseException("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V1.listContainer(CONTAINER_0, mock(ObjectListingListener.class));
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to list objects");
    }

    @Test
    public void put_large_object_should_complete_successfully_when_client_ok() throws Exception {
        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("uploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
        Mockito.when(amazonS3Client.uploadPart(any())).thenAnswer(args -> {
            UploadPartResult uploadPartResult = new UploadPartResult();
            UploadPartRequest uploadPartRequest = args.getArgument(0);
            uploadPartResult.setETag("etag" + uploadPartRequest.getPartNumber());
            return uploadPartResult;
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any())).thenReturn(new CompleteMultipartUploadResult());

        // When
        amazonS3V1.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L);

        // Then
        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());

        ArgumentCaptor<UploadPartRequest> uploadPartRequestArgumentCaptor = ArgumentCaptor.forClass(
            UploadPartRequest.class
        );
        Mockito.verify(amazonS3Client, times(2)).uploadPart(uploadPartRequestArgumentCaptor.capture());

        UploadPartRequest uploadPartRequest1 = uploadPartRequestArgumentCaptor.getAllValues().get(0);
        assertThat(uploadPartRequest1.getUploadId()).isEqualTo("uploadId");
        assertThat(uploadPartRequest1.getPartNumber()).isEqualTo(1);
        assertThat(uploadPartRequest1.getPartSize()).isEqualTo(5_242_880L);
        assertThat(uploadPartRequest1.getBucketName()).isEqualTo("0.unit");

        UploadPartRequest uploadPartRequest2 = uploadPartRequestArgumentCaptor.getAllValues().get(1);
        assertThat(uploadPartRequest2.getUploadId()).isEqualTo("uploadId");
        assertThat(uploadPartRequest2.getPartNumber()).isEqualTo(2);
        assertThat(uploadPartRequest2.getPartSize()).isEqualTo(757_120L);
        assertThat(uploadPartRequest2.getBucketName()).isEqualTo("0.unit");

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        Mockito.verify(amazonS3Client).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
        assertThat(completeMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("uploadId");
        assertThat(
            completeMultipartUploadRequestArgumentCaptor.getValue().getPartETags().stream().map(PartETag::getETag)
        ).containsExactly("etag1", "etag2");

        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void put_large_object_should_throw_exception_when_client_throws_exception_during_multipart_initialization() {
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenThrow(new SdkBaseException("Client error"));
        assertThatThrownBy(
            () ->
                amazonS3V1.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");
        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void put_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_upload() {
        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("uploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
        Mockito.when(amazonS3Client.uploadPart(any())).thenThrow(new SdkBaseException("Client error"));
        assertThatThrownBy(
            () ->
                amazonS3V1.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");

        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        Mockito.verify(amazonS3Client).uploadPart(any());

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("uploadId");
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void put_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_completion() {
        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("uploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
        Mockito.when(amazonS3Client.uploadPart(any())).thenAnswer(args -> {
            UploadPartResult uploadPartResult = new UploadPartResult();
            UploadPartRequest uploadPartRequest = args.getArgument(0);
            uploadPartResult.setETag("etag" + uploadPartRequest.getPartNumber());
            return uploadPartResult;
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any())).thenThrow(new SdkBaseException("Client error"));

        // When / Then
        assertThatThrownBy(
            () ->
                amazonS3V1.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");

        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        Mockito.verify(amazonS3Client, times(2)).uploadPart(any());
        Mockito.verify(amazonS3Client).completeMultipartUpload(any());

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("uploadId");
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_small_object_should_complete_successfully_when_client_ok() throws Exception {
        // Given
        S3ObjectInputStream inputStream = new S3ObjectInputStream(
            PropertiesUtils.getResourceAsStream("file1.pdf"),
            mock(HttpRequestBase.class)
        );
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(6906L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);

        Mockito.when(amazonS3Client.copyObject(any())).thenReturn(new CopyObjectResult());

        // When
        amazonS3V1.checkObjectDigestAndStoreDigest(
            CONTAINER_0,
            OBJECT_ID_0,
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
            DigestType.SHA512,
            6906L
        );

        // Then
        Mockito.verify(amazonS3Client).getObject(any());

        ArgumentCaptor<CopyObjectRequest> copyObjectRequestArgumentCaptor = ArgumentCaptor.forClass(
            CopyObjectRequest.class
        );
        Mockito.verify(amazonS3Client).copyObject(copyObjectRequestArgumentCaptor.capture());
        assertThat(copyObjectRequestArgumentCaptor.getValue().getSourceBucketName()).isEqualTo("0.unit");
        assertThat(copyObjectRequestArgumentCaptor.getValue().getSourceKey()).isEqualTo(OBJECT_ID_0);
        assertThat(copyObjectRequestArgumentCaptor.getValue().getDestinationBucketName()).isEqualTo("0.unit");
        assertThat(copyObjectRequestArgumentCaptor.getValue().getDestinationKey()).isEqualTo(OBJECT_ID_0);
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_large_object_should_complete_successfully_when_client_ok() throws Exception {
        // Given
        S3ObjectInputStream inputStream = new S3ObjectInputStream(
            new FixedPatternFakeInputStream(6_000_000L),
            mock(HttpRequestBase.class)
        );
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(6_000_000L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);

        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("copyUploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
        Mockito.when(amazonS3Client.copyPart(any())).thenAnswer(args -> {
            CopyPartResult copyPartResult = new CopyPartResult();
            CopyPartRequest copyPartRequest = args.getArgument(0);
            copyPartResult.setETag("etag" + copyPartRequest.getPartNumber());
            return copyPartResult;
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any())).thenReturn(new CompleteMultipartUploadResult());

        // When
        amazonS3V1.checkObjectDigestAndStoreDigest(
            CONTAINER_0,
            OBJECT_ID_0,
            "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
            DigestType.SHA512,
            6_000_000L
        );

        // Then
        Mockito.verify(amazonS3Client).getObject(any());

        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());

        ArgumentCaptor<CopyPartRequest> copyPartRequestArgumentCaptor = ArgumentCaptor.forClass(CopyPartRequest.class);
        Mockito.verify(amazonS3Client, times(2)).copyPart(copyPartRequestArgumentCaptor.capture());

        CopyPartRequest copyPartRequest1 = copyPartRequestArgumentCaptor.getAllValues().get(0);
        assertThat(copyPartRequest1.getUploadId()).isEqualTo("copyUploadId");
        assertThat(copyPartRequest1.getPartNumber()).isEqualTo(1);
        assertThat(copyPartRequest1.getFirstByte()).isEqualTo(0L);
        assertThat(copyPartRequest1.getLastByte()).isEqualTo(5_242_879L);
        assertThat(copyPartRequest1.getSourceBucketName()).isEqualTo("0.unit");
        assertThat(copyPartRequest1.getSourceKey()).isEqualTo(OBJECT_ID_0);

        CopyPartRequest copyPartRequest2 = copyPartRequestArgumentCaptor.getAllValues().get(1);
        assertThat(copyPartRequest2.getUploadId()).isEqualTo("copyUploadId");
        assertThat(copyPartRequest2.getPartNumber()).isEqualTo(2);
        assertThat(copyPartRequest2.getFirstByte()).isEqualTo(5_242_880L);
        assertThat(copyPartRequest2.getLastByte()).isEqualTo(5_999_999L);
        assertThat(copyPartRequest2.getSourceBucketName()).isEqualTo("0.unit");
        assertThat(copyPartRequest2.getSourceKey()).isEqualTo(OBJECT_ID_0);

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        Mockito.verify(amazonS3Client).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
        assertThat(completeMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("copyUploadId");
        assertThat(
            completeMultipartUploadRequestArgumentCaptor.getValue().getPartETags().stream().map(PartETag::getETag)
        ).containsExactly("etag1", "etag2");

        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_when_client_throws_exception_during_get_object() {
        // Given
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenThrow(
            new SdkBaseException("Client error")
        );

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V1.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to download object");

        Mockito.verify(amazonS3Client).getObject(any());
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_when_client_throws_exception_during_multipart_initialization()
        throws Exception {
        // Given
        S3ObjectInputStream inputStream = new S3ObjectInputStream(
            new FixedPatternFakeInputStream(6_000_000L),
            mock(HttpRequestBase.class)
        );
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(6_000_000L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);

        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenThrow(new SdkBaseException("Client error"));

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V1.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any());
        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_copy()
        throws Exception {
        // Given
        S3ObjectInputStream inputStream = new S3ObjectInputStream(
            new FixedPatternFakeInputStream(6_000_000L),
            mock(HttpRequestBase.class)
        );
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(6_000_000L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);

        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("copyUploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);

        Mockito.when(amazonS3Client.copyPart(any())).thenThrow(new SdkBaseException("Client error"));

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V1.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any());
        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        Mockito.verify(amazonS3Client).copyPart(any());

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("copyUploadId");
        verifyNoMoreInteractions(amazonS3Client);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_completion()
        throws Exception {
        // Given
        S3ObjectInputStream inputStream = new S3ObjectInputStream(
            new FixedPatternFakeInputStream(6_000_000L),
            mock(HttpRequestBase.class)
        );
        S3Object object = mock(S3Object.class);
        ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
        Mockito.when(object.getObjectMetadata()).thenReturn(objectMetadata);
        Mockito.when(object.getObjectContent()).thenReturn(inputStream);
        Mockito.when(objectMetadata.getContentLength()).thenReturn(6_000_000L);
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(object);

        InitiateMultipartUploadResult initiateMultipartUploadResult = new InitiateMultipartUploadResult();
        initiateMultipartUploadResult.setUploadId("copyUploadId");
        Mockito.when(amazonS3Client.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);

        Mockito.when(amazonS3Client.copyPart(any())).thenAnswer(args -> {
            CopyPartResult copyPartResult = new CopyPartResult();
            CopyPartRequest copyPartRequest = args.getArgument(0);
            copyPartResult.setETag("etag" + copyPartRequest.getPartNumber());
            return copyPartResult;
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any())).thenThrow(new SdkBaseException("Client error"));

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V1.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any());
        Mockito.verify(amazonS3Client).initiateMultipartUpload(any());
        Mockito.verify(amazonS3Client, times(2)).copyPart(any());
        Mockito.verify(amazonS3Client).completeMultipartUpload(any());

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().getUploadId()).isEqualTo("copyUploadId");
        verifyNoMoreInteractions(amazonS3Client);
    }
}
