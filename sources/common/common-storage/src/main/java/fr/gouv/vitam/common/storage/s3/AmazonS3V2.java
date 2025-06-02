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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDigestMismatchException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Amazon SDK S3 V2 implementation
 */
public class AmazonS3V2 extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AmazonS3V2.class);

    // Amazon SDK S3 V2 metadata keys
    private static final String X_OBJECT_META_DIGEST = "Digest";
    private static final String X_OBJECT_META_DIGEST_TYPE = "Digest-Type";
    private static final String NO_SUCH_BUCKET_ERROR_CODE = "NoSuchBucket";
    private static final AlertService ALERT_SERVICE = new AlertServiceImpl();
    private static final long MB_TO_BYTES = 1024L * 1024L;
    private static final int MAX_RETRIES = 3;
    private static final int NO_RETRIES = 1;

    private final boolean s3DisableMultipartUpload;
    private final long s3MaxUploadPartSize;
    private final int s3MultiPartCleanNbRetries;
    private final int s3MultiPartCleanWaitingTimeInMilliseconds;

    private final SdkHttpClient httpClient;
    private final S3Client s3ClientWithRetry;
    private final S3Client s3ClientWithoutRetry;

    public AmazonS3V2(StorageConfiguration configuration)
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        super(configuration);
        checkConfiguration(configuration);
        this.s3DisableMultipartUpload = configuration.isS3DisableMultipartUpload();
        this.s3MaxUploadPartSize = configuration.getS3MaxUploadPartSizeMB() * MB_TO_BYTES;
        this.s3MultiPartCleanNbRetries = configuration.getS3MultiPartCleanNbRetries();
        this.s3MultiPartCleanWaitingTimeInMilliseconds = configuration.getS3MultiPartCleanWaitingTimeInMilliseconds();

        // Build S3 client
        this.httpClient = createHttpClient(configuration);
        this.s3ClientWithRetry = createS3Client(configuration, httpClient, true);
        this.s3ClientWithoutRetry = createS3Client(configuration, httpClient, false);
    }

    @VisibleForTesting
    public AmazonS3V2(
        StorageConfiguration configuration,
        SdkHttpClient httpClient,
        S3Client s3ClientWithRetry,
        S3Client s3ClientWithoutRetry
    ) {
        super(configuration);
        this.httpClient = httpClient;
        this.s3ClientWithRetry = s3ClientWithRetry;
        this.s3ClientWithoutRetry = s3ClientWithoutRetry;
        this.s3DisableMultipartUpload = configuration.isS3DisableMultipartUpload();
        this.s3MaxUploadPartSize = configuration.getS3MaxUploadPartSizeMB() * MB_TO_BYTES;
        this.s3MultiPartCleanNbRetries = configuration.getS3MultiPartCleanNbRetries();
        this.s3MultiPartCleanWaitingTimeInMilliseconds = configuration.getS3MultiPartCleanWaitingTimeInMilliseconds();
    }

    private static void checkConfiguration(StorageConfiguration configuration) {
        if (configuration.isS3DisableMultipartUpload()) {
            return;
        }
        if (
            configuration.getS3MaxUploadPartSizeMB() < StorageConfiguration.MIN_UPLOAD_PART_SIZE_MB ||
            configuration.getS3MaxUploadPartSizeMB() > StorageConfiguration.MAX_UPLOAD_PART_SIZE_MB
        ) {
            throw new IllegalArgumentException(
                "Invalid max part upload size %d. Valid values must be in the range [%d MB - %d MB]".formatted(
                        configuration.getS3MaxUploadPartSizeMB(),
                        StorageConfiguration.MAX_UPLOAD_PART_SIZE_MB,
                        StorageConfiguration.MAX_UPLOAD_PART_SIZE_MB
                    )
            );
        }
    }

    private static SdkHttpClient createHttpClient(StorageConfiguration configuration)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        return ApacheHttpClient.builder()
            .maxConnections(configuration.getS3MaxConnections())
            .connectionTimeout(Duration.ofMillis(configuration.getS3ConnectionTimeout()))
            .socketTimeout(Duration.ofMillis(configuration.getS3SocketTimeout()))
            .socketFactory(createSSLConnectionSocketFactory(configuration))
            .build();
    }

    private static SSLConnectionSocketFactory createSSLConnectionSocketFactory(StorageConfiguration configuration)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        // HTTPS configuration
        if (!configuration.getS3Endpoint().startsWith("https://")) {
            return null;
        }
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(configuration.getS3TrustStore())) {
            trustStore.load(fis, configuration.getS3TrustStorePassword().toCharArray());
        }

        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(trustStore, null).build();

        HostnameVerifier hostnameVerifier = configuration.isS3IgnoreCertificateHostnameValidation()
            ? NoopHostnameVerifier.INSTANCE
            : SSLConnectionSocketFactory.getDefaultHostnameVerifier();

        return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
    }

    private static S3Client createS3Client(
        StorageConfiguration configuration,
        SdkHttpClient httpClient,
        boolean retryable
    ) {
        // Credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            configuration.getS3AccessKey(),
            configuration.getS3SecretKey()
        );

        // Use https://s3-server/my-bucket/my-object syntax instead of https://my-bucket.s3-server/my-object
        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(configuration.isS3PathStyleAccessEnabled())
            .build();

        return S3Client.builder()
            .endpointOverride(URI.create(configuration.getS3Endpoint()))
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .region(
                StringUtils.isEmpty(configuration.getS3RegionName())
                    ? Region.US_EAST_1
                    : Region.of(configuration.getS3RegionName())
            )
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .httpClient(httpClient)
            .serviceConfiguration(serviceConfiguration)
            .overrideConfiguration(
                cfg ->
                    cfg
                        .apiCallTimeout(
                            configuration.getS3ClientExecutionTimeout() != 0
                                ? Duration.ofMillis(configuration.getS3ClientExecutionTimeout())
                                : null
                        )
                        .apiCallAttemptTimeout(
                            configuration.getS3RequestTimeout() != 0
                                ? Duration.ofMillis(configuration.getS3RequestTimeout())
                                : null
                        )
                        .retryStrategy(
                            StandardRetryStrategy.builder().maxAttempts(retryable ? MAX_RETRIES : NO_RETRIES).build()
                        )
            )
            .build();
    }

    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Create container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        String bucketName = generateBucketName(containerName);
        try {
            CreateBucketRequest request = CreateBucketRequest.builder().bucket(bucketName).build();
            s3ClientWithRetry.createBucket(request);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            LOGGER.warn("Container " + containerName + " already exists", e);
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException("Error when trying to create container", e);
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Check existence of container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        String bucketName = generateBucketName(containerName);
        if (super.isExistingContainerInCache(containerName)) {
            return true;
        }
        try {
            boolean exists = bucketExists(bucketName);
            cacheExistsContainer(containerName, exists);
            return exists;
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException("Error when trying to check existence of container", e);
        }
    }

    private boolean bucketExists(String bucketName) throws SdkException {
        try {
            // If headBucket doesn't throw an exception, the bucket exists
            HeadBucketRequest request = HeadBucketRequest.builder().bucket(bucketName).build();
            s3ClientWithRetry.headBucket(request);
            return true;
        } catch (NoSuchBucketException e) {
            LOGGER.debug("Container '" + bucketName + " does not exist", e);
            return false;
        }
    }

    @Override
    public void writeObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Upload object %s in container %s", objectName, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );

        String bucketName = generateBucketName(containerName);

        Stopwatch times = Stopwatch.createStarted();
        try {
            if (useMultipartUpload(size)) {
                putLargeObject(bucketName, objectName, inputStream, size);
            } else {
                putSmallObject(bucketName, objectName, inputStream, size);
            }
        } finally {
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + getConfiguration().getProvider(),
                    containerName,
                    "REAL_S3_PUT_OBJECT",
                    times.elapsed(TimeUnit.MILLISECONDS)
                );
        }
    }

    @Override
    public void checkObjectDigestAndStoreDigest(
        String containerName,
        String objectName,
        String objectDigest,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );

        String bucketName = generateBucketName(containerName);

        String computedDigest = computeObjectDigest(containerName, objectName, digestType);
        if (!objectDigest.equals(computedDigest)) {
            throw new ContentAddressableStorageDigestMismatchException(
                "Illegal state for container " +
                containerName +
                " and object " +
                objectName +
                ". Stream digest " +
                objectDigest +
                " is not equal to computed digest " +
                computedDigest
            );
        }

        storeDigest(containerName, objectName, digestType, objectDigest, bucketName, size);
    }

    private void putLargeObject(String bucketName, String objectName, InputStream stream, long size)
        throws ContentAddressableStorageServerException {
        LOGGER.info("Uploading large object {}/{} ({} bytes)", bucketName, objectName, size);

        String uploadId = null;
        boolean uploadSucceeded = false;
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();

            uploadId = initiateMultipartUpload(bucketName, objectName, Collections.emptyMap());

            List<CompletedPart> completedParts = new ArrayList<>();

            long sentBytes = 0L;
            long nbParts = (size + this.s3MaxUploadPartSize - 1) / this.s3MaxUploadPartSize;

            for (int partNumber = 1; sentBytes < size; partNumber++) {
                long partSize = Math.min(this.s3MaxUploadPartSize, (size - sentBytes));
                CompletedPart completedPart = uploadPart(
                    bucketName,
                    objectName,
                    stream,
                    partNumber,
                    nbParts,
                    partSize,
                    uploadId
                );
                completedParts.add(completedPart);
                sentBytes += partSize;
            }

            completeMultipartUpload(bucketName, objectName, uploadId, completedParts);
            uploadSucceeded = true;
            LOGGER.info(
                "Large object {}/{} uploaded successfully  ({} bytes, {} ms)",
                bucketName,
                objectName,
                size,
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to upload object " + bucketName + "/" + objectName,
                e
            );
        } finally {
            StreamUtils.closeSilently(stream);
            if (uploadId != null && !uploadSucceeded) {
                tryCleanupMultiPartUpload(bucketName, objectName, uploadId);
            }
        }
    }

    private String initiateMultipartUpload(String containerName, String objectName, Map<String, String> metadata)
        throws SdkException {
        LOGGER.debug("Initiating multipart upload {}/{}", containerName, objectName);
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
            .bucket(containerName)
            .key(objectName)
            .metadata(metadata)
            .build();
        CreateMultipartUploadResponse multipartUpload = s3ClientWithRetry.createMultipartUpload(request);
        String uploadId = multipartUpload.uploadId();
        LOGGER.debug("Initiated multipart upload {}", uploadId);
        return uploadId;
    }

    private CompletedPart uploadPart(
        String containerName,
        String objectName,
        InputStream stream,
        int partNumber,
        long nbParts,
        long partSize,
        String uploadId
    ) throws ContentAddressableStorageServerException {
        LOGGER.info(
            "Multipart upload of {}/{} - Part {}/{} ({} bytes)",
            containerName,
            objectName,
            partNumber,
            nbParts,
            partSize
        );
        try (
            InputStream partInputStream = BoundedInputStream.builder()
                .setInputStream(stream)
                .setMaxCount(partSize)
                .setPropagateClose(false)
                .get()
        ) {
            UploadPartRequest request = UploadPartRequest.builder()
                .bucket(containerName)
                .uploadId(uploadId)
                .contentLength(partSize)
                .partNumber(partNumber)
                .key(objectName)
                .build();
            UploadPartResponse uploadPartResult = s3ClientWithoutRetry.uploadPart(
                request,
                fixedLengthInputStreamRequestBody(partInputStream, partSize)
            );
            return CompletedPart.builder().partNumber(partNumber).eTag(uploadPartResult.eTag()).build();
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    private void completeMultipartUpload(
        String bucketName,
        String objectName,
        String uploadId,
        List<CompletedPart> completedParts
    ) {
        LOGGER.debug("Completing multi-part upload {} of {}/{}", uploadId, bucketName, objectName);
        // Complete the multipart upload.
        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(objectName)
            .uploadId(uploadId)
            .multipartUpload(u -> u.parts(completedParts))
            .build();
        s3ClientWithRetry.completeMultipartUpload(request);
    }

    private void tryCleanupMultiPartUpload(String bucketName, String objectName, String uploadId) {
        try {
            new RetryableOnException<>(
                new RetryableParameters(
                    this.s3MultiPartCleanNbRetries,
                    this.s3MultiPartCleanWaitingTimeInMilliseconds,
                    this.s3MultiPartCleanWaitingTimeInMilliseconds,
                    this.s3MultiPartCleanWaitingTimeInMilliseconds,
                    TimeUnit.MILLISECONDS
                )
            ).exec(() -> {
                LOGGER.error(
                    "Multi-part upload for object {}/{} with id {} failed. Cleaning up...",
                    bucketName,
                    objectName,
                    uploadId
                );
                AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .uploadId(uploadId)
                    .build();
                s3ClientWithRetry.abortMultipartUpload(request);
                return null;
            });

            LOGGER.warn(
                "Cleanup of multi-part upload for object {}/{} with id {} succeeded",
                bucketName,
                objectName,
                uploadId
            );
        } catch (Exception e) {
            String msg = String.format(
                "An error occurred during multi-part upload for object %s/%s with id %s." +
                "Please cleanup your S3 storage server manually using the S3 AbortMultipartUpload API",
                bucketName,
                objectName,
                uploadId
            );
            ALERT_SERVICE.createAlert(msg);
            LOGGER.error(msg, e);
        }
    }

    private void putSmallObject(String bucketName, String objectName, InputStream stream, long size)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .contentLength(size)
                .build();
            s3ClientWithoutRetry.putObject(request, fixedLengthInputStreamRequestBody(stream, size));
        } catch (S3Exception e) {
            if (NO_SUCH_BUCKET_ERROR_CODE.equals(e.awsErrorDetails().errorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    "Error when trying to upload object : container does not exists",
                    e
                );
            }
            throw new ContentAddressableStorageServerException("Error when trying to upload object", e);
        } catch (SdkException | IOException e) {
            throw new ContentAddressableStorageServerException("Error when trying to upload object", e);
        }
    }

    private void storeDigest(
        String containerName,
        String objectName,
        DigestType digestType,
        String digest,
        String bucketName,
        long size
    ) throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (useMultipartUpload(size)) {
            storeLargeObjectDigest(bucketName, objectName, digestType, digest, size);
        } else {
            storeSmallObjectDigest(bucketName, objectName, digestType, digest);
        }
        PerformanceLogger.getInstance()
            .log(
                "STP_Offer_" + getConfiguration().getProvider(),
                containerName,
                "STORE_DIGEST_IN_METADATA",
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
    }

    private void storeSmallObjectDigest(String bucketName, String objectName, DigestType digestType, String digest)
        throws ContentAddressableStorageException {
        Map<String, String> metadataToUpdate = createObjectMetadata(digestType, digest);

        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(objectName)
                .destinationBucket(bucketName)
                .destinationKey(objectName)
                .metadata(metadataToUpdate)
                .metadataDirective(MetadataDirective.REPLACE)
                .build();
            s3ClientWithRetry.copyObject(request);
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException("Error when trying to update metadata of object", e);
        }
    }

    private static Map<String, String> createObjectMetadata(DigestType digestType, String digest) {
        return Map.ofEntries(
            Map.entry(X_OBJECT_META_DIGEST, digest),
            Map.entry(X_OBJECT_META_DIGEST_TYPE, digestType.getName())
        );
    }

    private void storeLargeObjectDigest(
        String bucketName,
        String objectName,
        DigestType digestType,
        String digest,
        long size
    ) throws ContentAddressableStorageException {
        LOGGER.info(
            "Updating digest for large object {}/{} (digest: {}, size: {} bytes)",
            bucketName,
            objectName,
            digest,
            size
        );

        String copyObjectUploadId = null;
        boolean copySucceeded = false;
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();

            Map<String, String> metadataToUpdate = createObjectMetadata(digestType, digest);
            copyObjectUploadId = initiateMultipartUpload(bucketName, objectName, metadataToUpdate);

            List<CompletedPart> completedParts = new ArrayList<>();

            long offset = 0L;
            long nbParts = (size + this.s3MaxUploadPartSize - 1) / this.s3MaxUploadPartSize;

            for (int partNumber = 1; offset < size; partNumber++) {
                long partSize = Math.min(this.s3MaxUploadPartSize, (size - offset));

                CompletedPart completedPart = copyPart(
                    bucketName,
                    objectName,
                    partNumber,
                    nbParts,
                    partSize,
                    copyObjectUploadId,
                    offset
                );
                completedParts.add(completedPart);
                offset += partSize;
            }

            completeMultipartUpload(bucketName, objectName, copyObjectUploadId, completedParts);
            copySucceeded = true;
            LOGGER.info(
                "Large object {}/{} updated successfully with digest {} ({} bytes, {} ms)",
                bucketName,
                objectName,
                digest,
                size,
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to updating large object digest " + bucketName + "/" + objectName,
                e
            );
        } finally {
            if (copyObjectUploadId != null && !copySucceeded) {
                tryCleanupMultiPartUpload(bucketName, objectName, copyObjectUploadId);
            }
        }
    }

    private CompletedPart copyPart(
        String containerName,
        String objectName,
        int partNumber,
        long nbParts,
        long partSize,
        String uploadId,
        long offset
    ) {
        LOGGER.info(
            "Copying multi-part for digest update of {}/{} - Part {}/{} ({} bytes)",
            containerName,
            objectName,
            partNumber,
            nbParts,
            partSize
        );

        UploadPartCopyRequest uploadPartCopyRequest = UploadPartCopyRequest.builder()
            .sourceBucket(containerName)
            .sourceKey(objectName)
            .destinationBucket(containerName)
            .destinationKey(objectName)
            .uploadId(uploadId)
            .copySourceRange(String.format("bytes=%d-%d", offset, offset + partSize - 1))
            .partNumber(partNumber)
            .build();
        UploadPartCopyResponse uploadPartCopyResponse = s3ClientWithRetry.uploadPartCopy(uploadPartCopyRequest);

        return CompletedPart.builder()
            .partNumber(partNumber)
            .eTag(uploadPartCopyResponse.copyPartResult().eTag())
            .build();
    }

    @Override
    public ObjectContent getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Download object %s from container %s", objectName, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        String bucketName = generateBucketName(containerName);
        try {
            // Get the object and its metadata
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(objectName).build();
            ResponseInputStream<GetObjectResponse> responseInputStream = s3ClientWithRetry.getObject(request);

            long size = responseInputStream.response().contentLength();

            return new ObjectContent(responseInputStream, size);
        } catch (NoSuchKeyException e) {
            throw new ContentAddressableStorageNotFoundException(
                "Error when trying to download object " + containerName + "/" + objectName + ". Object not found.",
                e
            );
        } catch (NoSuchBucketException e) {
            throw new ContentAddressableStorageNotFoundException(
                "Error when trying to download object " + containerName + "/" + objectName + ". Container not found.",
                e
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to download object " + containerName + "/" + objectName,
                e
            );
        }
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Delete object %s from container %s", objectName, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        String bucketName = generateBucketName(containerName);

        try {
            // Delete the object
            DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(objectName).build();
            s3ClientWithRetry.deleteObject(request);
        } catch (NoSuchBucketException e) {
            throw new ContentAddressableStorageNotFoundException(
                "Error when trying to delete object " + containerName + "/" + objectName + ". Container not found.",
                e
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to delete object " + containerName + "/" + objectName,
                e
            );
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Check existence of object %s in container %s", objectName, containerName));
        String bucketName = generateBucketName(containerName);
        try {
            try {
                // If headObject doesn't throw an exception, the object exists
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectName).build();
                s3ClientWithRetry.headObject(request);
                return true;
            } catch (NoSuchKeyException | NoSuchBucketException e) {
                // Object or container doesn't exist
                return false;
            }
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to check existence of object " + containerName + "/" + objectName,
                e
            );
        }
    }

    @Override
    public String getObjectDigest(String containerName, String objectName, DigestType digestType, boolean noCache)
        throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Get digest of object %s in container %s", objectName, containerName));

        if (!noCache) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            String bucketName = generateBucketName(containerName);
            try {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectName).build();
                HeadObjectResponse headObjectResponse = s3ClientWithRetry.headObject(request);

                PerformanceLogger.getInstance()
                    .log(
                        "STP_Offer_" + getConfiguration().getProvider(),
                        containerName,
                        "READ_DIGEST_FROM_METADATA",
                        stopwatch.elapsed(TimeUnit.MILLISECONDS)
                    );

                // Get the digest from the metadata
                return getDigestFromObjectMetadata(
                    containerName,
                    objectName,
                    digestType,
                    bucketName,
                    headObjectResponse
                );
            } catch (NoSuchKeyException e) {
                throw new ContentAddressableStorageNotFoundException(
                    "Error when trying to compute digest of object " +
                    objectName +
                    " from container " +
                    containerName +
                    ". Object not found.",
                    e
                );
            } catch (SdkException e) {
                throw new ContentAddressableStorageServerException(
                    "Error when trying to compute digest of object " + containerName + "/" + objectName,
                    e
                );
            }
        }

        return computeObjectDigest(containerName, objectName, digestType);
    }

    private String getDigestFromObjectMetadata(
        String containerName,
        String objectName,
        DigestType digestType,
        String bucketName,
        HeadObjectResponse headObjectResponse
    ) throws ContentAddressableStorageException {
        if (
            null != headObjectResponse &&
            headObjectResponse.metadata().containsKey(X_OBJECT_META_DIGEST) &&
            headObjectResponse.metadata().containsKey(X_OBJECT_META_DIGEST_TYPE) &&
            digestType.getName().equals(headObjectResponse.metadata().get(X_OBJECT_META_DIGEST_TYPE)) &&
            null != headObjectResponse.metadata().get(X_OBJECT_META_DIGEST)
        ) {
            return headObjectResponse.metadata().get(X_OBJECT_META_DIGEST);
        } else {
            LOGGER.warn(
                String.format(
                    "Could not retrieve cached digest of object '%s' in container '%s'. Recomputing digest",
                    objectName,
                    containerName
                )
            );
            Pair<String, Long> objectDigestAndSize = computeObjectDigestAndSize(containerName, objectName, digestType);
            String digestToStore = objectDigestAndSize.getKey();
            Long objectSize = objectDigestAndSize.getValue();
            storeDigest(containerName, objectName, digestType, digestToStore, bucketName, objectSize);
            return digestToStore;
        }
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Get information of container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        // we do not call the storage since it is not pertinent in s3
        final ContainerInformation containerInformation = new ContainerInformation();
        containerInformation.setUsableSpace(-1);
        return containerInformation;
    }

    @Override
    public MetadatasObject getObjectMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Get metadata of object %s in container %s", objectId, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectId
        );
        String bucketName = generateBucketName(containerName);
        try {
            MetadatasStorageObject result = new MetadatasStorageObject();
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectId).build();
            HeadObjectResponse headObjectResponse = s3ClientWithRetry.headObject(request);
            // ugly
            result.setType(containerName.split("_")[1]);
            result.setObjectName(objectId);
            result.setDigest(
                noCache
                    ? computeObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType())
                    : getDigestFromObjectMetadata(
                        containerName,
                        objectId,
                        VitamConfiguration.getDefaultDigestType(),
                        bucketName,
                        headObjectResponse
                    )
            );
            result.setFileSize(headObjectResponse.contentLength());
            result.setLastModifiedDate(headObjectResponse.lastModified().toString());
            return result;
        } catch (NoSuchKeyException e) {
            throw new ContentAddressableStorageNotFoundException(
                "Error when trying to get metadata of object " + containerName + "/" + objectId + ". Object not found.",
                e
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to get metadata of object " + containerName + "/" + objectId,
                e
            );
        }
    }

    @Override
    public void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        LOGGER.debug(String.format("Listing of object in container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        String bucketName = generateBucketName(containerName);
        try {
            String continuationToken = null;
            do {
                // Create a request to list objects
                ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(getConfiguration().getS3ListObjectBulkSize())
                    .continuationToken(continuationToken)
                    .build();

                // List objects
                ListObjectsV2Response listObjectsV2Response = s3ClientWithRetry.listObjectsV2(listObjectsV2Request);

                // Process each object
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                    LOGGER.debug("Found object {}/{} ({} bytes)", containerName, s3Object.key(), s3Object.size());
                    objectListingListener.handleObjectEntry(new ObjectEntry(s3Object.key(), s3Object.size()));
                }

                // Get the continuation token for the next page
                continuationToken = listObjectsV2Response.nextContinuationToken();
            } while (continuationToken != null);
        } catch (NoSuchBucketException e) {
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName,
                e
            );
        } catch (SdkException e) {
            throw new ContentAddressableStorageServerException(
                "Error when trying to list objects of container " + containerName,
                e
            );
        }
    }

    @Override
    public void close() {
        s3ClientWithRetry.close();
        s3ClientWithoutRetry.close();
        httpClient.close();
    }

    /**
     * Generate a valid bucket name from the container name : replace non-alphanumeric
     * values by '.', lowercase every alphabetic value and remove '.' at start and end.
     *
     * @param containerName vitam container name
     * @return bucket name valid according to s3 API specification
     * @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">General purpose bucket naming rules</a>
     */
    public String generateBucketName(String containerName) {
        String bucketName = containerName.replaceAll("[^A-Za-z0-9]", ".").toLowerCase();
        bucketName = StringUtils.strip(bucketName, ".");
        LOGGER.debug(String.format("Generated bucket name %s from container name %s", bucketName, containerName));
        BucketNameUtils.validateBucketName(bucketName);
        return bucketName;
    }

    private boolean useMultipartUpload(long size) {
        return !s3DisableMultipartUpload && (size > this.s3MaxUploadPartSize);
    }

    private static RequestBody fixedLengthInputStreamRequestBody(InputStream inputStream, long contentLength)
        throws IOException {
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(inputStream, contentLength);
        AtomicBoolean alreadyConsumed = new AtomicBoolean(false);
        ContentStreamProvider contentStreamProvider = () -> {
            if (alreadyConsumed.getAndSet(true)) {
                throw new IllegalStateException("Cannot create a new stream. Already consumed");
            }
            return exactSizeInputStream;
        };
        return RequestBody.fromContentProvider(contentStreamProvider, contentLength, Mimetype.MIMETYPE_OCTET_STREAM);
    }
}
