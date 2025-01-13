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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FILE_HEADER;

public class CsvHelper {

    private CsvHelper() {}

    public static void convertCsvToJsonlMetadataFile(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        InputStream is,
        File metadataFile
    ) throws IOException, CollectInvalidCsvFormatException {
        try (
            CSVParser parser = createParser(is);
            JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(metadataFile, true), true)
        ) {
            final List<String> headerNames = parser.getHeaderNames();
            CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);

            for (CSVRecord record : parser) {
                ObjectNode unitJson = csvToJsonConverter.convertCsvRecordToJson(record);
                String uploadPath = FilenameUtils.separatorsToUnix(record.get(FILE_HEADER));
                writer.addEntry(new CollectJsonMetadataLine().setFile(uploadPath).setUnitContent(unitJson));
            }
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
}
