/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.engine.server.storagelog;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;

public class StorageLogFactory implements StorageLog {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogFactory.class);

    private static final String FILENAME_PATTERN_CREATION_DATE_GROUP = "CreationDate";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^\\d+_(?<CreationDate>\\d+)_.*$");
    public static final String STORAGE_LOG_DIR = "storage-log";
    private static final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";

    private static StorageLog instance;
    private final List<Integer> tenants;
    private Path storageLogPath;
    private final Map<Integer, StorageLogAppender> storageLogAppenders;
    private final Map<Integer, Object> lockers;

    /**
     * Constructor.
     *
     * @param tenants
     * @param basePath
     * @throws IOException
     */
    private StorageLogFactory(List<Integer> tenants, Path basePath) throws IOException {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, tenants, basePath);
        this.tenants = tenants;
        this.storageLogAppenders = new HashMap<>();
        this.lockers = new HashMap<>();
        initializeStorageLogs(basePath);
    }

    /**
     * get Thread-Safe instance instance. <br/>
     *
     * @return the instance.
     */
    public static synchronized StorageLog getInstance(List<Integer> tenants, Path basePath) throws IOException {
        if (instance == null) {
            instance = new StorageLogFactory(tenants, basePath);
        }
        return instance;
    }

    /**
     * Creates storage directory if not exists.
     *
     * @param basePath
     * @return the storage directory path
     * @throws IOException thrown on IO error
     */
    private Path createStoragePathDirectory(Path basePath) throws IOException {

        Path storageLogPath = basePath.resolve(STORAGE_LOG_DIR);
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
    private StorageLogAppender createAppender(Integer tenant) throws IOException {
        LocalDateTime date = LocalDateUtil.now();
        DateTimeFormatter formatter = getDateTimeFormatter();
        String file_name =
            tenant.toString() + "_" + date.format(formatter) + "_" + UUID.randomUUID().toString() + ".log";
        Path appenderPath = this.storageLogPath.resolve(file_name);
        return new StorageLogAppender(appenderPath);
    }

    private DateTimeFormatter getDateTimeFormatter() {
        // Cannot use yyyyMMddHHmmssSSS due to Java 8 bug https://bugs.java.com/view_bug.do?bug_id=8031085
        return new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .appendValue(ChronoField.MILLI_OF_SECOND, 3)
            .toFormatter()
            .withZone(ZoneOffset.UTC);
    }

    @Override
    public void append(Integer tenant, StorageLogbookParameters parameters) throws IOException {
        Object lock = lockers.get(tenant);
        synchronized (lock) {
            storageLogAppenders.get(tenant).append(parameters);
        }
    }

    @Override
    public List<LogInformation> rotateLogFile(Integer tenant) throws IOException {

        Object lock = lockers.get(tenant);
        synchronized (lock) {
            storageLogAppenders.get(tenant).close();
            List<LogInformation> storageLogToBackup = listStorageLogsToBackup(tenant);
            storageLogAppenders.put(tenant, createAppender(tenant));

            return storageLogToBackup;
        }
    }

    @Override
    public void initializeStorageLogs(Path basePath) throws IOException {
        storageLogPath = createStoragePathDirectory(basePath);

            for (Integer tenant : tenants) {
                storageLogAppenders.put(tenant, createAppender(tenant));
            }
            for (Integer tenant : tenants) {
                this.lockers.put(tenant, new Object());
            }
    }

    private List<LogInformation> listStorageLogsToBackup(Integer tenant) throws IOException {
        // List storage log files by tenant
        LocalDateTime now = LocalDateUtil.now();

        List<LogInformation> previousLogFiles = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(this.storageLogPath, tenant + "_*.log")) {
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

        return previousLogFiles;
    }

    private Optional<LocalDateTime> tryParseCreationDateFromFileName(String filename) {
        Matcher matcher = FILENAME_PATTERN.matcher(filename);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String creationDateStr = matcher.group(FILENAME_PATTERN_CREATION_DATE_GROUP);

        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter();
        try {
            LocalDateTime creationDate = LocalDateTime.parse(creationDateStr, dateTimeFormatter);
            return Optional.of(creationDate);
        } catch (RuntimeException ex) {
            LOGGER.warn("Invalid creation date in storage log filename '" + filename + "'", ex);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        for (Integer tenant : this.tenants) {
            Object lock = lockers.get(tenant);
            synchronized (lock) {
                storageLogAppenders.get(tenant).close();
            }
        }
    }
}
