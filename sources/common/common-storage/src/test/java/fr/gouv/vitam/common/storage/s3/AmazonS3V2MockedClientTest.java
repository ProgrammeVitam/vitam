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
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.FixedPatternFakeInputStream;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AmazonS3V2MockedClientTest {

    private static final String CONTAINER_0 = "0_Unit";
    private static final String BUCKET_0 = "0.unit";
    private static final String OBJECT_ID_0 = "object0id";

    private AmazonS3V2 amazonS3V2;
    private S3Client amazonS3Client;
    private S3Client amazonS3ClientWithoutRetries;

    @Before
    public void setUp() {
        amazonS3Client = mock(S3Client.class);
        amazonS3ClientWithoutRetries = mock(S3Client.class);
        StorageConfiguration configuration = new StorageConfiguration();
        configuration.setS3MaxUploadPartSizeMB(5);
        amazonS3V2 = new AmazonS3V2(
            configuration,
            mock(SdkHttpClient.class),
            amazonS3Client,
            amazonS3ClientWithoutRetries
        );
    }

    @Test
    public void generate_bucket_name_should_return_valid_name_when_uppercase() {
        String containerName = "UNIT";
        String buckName = amazonS3V2.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_prefix_not_alphanumeric() {
        String containerName = "#int-0_UNIT";
        String buckName = amazonS3V2.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("int.0.unit");
    }

    @Test
    public void generate_bucket_name_should_return_return_valid_name_when_last_char_not_alphanumeric() {
        String containerName = "UNIT)";
        String buckName = amazonS3V2.generateBucketName(containerName);
        assertThat(buckName).isEqualTo("unit");
    }

    @Test
    public void exists_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(
            amazonS3Client.headBucket(argThat((HeadBucketRequest r) -> r.bucket().equals(BUCKET_0)))
        ).thenThrow(ApiCallTimeoutException.create("Client error"));
        assertThatThrownBy(() -> {
            amazonS3V2.isExistingContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existence of container");
    }

    @Test
    public void exists_object_should_throw_exception_when_client_throws_exception() {
        Mockito.when(
            amazonS3Client.headObject(
                argThat((HeadObjectRequest r) -> r.bucket().equals(BUCKET_0) && r.key().equals(OBJECT_ID_0))
            )
        ).thenThrow(ApiCallTimeoutException.create("Client error"));
        assertThatThrownBy(() -> {
            amazonS3V2.isExistingObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to check existence of object 0_Unit/object0id");
    }

    @Test
    public void create_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(
            amazonS3Client.createBucket(argThat((CreateBucketRequest r) -> r.bucket().equals(BUCKET_0)))
        ).thenThrow(ApiCallTimeoutException.create("Client error"));
        assertThatCode(() -> {
            amazonS3V2.createContainer(CONTAINER_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to create container");
    }

    @Test
    public void delete_object_should_throw_exception_when_client_throws_exception() {
        Mockito.doThrow(ApiCallTimeoutException.create("Client error"))
            .when(amazonS3Client)
            .deleteObject(any(DeleteObjectRequest.class));
        assertThatCode(() -> {
            amazonS3V2.deleteObject(BUCKET_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to delete object 0.unit/object0id");
    }

    @Test
    public void get_object_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );
        assertThatCode(() -> {
            amazonS3V2.getObject(CONTAINER_0, OBJECT_ID_0);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to download object 0_Unit/object0id");
    }

    @Test
    public void get_object_digest_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V2.getObjectDigest(CONTAINER_0, OBJECT_ID_0, DigestType.SHA512, false);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to compute digest of object 0_Unit/object0id");
    }

    @Test
    public void get_object_metadata_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V2.getObjectMetadata(CONTAINER_0, OBJECT_ID_0, false);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to get metadata of object 0_Unit/object0id");
    }

    @Test
    public void upload_object_should_throw_exception_when_client_putobject_throws_exception() {
        FakeInputStream fakeInputStream = new FakeInputStream(3500L);
        Mockito.when(
            amazonS3ClientWithoutRetries.putObject(any(PutObjectRequest.class), any(RequestBody.class))
        ).thenThrow(ApiCallTimeoutException.create("Client error"));
        assertThatCode(() -> {
            amazonS3V2.putObject(CONTAINER_0, OBJECT_ID_0, fakeInputStream, DigestType.SHA512, 3_500L);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to upload object");
    }

    @Test
    public void upload_object_should_throw_exception_when_client_copyobject_throws_exception() {
        InputStream stream = new FakeInputStream(0);
        Mockito.when(
            amazonS3ClientWithoutRetries.putObject(any(PutObjectRequest.class), any(RequestBody.class))
        ).thenReturn(PutObjectResponse.builder().build());

        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream(
            GetObjectResponse.builder().contentLength(0L).build(),
            new FakeInputStream(0)
        );

        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        Mockito.when(amazonS3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        assertThatCode(() -> {
            amazonS3V2.putObject(CONTAINER_0, OBJECT_ID_0, stream, DigestType.SHA512, 0L);
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to update metadata of object");
    }

    @Test
    public void list_container_should_throw_exception_when_client_throws_exception() {
        Mockito.when(amazonS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );
        assertThatThrownBy(() -> {
            amazonS3V2.listContainer(CONTAINER_0, mock(ObjectListingListener.class));
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessage("Error when trying to list objects of container 0_Unit");
    }

    @Test
    public void put_large_object_should_complete_successfully_when_client_ok() throws Exception {
        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse.builder()
            .uploadId("uploadId")
            .build();
        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            createMultipartUploadResponse
        );
        Mockito.when(
            amazonS3ClientWithoutRetries.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))
        ).thenAnswer(args -> {
            UploadPartRequest uploadPartRequest = args.getArgument(0);
            return UploadPartResponse.builder().eTag("etag" + uploadPartRequest.partNumber()).build();
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(
            CompleteMultipartUploadResponse.builder().build()
        );

        // When
        amazonS3V2.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L);

        // Then
        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        ArgumentCaptor<UploadPartRequest> uploadPartRequestArgumentCaptor = ArgumentCaptor.forClass(
            UploadPartRequest.class
        );
        ArgumentCaptor<RequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);
        Mockito.verify(amazonS3ClientWithoutRetries, times(2)).uploadPart(
            uploadPartRequestArgumentCaptor.capture(),
            requestBodyArgumentCaptor.capture()
        );

        UploadPartRequest uploadPartRequest1 = uploadPartRequestArgumentCaptor.getAllValues().get(0);
        RequestBody requestBody1 = requestBodyArgumentCaptor.getAllValues().get(0);
        assertThat(uploadPartRequest1.uploadId()).isEqualTo("uploadId");
        assertThat(uploadPartRequest1.partNumber()).isEqualTo(1);
        assertThat(requestBody1.optionalContentLength().orElseThrow()).isEqualTo(5_242_880L);
        assertThat(uploadPartRequest1.bucket()).isEqualTo("0.unit");

        UploadPartRequest uploadPartRequest2 = uploadPartRequestArgumentCaptor.getAllValues().get(1);
        RequestBody requestBody2 = requestBodyArgumentCaptor.getAllValues().get(1);
        assertThat(uploadPartRequest2.uploadId()).isEqualTo("uploadId");
        assertThat(uploadPartRequest2.partNumber()).isEqualTo(2);
        assertThat(requestBody2.optionalContentLength().orElseThrow()).isEqualTo(757_120L);
        assertThat(uploadPartRequest2.bucket()).isEqualTo("0.unit");

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        Mockito.verify(amazonS3Client).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
        assertThat(completeMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("uploadId");
        assertThat(
            completeMultipartUploadRequestArgumentCaptor
                .getValue()
                .multipartUpload()
                .parts()
                .stream()
                .map(CompletedPart::eTag)
        ).containsExactly("etag1", "etag2");

        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void put_large_object_should_throw_exception_when_client_throws_exception_during_multipart_initialization() {
        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );
        assertThatThrownBy(
            () ->
                amazonS3V2.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");
        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void put_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_upload() {
        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse.builder()
            .uploadId("uploadId")
            .build();
        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            createMultipartUploadResponse
        );
        Mockito.when(
            amazonS3ClientWithoutRetries.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))
        ).thenThrow(ApiCallTimeoutException.create("Client error"));
        assertThatThrownBy(
            () ->
                amazonS3V2.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");

        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        Mockito.verify(amazonS3ClientWithoutRetries).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("uploadId");
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void put_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_completion() {
        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse.builder()
            .uploadId("uploadId")
            .build();
        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            createMultipartUploadResponse
        );
        Mockito.when(
            amazonS3ClientWithoutRetries.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))
        ).thenAnswer(args -> {
            UploadPartRequest uploadPartRequest = args.getArgument(0);
            return UploadPartResponse.builder().eTag("etag" + uploadPartRequest.partNumber()).build();
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        // When / Then
        assertThatThrownBy(
            () ->
                amazonS3V2.writeObject(CONTAINER_0, OBJECT_ID_0, new FakeInputStream(1L), DigestType.SHA512, 6_000_000L)
        )
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to upload object 0.unit/object0id")
            .hasRootCauseMessage("Client error");

        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        Mockito.verify(amazonS3ClientWithoutRetries, times(2)).uploadPart(
            any(UploadPartRequest.class),
            any(RequestBody.class)
        );
        Mockito.verify(amazonS3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("uploadId");
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_small_object_should_complete_successfully_when_client_ok() throws Exception {
        // Given
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength(6906L).build(),
            PropertiesUtils.getResourceAsStream("file1.pdf")
        );

        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Mockito.when(amazonS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(
            CopyObjectResponse.builder().build()
        );

        // When
        amazonS3V2.checkObjectDigestAndStoreDigest(
            CONTAINER_0,
            OBJECT_ID_0,
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
            DigestType.SHA512,
            6906L
        );

        // Then
        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));

        ArgumentCaptor<CopyObjectRequest> copyObjectRequestArgumentCaptor = ArgumentCaptor.forClass(
            CopyObjectRequest.class
        );
        Mockito.verify(amazonS3Client).copyObject(copyObjectRequestArgumentCaptor.capture());
        assertThat(copyObjectRequestArgumentCaptor.getValue().sourceBucket()).isEqualTo("0.unit");
        assertThat(copyObjectRequestArgumentCaptor.getValue().sourceKey()).isEqualTo(OBJECT_ID_0);
        assertThat(copyObjectRequestArgumentCaptor.getValue().destinationBucket()).isEqualTo("0.unit");
        assertThat(copyObjectRequestArgumentCaptor.getValue().destinationKey()).isEqualTo(OBJECT_ID_0);
        assertThat(copyObjectRequestArgumentCaptor.getValue().metadataDirective()).isEqualTo(MetadataDirective.REPLACE);
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_large_object_should_complete_successfully_when_client_ok() throws Exception {
        // Given
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength(6_000_000L).build(),
            new FixedPatternFakeInputStream(6_000_000L)
        );
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        CreateMultipartUploadResponse createMultipartUploadResult = CreateMultipartUploadResponse.builder()
            .uploadId("copyUploadId")
            .build();
        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            createMultipartUploadResult
        );
        Mockito.when(amazonS3Client.uploadPartCopy(any(UploadPartCopyRequest.class))).thenAnswer(args -> {
            UploadPartCopyRequest uploadPartCopyRequest = args.getArgument(0);
            return UploadPartCopyResponse.builder()
                .copyPartResult(r -> r.eTag("etag" + uploadPartCopyRequest.partNumber()))
                .build();
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(
            CompleteMultipartUploadResponse.builder().build()
        );

        // When
        amazonS3V2.checkObjectDigestAndStoreDigest(
            CONTAINER_0,
            OBJECT_ID_0,
            "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
            DigestType.SHA512,
            6_000_000L
        );

        // Then
        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));

        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        ArgumentCaptor<UploadPartCopyRequest> uploadPartCopyRequestArgumentCaptor = ArgumentCaptor.forClass(
            UploadPartCopyRequest.class
        );
        Mockito.verify(amazonS3Client, times(2)).uploadPartCopy(uploadPartCopyRequestArgumentCaptor.capture());

        UploadPartCopyRequest uploadPartCopyRequest1 = uploadPartCopyRequestArgumentCaptor.getAllValues().get(0);
        assertThat(uploadPartCopyRequest1.uploadId()).isEqualTo("copyUploadId");
        assertThat(uploadPartCopyRequest1.partNumber()).isEqualTo(1);
        assertThat(uploadPartCopyRequest1.copySourceRange()).isEqualTo("bytes=0-5242879");
        assertThat(uploadPartCopyRequest1.sourceBucket()).isEqualTo("0.unit");
        assertThat(uploadPartCopyRequest1.sourceKey()).isEqualTo(OBJECT_ID_0);
        assertThat(uploadPartCopyRequest1.sourceKey()).isEqualTo(OBJECT_ID_0);

        UploadPartCopyRequest uploadPartCopyRequest2 = uploadPartCopyRequestArgumentCaptor.getAllValues().get(1);
        assertThat(uploadPartCopyRequest2.uploadId()).isEqualTo("copyUploadId");
        assertThat(uploadPartCopyRequest2.partNumber()).isEqualTo(2);
        assertThat(uploadPartCopyRequest2.copySourceRange()).isEqualTo("bytes=5242880-5999999");
        assertThat(uploadPartCopyRequest2.sourceBucket()).isEqualTo("0.unit");
        assertThat(uploadPartCopyRequest2.sourceKey()).isEqualTo(OBJECT_ID_0);

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        Mockito.verify(amazonS3Client).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
        assertThat(completeMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("copyUploadId");
        assertThat(
            completeMultipartUploadRequestArgumentCaptor
                .getValue()
                .multipartUpload()
                .parts()
                .stream()
                .map(CompletedPart::eTag)
        ).containsExactly("etag1", "etag2");

        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_when_client_throws_exception_during_get_object() {
        // Given
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V2.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to download object");

        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_when_client_throws_exception_during_multipart_initialization()
        throws Exception {
        // Given
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength(6_000_000L).build(),
            new FixedPatternFakeInputStream(6_000_000L)
        );
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V2.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));
        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_copy() {
        // Given
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength(6_000_000L).build(),
            new FixedPatternFakeInputStream(6_000_000L)
        );
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("copyUploadId").build()
        );

        Mockito.when(amazonS3Client.uploadPartCopy(any(UploadPartCopyRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V2.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));
        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        Mockito.verify(amazonS3Client).uploadPartCopy(any(UploadPartCopyRequest.class));

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("copyUploadId");
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }

    @Test
    public void store_digest_of_large_object_should_throw_exception_with_proper_upload_cleanup_when_client_throws_exception_during_multipart_completion()
        throws Exception {
        // Given
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength(6_000_000L).build(),
            new FixedPatternFakeInputStream(6_000_000L)
        );
        Mockito.when(amazonS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Mockito.when(amazonS3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(
            CreateMultipartUploadResponse.builder().uploadId("copyUploadId").build()
        );

        Mockito.when(amazonS3Client.uploadPartCopy(any(UploadPartCopyRequest.class))).thenAnswer(args -> {
            UploadPartCopyRequest uploadPartCopyRequest = args.getArgument(0);
            return UploadPartCopyResponse.builder()
                .copyPartResult(c -> c.eTag("etag" + uploadPartCopyRequest.partNumber()))
                .build();
        });
        Mockito.when(amazonS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(
            ApiCallTimeoutException.create("Client error")
        );

        // When / Then
        assertThatThrownBy(() -> {
            amazonS3V2.checkObjectDigestAndStoreDigest(
                CONTAINER_0,
                OBJECT_ID_0,
                "c98f6bc11a3ba8ec9260b0b6b79bccc7a5916f8914f66263de8e2862238599165f355fa92b38ac24e1232a80e5f5d64606a60ab6e217ea52de6b553ad1327ba8",
                DigestType.SHA512,
                6_000_000L
            );
        })
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasMessageContaining("Error when trying to updating large object digest 0.unit/object0id");

        Mockito.verify(amazonS3Client).getObject(any(GetObjectRequest.class));
        Mockito.verify(amazonS3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        Mockito.verify(amazonS3Client, times(2)).uploadPartCopy(any(UploadPartCopyRequest.class));
        Mockito.verify(amazonS3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
            AbortMultipartUploadRequest.class
        );
        Mockito.verify(amazonS3Client).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        assertThat(abortMultipartUploadRequestArgumentCaptor.getValue().uploadId()).isEqualTo("copyUploadId");
        verifyNoMoreInteractions(amazonS3Client);
        verifyNoMoreInteractions(amazonS3ClientWithoutRetries);
    }
}
