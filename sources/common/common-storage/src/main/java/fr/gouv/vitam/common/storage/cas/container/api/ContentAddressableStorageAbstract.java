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
package fr.gouv.vitam.common.storage.cas.container.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class of CAS that contains common methos
 */
public abstract class ContentAddressableStorageAbstract implements ContentAddressableStorage {

    private static boolean disableContainerCaching = false;

    private final Set<String> existingContainer = Collections.synchronizedSet(new HashSet<>());

    private final StorageConfiguration configuration;

    /**
     * Max result for listing option TODO: have to be configurable ?
     */
    public static final int LISTING_MAX_RESULTS = 100;

    protected ContentAddressableStorageAbstract(StorageConfiguration configuration) {
        this.configuration = configuration;
    }

    public StorageConfiguration getConfiguration() {
        return configuration;
    }

    protected String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(), algo);

        Stopwatch sw = Stopwatch.createStarted();
        try (InputStream stream = getObject(containerName, objectName).getInputStream()) {
            final Digest digest = new Digest(algo);
            digest.update(stream);
            return digest.toString();
        } catch (final IOException e) {
            throw new ContentAddressableStorageException(e);
        } finally {
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + configuration.getProvider(),
                    containerName,
                    "COMPUTE_DIGEST_FROM_STREAM",
                    sw.elapsed(TimeUnit.MILLISECONDS)
                );
        }
    }

    /**
     * Determines if a container exists in cache
     *
     * @param containerName name of container
     * @return boolean type
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    protected boolean isExistingContainerInCache(String containerName) {
        // If existing containers are already checked, this help just an in memory check
        return existingContainer.contains(containerName);
    }

    /**
     * This handle cache already existing container
     * Prevent handling an i/o check container exists
     * Do only memory check if the container is already exists
     *
     * @param containerName
     * @param exists
     */
    protected void cacheExistsContainer(String containerName, boolean exists) {
        if (exists && !disableContainerCaching) {
            existingContainer.add(containerName);
        }
    }

    @VisibleForTesting
    public static void disableContainerCaching() {
        disableContainerCaching = true;
    }
}
