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

/**
 * @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">General purpose bucket naming rules</a>
 */
class BucketNameUtils {

    private static final int MIN_BUCKET_NAME_LENGTH = 3;
    private static final int MAX_BUCKET_NAME_LENGTH = 63;

    public static void validateBucketName(String bucketName) {
        if (bucketName == null) {
            throw new IllegalArgumentException("Bucket name cannot be null");
        }

        if (bucketName.length() < MIN_BUCKET_NAME_LENGTH || bucketName.length() > MAX_BUCKET_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Bucket name must be between " +
                MIN_BUCKET_NAME_LENGTH +
                " and " +
                MAX_BUCKET_NAME_LENGTH +
                " characters long: " +
                bucketName
            );
        }

        if (!bucketName.matches("^[a-z0-9.]+$")) {
            throw new IllegalArgumentException(
                "Bucket name can only contain lowercase letters, numbers, and periods: " + bucketName
            );
        }

        if (!bucketName.matches("^[a-z0-9].*")) {
            throw new IllegalArgumentException(
                "Bucket name must start with a lowercase letter or number: " + bucketName
            );
        }

        if (bucketName.contains("..")) {
            throw new IllegalArgumentException("Bucket name cannot contain two adjacent periods: " + bucketName);
        }

        if (bucketName.endsWith(".")) {
            throw new IllegalArgumentException("Bucket name cannot end with a period: " + bucketName);
        }

        if (bucketName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            throw new IllegalArgumentException("Bucket name cannot be formatted as an IP address: " + bucketName);
        }
    }
}
