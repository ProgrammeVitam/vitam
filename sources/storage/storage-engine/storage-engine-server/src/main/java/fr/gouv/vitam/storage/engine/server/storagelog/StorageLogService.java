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

package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.AccessLogParameters;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogStructure;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageLogService implements StorageLog {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogService.class);

    private static final String FILENAME_PATTERN_CREATION_DATE_GROUP = "CreationDate";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^\\d+_(?<CreationDate>\\d+)_.*$");
    public static final String WRITE_LOG_DIR = "storage-log";
    public static final String ACCESS_LOG_DIR = "access-log";
    private static final String WRITE_LOG_BACKUP_FILENAME = "storage_logbook";
    private static final String ACCESS_LOG_BACKUP_FILENAME = "storage_access_logbook";

    private static final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";

    private final List<Integer> tenants;
    private final Path writeOperationLogPath;
    private final Path accessOperationLogPath;
    private final Map<Integer, StorageLogAppender> writeOperationLogAppenders;
    private final Map<Integer, StorageLogAppender> accessOperationLogAppenders;
    private final Map<Integer, Object> writeLockers;
    private final Map<Integer, Object> accessLockers;

    /**
     * Constructor.
     *
     * @param tenants
     * @param basePath
     * @throws IOException
     */
    public StorageLogService(List<Integer> tenants, Path basePath) throws IOException {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, tenants, basePath);
        this.tenants = tenants;

        this.writeOperationLogPath = createStoragePathDirectory(basePath, true);
        this.accessOperationLogPath = createStoragePathDirectory(basePath, false);

        this.writeOperationLogAppenders = new HashMap<>();
        this.accessOperationLogAppenders = new HashMap<>();
        this.writeLockers = new HashMap<>();
        this.accessLockers = new HashMap<>();
        initializeStorageLogs();
    }

    /**
     * Creates storage directory if not exists.
     *
     * @param basePath
     * @return the storage directory path
     * @throws IOException thrown on IO error
     */
    private Path createStoragePathDirectory(Path basePath, Boolean isWriteOperation) throws IOException {
        Path storageLogPath;
        if (isWriteOperation) {
            storageLogPath = basePath.resolve(WRITE_LOG_DIR);
        } else {
            storageLogPath = basePath.resolve(ACCESS_LOG_DIR);
        }
        if (!Files.exists(storageLogPath)) {
            Files.createDirectories(storageLogPath);
        }

        checkExistingStorageLogFiles(storageLogPath);

        return storageLogPath;
    }

    /**
     * Check if storage log file is empty. Otherwise log warning
     *
     * @param storageLogPath
     * @throws IOException thrown on IO error
     */
    private void checkExistingStorageLogFiles(Path storageLogPath) throws IOException {
        try (Stream<Path> list = Files.list(storageLogPath)) {
            List<Path> exitingFiles = list.collect(Collectors.toList());
            if (!exitingFiles.isEmpty()) {
                LOGGER.warn("Existing storage log files found: " + exitingFiles.toString());
            }
        }
    }

    /**
     * Get the name of the new file
     *
     * @param tenant
     * @return
     */
    private StorageLogAppender createAppender(Integer tenant, Boolean isWriteOperation) throws IOException {
        String file_name =
            tenant.toString() +
            "_" +
            LocalDateUtil.now().format(LocalDateUtil.getDateTimeFormatterForFileNames()) +
            "_" +
            UUID.randomUUID() +
            ".log";
        Path appenderPath;
        if (isWriteOperation) {
            appenderPath = this.writeOperationLogPath.resolve(file_name);
        } else {
            appenderPath = this.accessOperationLogPath.resolve(file_name);
        }
        return new StorageLogAppender(appenderPath);
    }

    @Override
    public void appendWriteLog(Integer tenant, StorageLogbookParameters parameters) throws IOException {
        append(tenant, parameters, true);
    }

    @Override
    public void appendAccessLog(Integer tenant, AccessLogParameters parameters) throws IOException {
        append(tenant, parameters, false);
    }

    private void append(Integer tenant, StorageLogStructure parameters, Boolean isWriteOperation) throws IOException {
        if (isWriteOperation) {
            synchronized (writeLockers.get(tenant)) {
                writeOperationLogAppenders.get(tenant).append(parameters);
            }
        } else {
            synchronized (accessLockers.get(tenant)) {
                accessOperationLogAppenders.get(tenant).append(parameters);
            }
        }
    }

    @Override
    public List<LogInformation> rotateLogFile(Integer tenant, boolean isWriteOperation) throws IOException {
        if (isWriteOperation) {
            synchronized (writeLockers.get(tenant)) {
                writeOperationLogAppenders.get(tenant).close();
                List<LogInformation> storageLogToBackup = listStorageLogsToBackup(tenant, isWriteOperation);
                writeOperationLogAppenders.put(tenant, createAppender(tenant, isWriteOperation));
                return storageLogToBackup;
            }
        } else {
            synchronized (accessLockers.get(tenant)) {
                accessOperationLogAppenders.get(tenant).close();
                List<LogInformation> storageLogToBackup = listStorageLogsToBackup(tenant, isWriteOperation);
                accessOperationLogAppenders.put(tenant, createAppender(tenant, isWriteOperation));
                return storageLogToBackup;
            }
        }
    }

    public void initializeStorageLogs() throws IOException {
        for (Integer tenant : tenants) {
            writeOperationLogAppenders.put(tenant, createAppender(tenant, true));
            accessOperationLogAppenders.put(tenant, createAppender(tenant, false));
        }
        for (Integer tenant : tenants) {
            this.writeLockers.put(tenant, new Object());
            this.accessLockers.put(tenant, new Object());
        }
    }

    private List<LogInformation> listStorageLogsToBackup(Integer tenant, Boolean isWriteOperation) throws IOException {
        // List storage log files by tenant
        LocalDateTime now = LocalDateUtil.now();

        List<LogInformation> previousLogFiles = new ArrayList<>();
        Path path;
        if (isWriteOperation) {
            path = writeOperationLogPath;
        } else {
            path = accessOperationLogPath;
        }
        if (Files.exists(path)) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(path, tenant + "_*.log")) {
                for (Path filePath : paths) {
                    String filename = filePath.getFileName().toString();
                    Optional<LocalDateTime> localDateTime = tryParseCreationDateFromFileName(filename);
                    if (!localDateTime.isPresent()) {
                        LOGGER.warn("Invalid storage log filename '" + filename + "'");
                    } else {
                        previousLogFiles.add(new LogInformation(filePath, localDateTime.get(), now));
                    }
                }
            }
        }
        return previousLogFiles;
    }

    private Optional<LocalDateTime> tryParseCreationDateFromFileName(String filename) {
        Matcher matcher = FILENAME_PATTERN.matcher(filename);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String creationDateStr = matcher.group(FILENAME_PATTERN_CREATION_DATE_GROUP);

        try {
            LocalDateTime creationDate = LocalDateUtil.parse(
                creationDateStr,
                LocalDateUtil.getDateTimeFormatterForFileNames()
            );
            return Optional.of(creationDate);
        } catch (RuntimeException ex) {
            LOGGER.warn("Invalid creation date in storage log filename '" + filename + "'", ex);
            return Optional.empty();
        }
    }

    @Override
    public String getFileName(boolean isWriteOperation) {
        if (isWriteOperation) {
            return WRITE_LOG_BACKUP_FILENAME;
        } else {
            return ACCESS_LOG_BACKUP_FILENAME;
        }
    }

    @Override
    public void close() {
        for (Integer tenant : this.tenants) {
            synchronized (writeLockers.get(tenant)) {
                writeOperationLogAppenders.get(tenant).close();
            }
            synchronized (accessLockers.get(tenant)) {
                accessOperationLogAppenders.get(tenant).close();
            }
        }
    }
}
