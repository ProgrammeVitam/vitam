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
package fr.gouv.vitam.storage.offers.tape.utils;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.guid.GUIDFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalFileUtils {

    private static final String CONTAINER_SEPARATOR = "/";
    private static final String SEPARATOR = "-";
    private static final Pattern ARCHIVE_ID_FILENAME_PATTERN = Pattern.compile(
        "^(?<CreationDate>\\d{17})-(?<GUID>[a-z0-9\\-]*)\\.(tar|zip)$"
    );

    private static final int GUID_LENGTH = 36;
    public static final String TAR_EXTENSION = ".tar";
    public static final String ZIP_EXTENSION = ".zip";
    public static final String TMP_EXTENSION = ".tmp";
    public static final String INPUT_TAR_TMP_FOLDER = "tmp";

    private LocalFileUtils() {
        throw new IllegalStateException("No constructor for helper class");
    }

    public static String createStorageId(String objectName) {
        return objectName + SEPARATOR + GUIDFactory.newGUID();
    }

    public static String storageIdToObjectName(String storageId) {
        if (storageId.length() <= SEPARATOR.length() + GUID_LENGTH) {
            throw new IllegalArgumentException("Invalid storage id " + storageId);
        }
        return storageId.substring(0, storageId.length() - SEPARATOR.length() - GUID_LENGTH);
    }

    public static String createTarEntryName(String containerName, String storageId, int entryIndex) {
        return containerName + CONTAINER_SEPARATOR + storageId + SEPARATOR + entryIndex;
    }

    public static String getContainerNameFromTarEntryName(String tarEntryName) {
        int containerSeparatorIndex = tarEntryName.indexOf(CONTAINER_SEPARATOR);
        if (containerSeparatorIndex <= 0) {
            throw new IllegalArgumentException("Invalid container name '" + tarEntryName + "'");
        }
        return tarEntryName.substring(0, containerSeparatorIndex);
    }

    public static String getStorageIdFromTarEntryName(String tarEntryName) {
        int containerSeparatorIndex = tarEntryName.indexOf(CONTAINER_SEPARATOR);
        int entryIndexSeparatorIndex = tarEntryName.lastIndexOf(SEPARATOR);

        if (containerSeparatorIndex <= 0) {
            throw new IllegalArgumentException("Invalid tar entry name '" + tarEntryName + "'");
        }
        if (entryIndexSeparatorIndex <= containerSeparatorIndex) {
            throw new IllegalArgumentException("Invalid tar entry name '" + tarEntryName + "'");
        }
        return tarEntryName.substring(containerSeparatorIndex + 1, entryIndexSeparatorIndex);
    }

    public static int getEntryIndexFromTarEntryName(String tarEntryName) {
        int entryIndexSeparatorIndex = tarEntryName.lastIndexOf(SEPARATOR);
        if (entryIndexSeparatorIndex <= 0) {
            throw new IllegalArgumentException("Invalid tar entry name '" + tarEntryName + "'");
        }
        return Integer.parseInt(tarEntryName.substring(entryIndexSeparatorIndex + 1));
    }

    public static String createTarId(LocalDateTime now) {
        return (
            LocalDateUtil.getDateTimeFormatterForFileNames().format(now) + SEPARATOR + UUID.randomUUID() + TAR_EXTENSION
        );
    }

    public static String archiveFileNameRelativeToInputArchiveStorageFolder(String fileBucket, String archiveId) {
        return fileBucket + "/" + archiveId;
    }

    public static String getCreationDateFromArchiveId(String archiveId) {
        Matcher matcher = ARCHIVE_ID_FILENAME_PATTERN.matcher(archiveId);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid archiveId '" + archiveId + "'");
        }
        return matcher.group("CreationDate");
    }

    public static String archiveFileNamePathToArchiveId(String tarFileName) {
        if (tarFileName.endsWith(TMP_EXTENSION)) {
            tarFileName = tarFileName.substring(0, tarFileName.length() - TMP_EXTENSION.length());
        }

        if (!tarFileName.endsWith(TAR_EXTENSION) && !tarFileName.endsWith(ZIP_EXTENSION)) {
            throw new IllegalArgumentException("Invalid archive file name " + tarFileName);
        }
        return tarFileName;
    }

    public static Path fileBuckedInputFilePath(String inputTarStorageFolder, String fileBucketId) {
        return Paths.get(inputTarStorageFolder).resolve(fileBucketId);
    }
}
