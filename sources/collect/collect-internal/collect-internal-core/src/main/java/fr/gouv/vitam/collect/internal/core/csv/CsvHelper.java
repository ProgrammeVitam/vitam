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

package fr.gouv.vitam.collect.internal.core.csv;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FILE_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.OBJECT_FIlES_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.PREFIX_ID_HEADER;

public class CsvHelper {

    private static final int MAX_PATH_LENGTH = 255;

    private CsvHelper() {}

    public static void convertCsvToJsonlMetadataFile(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        InputStream is,
        File metadataFile,
        boolean isFirstUpload,
        boolean explicitAttachementMode
    ) throws IOException, CollectInvalidCsvFormatException {
        try (
            CSVParser parser = createParser(is);
            JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(metadataFile, true), true)
        ) {
            final List<String> headerNames = parser.getHeaderNames();
            CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(
                sedaSchemaInfoResolver,
                headerNames,
                isFirstUpload
            );

            try (CsvErrorAccumulator csvErrorAccumulator = new CsvErrorAccumulator()) {
                for (CSVRecord record : parser) {
                    Optional<CollectJsonMetadataLine> collectJsonMetadataLine = processRecord(
                        record,
                        headerNames,
                        isFirstUpload,
                        explicitAttachementMode,
                        csvErrorAccumulator,
                        parser,
                        csvToJsonConverter
                    );
                    if (collectJsonMetadataLine.isPresent()) {
                        writer.addEntry(collectJsonMetadataLine.get());
                    }
                }
            }
        }
    }

    private static Optional<CollectJsonMetadataLine> processRecord(
        CSVRecord record,
        List<String> headerNames,
        boolean isFirstUpload,
        boolean explicitAttachementMode,
        CsvErrorAccumulator csvErrorAccumulator,
        CSVParser parser,
        CsvToJsonConverter csvToJsonConverter
    ) throws CollectInvalidCsvFormatException {
        long csvRecordNumberIncludingHeader = record.getRecordNumber() + 1;

        // Validate record columns
        if (hasInconsistentRecordColumns(record, headerNames, csvErrorAccumulator, csvRecordNumberIncludingHeader)) {
            return Optional.empty();
        }

        String uploadPath = null;
        String id = null;
        String valueToCheck;
        String header = FILE_HEADER;
        if (headerNames.contains(FILE_HEADER)) {
            uploadPath = FilenameUtils.separatorsToUnix(record.get(FILE_HEADER));
            valueToCheck = uploadPath;
        } else {
            id = record.get(PREFIX_ID_HEADER);
            valueToCheck = id;
            header = PREFIX_ID_HEADER;
        }

        if (
            hasMissingUploadPath(csvErrorAccumulator, valueToCheck, csvRecordNumberIncludingHeader, header) ||
            (uploadPath != null &&
                hasIllegalUploadPath(csvErrorAccumulator, uploadPath, csvRecordNumberIncludingHeader))
        ) {
            return Optional.empty();
        }

        String objectFilesPath = parser.getHeaderMap().containsKey(OBJECT_FIlES_HEADER) &&
            StringUtils.isNotEmpty(record.get(OBJECT_FIlES_HEADER))
            ? FilenameUtils.separatorsToUnix(record.get(OBJECT_FIlES_HEADER))
            : null;

        if (objectFilesPath != null) {
            if (
                isForbiddenObjectFilesForUpdateMode(
                    isFirstUpload,
                    csvErrorAccumulator,
                    csvRecordNumberIncludingHeader,
                    valueToCheck,
                    header
                )
            ) {
                return Optional.empty();
            }

            if (
                hasIllegalObjectFilesField(
                    csvErrorAccumulator,
                    objectFilesPath,
                    csvRecordNumberIncludingHeader,
                    valueToCheck,
                    header
                )
            ) {
                return Optional.empty();
            }
        }

        return tryConvertCsvToJsonl(
            record,
            csvErrorAccumulator,
            csvToJsonConverter,
            uploadPath,
            id,
            objectFilesPath,
            explicitAttachementMode,
            csvRecordNumberIncludingHeader
        );
    }

    private static boolean hasInconsistentRecordColumns(
        CSVRecord record,
        List<String> headerNames,
        CsvErrorAccumulator csvErrorAccumulator,
        long csvRecordNumberIncludingHeader
    ) throws CollectInvalidCsvFormatException {
        if (record.isConsistent()) {
            return false;
        }
        csvErrorAccumulator.report(
            "Invalid CSV record at line %d: Nb columns (%d) must match nb headers (%d)".formatted(
                    csvRecordNumberIncludingHeader,
                    record.size(),
                    headerNames.size()
                )
        );
        return true;
    }

    private static boolean hasMissingUploadPath(
        CsvErrorAccumulator csvErrorAccumulator,
        String uploadPath,
        long csvRecordNumberIncludingHeader,
        String header
    ) throws CollectInvalidCsvFormatException {
        if (StringUtils.isNotBlank(uploadPath)) {
            return false;
        }
        csvErrorAccumulator.report(
            "Invalid CSV record at line " + csvRecordNumberIncludingHeader + ": Empty " + header
        );
        return true;
    }

    private static boolean hasIllegalUploadPath(
        CsvErrorAccumulator csvErrorAccumulator,
        String uploadPath,
        long csvRecordNumberIncludingHeader
    ) throws CollectInvalidCsvFormatException {
        String normalizedUploadPath = FilenameUtils.normalize(uploadPath);
        if (FilenameUtils.equals(uploadPath, normalizedUploadPath)) {
            return false;
        }
        csvErrorAccumulator.report(
            "Invalid CSV record at line %d: Illegal '%s' value '%s'".formatted(
                    csvRecordNumberIncludingHeader,
                    FILE_HEADER,
                    sanitizeStringForLog(uploadPath, MAX_PATH_LENGTH)
                )
        );
        return true;
    }

    private static boolean isForbiddenObjectFilesForUpdateMode(
        boolean isFirstUpload,
        CsvErrorAccumulator csvErrorAccumulator,
        long csvRecordNumberIncludingHeader,
        String uploadPathOrGuid,
        String header
    ) throws CollectInvalidCsvFormatException {
        if (isFirstUpload) {
            return false;
        }
        csvErrorAccumulator.report(
            String.format(
                "Invalid CSV record at line %d (" + header + "=\"%s\"): %s field not supported for update operations",
                csvRecordNumberIncludingHeader,
                sanitizeStringForLog(uploadPathOrGuid, MAX_PATH_LENGTH),
                OBJECT_FIlES_HEADER
            )
        );
        return true;
    }

    private static boolean hasIllegalObjectFilesField(
        CsvErrorAccumulator csvErrorAccumulator,
        String objectFilesPath,
        long csvRecordNumberIncludingHeader,
        String uploadPathOrGuid,
        String header
    ) throws CollectInvalidCsvFormatException {
        String normalizedObjectFilesPath = FilenameUtils.normalize(objectFilesPath);
        if (FilenameUtils.equals(objectFilesPath, normalizedObjectFilesPath)) {
            return false;
        }

        csvErrorAccumulator.report(
            String.format(
                "Invalid CSV record at line %d (" + header + "=\"%s\"): %s",
                csvRecordNumberIncludingHeader,
                sanitizeStringForLog(uploadPathOrGuid, MAX_PATH_LENGTH),
                "Invalid '" +
                OBJECT_FIlES_HEADER +
                "' value '" +
                sanitizeStringForLog(objectFilesPath, MAX_PATH_LENGTH) +
                "'"
            )
        );
        return true;
    }

    private static Optional<CollectJsonMetadataLine> tryConvertCsvToJsonl(
        CSVRecord record,
        CsvErrorAccumulator csvErrorAccumulator,
        CsvToJsonConverter csvToJsonConverter,
        String uploadPath,
        String id,
        String objectFilesPath,
        boolean explicitAttachementMode,
        long csvRecordNumberIncludingHeader
    ) throws CollectInvalidCsvFormatException {
        try {
            boolean isTopLevelFolder = uploadPath != null && !uploadPath.contains(File.separator);
            ObjectNode unitJson = csvToJsonConverter.convertCsvRecordToJson(
                record,
                isTopLevelFolder,
                explicitAttachementMode
            );
            return Optional.of(new CollectJsonMetadataLine(uploadPath, id, objectFilesPath, null, unitJson));
        } catch (CollectInvalidCsvFormatException e) {
            csvErrorAccumulator.report(
                String.format(
                    "Invalid CSV record at line %d (File=\"%s\"): %s",
                    csvRecordNumberIncludingHeader,
                    sanitizeStringForLog(uploadPath, MAX_PATH_LENGTH),
                    e.getMessage()
                )
            );
            return Optional.empty();
        }
    }

    public static CSVParser createParser(InputStream is) throws IOException {
        final InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        return new CSVParser(
            reader,
            CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setTrim(true)
                .setIgnoreEmptyLines(false)
                .setDelimiter(';')
                .build()
        );
    }

    public static String sanitizeStringForLog(String uploadPath, int maxLength) {
        return StringUtils.abbreviate(StringEscapeUtils.escapeJava(uploadPath), maxLength);
    }
}
