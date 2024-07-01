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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ApacheHttpClientConfig;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkBaseException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * Amazon SDK S3 V1 abstract implementation Manage with all common amazon sdk s3
 * v1 methods.
 */
public class AmazonS3V1 extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AmazonS3V1.class);

    // Amazon SDK S3 V1 already adds the "X-Amz-Meta-" before
    private static final String X_OBJECT_META_DIGEST = "Digest";
    private static final String X_OBJECT_META_DIGEST_TYPE = "Digest-Type";
    private static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = NoopHostnameVerifier.INSTANCE;

    /**
     * Amazon SDK S3 V1 client
     */
    private final AmazonS3 client;

    public AmazonS3V1(StorageConfiguration configuration)
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        super(configuration);
        LOGGER.debug("Instanciation of amazon S3 V1 client");

        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
            configuration.getS3Endpoint(),
            configuration.getS3RegionName()
        );

        ClientConfiguration clientConfig = new ClientConfiguration();
        if (StringUtils.isNotBlank(configuration.getS3SignerType())) {
            clientConfig.setSignerOverride(configuration.getS3SignerType());
        }
        if (configuration.getS3MaxConnections() > 0) {
            clientConfig.setMaxConnections(configuration.getS3MaxConnections());
        }
        if (configuration.getS3ConnectionTimeout() >= 0) {
            clientConfig.setConnectionTimeout(configuration.getS3ConnectionTimeout());
        }
        if (configuration.getS3SocketTimeout() >= 0) {
            clientConfig.setSocketTimeout(configuration.getS3SocketTimeout());
        }
        clientConfig.setRequestTimeout(configuration.getS3RequestTimeout());
        clientConfig.setClientExecutionTimeout(configuration.getS3ClientExecutionTimeout());

        if (configuration.getS3Endpoint().startsWith("https")) {
            ApacheHttpClientConfig apacheClient = clientConfig.getApacheHttpClientConfig();
            File file = new File(configuration.getS3TrustStore());
            SSLContext ctx = SSLContexts.custom()
                .loadTrustMaterial(file, configuration.getS3TrustStorePassword().toCharArray())
                .build();
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                ctx,
                ALLOW_ALL_HOSTNAME_VERIFIER
            );
            apacheClient.withSslSocketFactory(sslConnectionSocketFactory);
        }

        AWSCredentials credentials = new BasicAWSCredentials(
            configuration.getS3AccessKey(),
            configuration.getS3SecretKey()
        );

        this.client = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(clientConfig)
            .withEndpointConfiguration(endpointConfiguration)
            .withPathStyleAccessEnabled(configuration.isS3PathStyleAccessEnabled())
            .build();
    }

    @VisibleForTesting
    AmazonS3V1(StorageConfiguration configuration, AmazonS3 client) {
        super(configuration);
        this.client = client;
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
            client.createBucket(bucketName);
        } catch (AmazonServiceException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to create container with name: %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                    containerName,
                    e.getErrorCode(),
                    e.getErrorType(),
                    e.getErrorMessage()
                ),
                e
            );
            if (
                AmazonS3APIErrorCodes.BUCKET_ALREADY_EXISTS.getErrorCode().equals(e.getErrorCode()) ||
                AmazonS3APIErrorCodes.BUCKET_ALREADY_OWNED_BY_YOU.getErrorCode().equals(e.getErrorCode())
            ) {
                LOGGER.warn("Container " + containerName + " already exists");
            } else {
                throw new ContentAddressableStorageServerException("Error when trying to create container", e);
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to create container with name: %s. Reason: errorMessage=%s",
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to create container", e);
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Check existance of container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        String bucketName = generateBucketName(containerName);
        if (super.isExistingContainerInCache(containerName)) {
            return true;
        }
        try {
            boolean exists = client.doesBucketExistV2(bucketName);
            cacheExistsContainer(containerName, exists);
            return exists;
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when checking existance of container %s. Reason: errorMessage=%s",
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to check existance of container", e);
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
        storeObject(containerName, objectName, inputStream, size, bucketName);
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
            throw new ContentAddressableStorageException(
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

        storeDigest(containerName, objectName, digestType, objectDigest, bucketName);
    }

    private void storeObject(String containerName, String objectName, InputStream stream, long size, String bucketName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(size);
            try {
                client.putObject(bucketName, objectName, stream, objectMetadata);
            } catch (AmazonServiceException e) {
                LOGGER.debug(
                    String.format(
                        "Error when trying to upload object %s in container %s. Reason: errorMessage=%s",
                        objectName,
                        containerName,
                        e.getMessage()
                    ),
                    e
                );
                if (AmazonS3APIErrorCodes.NO_SUCH_BUCKET.getErrorCode().equals(e.getErrorCode())) {
                    throw new ContentAddressableStorageNotFoundException(
                        "Error when trying to upload object : container does not exists",
                        e
                    );
                } else {
                    throw new ContentAddressableStorageServerException("Error when trying to upload object", e);
                }
            } catch (SdkBaseException e) {
                LOGGER.debug(
                    String.format(
                        "Error when trying to upload object %s in container %s. Reason: errorMessage=%s",
                        objectName,
                        containerName,
                        e.getMessage()
                    ),
                    e
                );
                throw new ContentAddressableStorageServerException("Error when trying to upload object", e);
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

    private void storeDigest(
        String containerName,
        String objectName,
        DigestType digestType,
        String digest,
        String bucketName
    ) throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ObjectMetadata metadataToUpdate = new ObjectMetadata();
        metadataToUpdate.addUserMetadata(X_OBJECT_META_DIGEST, digest);
        metadataToUpdate.addUserMetadata(X_OBJECT_META_DIGEST_TYPE, digestType.getName());

        CopyObjectRequest request = new CopyObjectRequest(
            bucketName,
            objectName,
            bucketName,
            objectName
        ).withNewObjectMetadata(metadataToUpdate);

        try {
            CopyObjectResult updateMetadataResult = client.copyObject(request);
            if (updateMetadataResult == null) {
                LOGGER.error("Failed to update object metadata -> try remove object");
                throw new ContentAddressableStorageServerException(
                    "Cannot put object " + objectName + " on container " + containerName
                );
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to update metadatas of object %s in container %s. Reason: errorMessage=%s",
                    objectName,
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to update metadatas of object", e);
        }
        PerformanceLogger.getInstance()
            .log(
                "STP_Offer_" + getConfiguration().getProvider(),
                containerName,
                "STORE_DIGEST_IN_METADATA",
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
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
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, objectName);
        try {
            S3Object object = client.getObject(getObjectRequest);
            long size = object.getObjectMetadata().getContentLength();
            InputStream inputStream = object.getObjectContent().getDelegateStream();
            return new ObjectContent(inputStream, size);
        } catch (AmazonServiceException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to download object %s from container %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                    objectName,
                    containerName,
                    e.getErrorCode(),
                    e.getErrorType(),
                    e.getErrorMessage()
                ),
                e
            );
            if (AmazonS3APIErrorCodes.NO_SUCH_KEY.getErrorCode().equals(e.getErrorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName,
                    e
                );
            } else if (AmazonS3APIErrorCodes.NO_SUCH_BUCKET.getErrorCode().equals(e.getErrorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName,
                    e
                );
            } else {
                throw new ContentAddressableStorageServerException("Error when trying to download object", e);
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to download object %s from container %s. Reason: errorMessage=%s",
                    objectName,
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to download object", e);
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
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, objectName);
        try {
            client.deleteObject(deleteObjectRequest);
        } catch (AmazonServiceException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to delete object %s from container %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                    objectName,
                    containerName,
                    e.getErrorCode(),
                    e.getErrorType(),
                    e.getErrorMessage()
                ),
                e
            );
            if (AmazonS3APIErrorCodes.NO_SUCH_BUCKET.getErrorCode().equals(e.getErrorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName,
                    e
                );
            } else {
                throw new ContentAddressableStorageServerException("Error when trying to delete object", e);
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to delete object %s from container %s. Reason: errorMessage=%s",
                    objectName,
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to delete object " + objectName, e);
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Check existance of object %s in container %s", objectName, containerName));
        String bucketName = generateBucketName(containerName);
        try {
            return client.doesObjectExist(bucketName, objectName);
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to check existance of object %s in container %s. Reason: errorMessage=%s",
                    objectName,
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to check existance of object", e);
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
                GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(
                    bucketName,
                    objectName
                );
                ObjectMetadata objectMetadata = client.getObjectMetadata(getObjectMetadataRequest);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_Offer_" + getConfiguration().getProvider(),
                        containerName,
                        "READ_DIGEST_FROM_METADATA",
                        stopwatch.elapsed(TimeUnit.MILLISECONDS)
                    );
                return getDigestFromObjectMetadata(containerName, objectName, digestType, bucketName, objectMetadata);
            } catch (AmazonServiceException e) {
                LOGGER.debug(
                    String.format(
                        "Error when trying to compute digest of object %s from container %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                        objectName,
                        containerName,
                        e.getErrorCode(),
                        e.getErrorType(),
                        e.getErrorMessage()
                    ),
                    e
                );
                if (AmazonS3APIErrorCodes.NOT_FOUND.getErrorCode().equals(e.getErrorCode())) {
                    throw new ContentAddressableStorageNotFoundException(
                        ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName,
                        e
                    );
                } else {
                    throw new ContentAddressableStorageServerException(
                        "Error when trying to compute digest of object",
                        e
                    );
                }
            } catch (SdkBaseException e) {
                LOGGER.debug(
                    String.format(
                        "Error when trying to compute digest of object %s from container %s. Reason: errorMessage=%s",
                        objectName,
                        containerName,
                        e.getMessage()
                    ),
                    e
                );
                throw new ContentAddressableStorageServerException("Error when trying to compute digest of object", e);
            }
        }

        return computeObjectDigest(containerName, objectName, digestType);
    }

    private String getDigestFromObjectMetadata(
        String containerName,
        String objectName,
        DigestType digestType,
        String bucketName,
        ObjectMetadata objectMetadata
    ) throws ContentAddressableStorageException {
        if (
            null != objectMetadata &&
            objectMetadata.getUserMetadata().containsKey(X_OBJECT_META_DIGEST) &&
            objectMetadata.getUserMetadata().containsKey(X_OBJECT_META_DIGEST_TYPE) &&
            digestType.getName().equals(objectMetadata.getUserMetadata().get(X_OBJECT_META_DIGEST_TYPE)) &&
            null != objectMetadata.getUserMetadata().get(X_OBJECT_META_DIGEST)
        ) {
            return objectMetadata.getUserMetadata().get(X_OBJECT_META_DIGEST);
        } else {
            LOGGER.warn(
                String.format(
                    "Could not retrieve cached digest of object '%s' in container '%s'. Recomputing digest",
                    objectName,
                    containerName
                )
            );
            String digestToStore = computeObjectDigest(containerName, objectName, digestType);
            storeDigest(containerName, objectName, digestType, digestToStore, bucketName);
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
        LOGGER.debug(String.format("Get metadatas of object %s in container %s", objectId, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectId
        );
        String bucketName = generateBucketName(containerName);
        try {
            MetadatasStorageObject result = new MetadatasStorageObject();
            GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(bucketName, objectId);
            ObjectMetadata objectMetadata = client.getObjectMetadata(getObjectMetadataRequest);
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
                        objectMetadata
                    )
            );
            result.setFileSize(objectMetadata.getContentLength());
            result.setLastModifiedDate(objectMetadata.getLastModified().toString());
            return result;
        } catch (AmazonServiceException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to get metadatas of object %s in container %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                    objectId,
                    containerName,
                    e.getErrorCode(),
                    e.getErrorType(),
                    e.getErrorMessage()
                ),
                e
            );
            if (AmazonS3APIErrorCodes.NOT_FOUND.getErrorCode().equals(e.getErrorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectId,
                    e
                );
            } else {
                throw new ContentAddressableStorageServerException("Error when trying to get metadatas of object", e);
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to get metadatas of object %s in container %s. Reason: errorMessage=%s",
                    objectId,
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to get metadatas of object", e);
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
                ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
                listObjectsV2Request.setBucketName(bucketName);
                listObjectsV2Request.setMaxKeys(LISTING_MAX_RESULTS);
                listObjectsV2Request.setContinuationToken(continuationToken);

                ListObjectsV2Result listObjectsV2Result = client.listObjectsV2(listObjectsV2Request);

                for (S3ObjectSummary objectSummary : listObjectsV2Result.getObjectSummaries()) {
                    objectListingListener.handleObjectEntry(
                        new ObjectEntry(objectSummary.getKey(), objectSummary.getSize())
                    );
                }

                continuationToken = listObjectsV2Result.getNextContinuationToken();
            } while (continuationToken != null);
        } catch (AmazonServiceException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to list objects from container %s. Reason: errorCode=%s, errorType=%s, errorMessage=%s",
                    containerName,
                    e.getErrorCode(),
                    e.getErrorType(),
                    e.getErrorMessage()
                ),
                e
            );
            if (AmazonS3APIErrorCodes.NO_SUCH_BUCKET.getErrorCode().equals(e.getErrorCode())) {
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName,
                    e
                );
            } else {
                throw new ContentAddressableStorageServerException("Error when trying to list objects", e);
            }
        } catch (SdkBaseException e) {
            LOGGER.debug(
                String.format(
                    "Error when trying to list objects from container %s. Reason: errorMessage=%s",
                    containerName,
                    e.getMessage()
                ),
                e
            );
            throw new ContentAddressableStorageServerException("Error when trying to list objects", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

    /**
     * Generate a valid bucket name from the container name : replace non
     * alphanumeric values by '.', lowercase every alphabetic value and remove '.
     * 'at start and end.
     *
     * @param containerName vitam container name
     * @return bucket name valid according to s3 API specification
     * @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html">Bucket restrictions and limitations</a>
     */
    public String generateBucketName(String containerName) {
        String bucketName = containerName.replaceAll("[^A-Za-z0-9]", ".").toLowerCase();
        bucketName = StringUtils.strip(bucketName, ".");
        LOGGER.debug(String.format("Generated bucket name %s from container name %s", bucketName, containerName));
        BucketNameUtils.validateBucketName(bucketName);
        return bucketName;
    }
}
