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
package fr.gouv.vitam.common.storage.constants;

/**
 * Storage offers provider
 */
public enum StorageProvider {
    /**
     * File system storage offer
     */
    FILESYSTEM("filesystem", true),
    /**
     * Swift storage offer (ceph or openStack)
     * authent v1
     * DO NOT CHANGE THE VALUE OF THIS VAR AS JCLOUDS IS EXPECTING THIS EXACT VALUE
     */
    SWIFT_AUTH_V1("openstack-swift", false),
    /**
     * Swift storage offer
     * authent v2
     */
    SWIFT_AUTH_V2("openstack-swift-v2", false),
    /**
     * Swift storage offer
     * authent v3
     */
    SWIFT_AUTH_V3("openstack-swift-v3", false),
    /**
     * S3 storage offer
     * Amazon SDK S3 v1
     */
    AMAZON_S3_V1("amazon-s3-v1", false),
    /**
     * File system storage offer with a hashed directory structure
     */
    HASHFILESYSTEM("filesystem-hash", true),
    /**
     * Tape library offer
     */
    TAPE_LIBRARY("tape-library", false);

    private String value;
    private boolean hasStoragePath;

    StorageProvider(String value, boolean hasStoragePath) {
        this.value = value;
        this.hasStoragePath = hasStoragePath;
    }

    public static StorageProvider getStorageProvider(String storageProvider) {
        for (StorageProvider provider : values()) {
            if (provider.getValue().equalsIgnoreCase(storageProvider)) {
                return provider;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    public boolean hasStoragePath() {
        return this.hasStoragePath;
    }
}
