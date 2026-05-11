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

package fr.gouv.vitam.common;

import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public final class TraceabilityITUtils {

    public static void downloadZips(
        File rootFolder,
        String securisationVersion,
        LogbookOperation... traceabilityOperations
    ) throws Exception {
        for (LogbookOperation traceabilityOperation : traceabilityOperations) {
            TraceabilityEvent traceabilityEvent = getTraceabilityEvent(traceabilityOperation);
            File dedicatedFolder = new File(rootFolder, traceabilityEvent.getFileName());
            Files.createDirectories(dedicatedFolder.toPath());
            downloadZip(traceabilityEvent.getFileName(), dedicatedFolder);

            verifyTraceabilityZip(rootFolder, traceabilityEvent, securisationVersion);
        }
    }

    public static void downloadZip(String fileName, File folder)
        throws IOException, StorageNotFoundException, StorageServerClientException, StorageUnavailableDataFromAsyncOfferClientException {
        try (
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response containerAsync = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                DataCategory.LOGBOOK,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = containerAsync.readEntity(InputStream.class);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(new File(folder, entry.getName()))) {
                    IOUtils.copy(zipInputStream, fileOutputStream);
                }
            }
        }
    }

    public static void verifyChaining(
        File rootDirectory,
        LogbookOperation logbookOperation,
        LogbookOperation previousOperation,
        LogbookOperation previousMonthOperation,
        LogbookOperation previousYearOperation
    ) throws InvalidParseOperationException, IOException {
        TraceabilityEvent currentEvent = getTraceabilityEvent(logbookOperation);

        // Use already unzipped folder for current event
        File currentEventFolder = new File(rootDirectory, currentEvent.getFileName());

        File computingInfoFile = new File(currentEventFolder, "computing_information.txt");
        Properties computingInfoProperties = new Properties();
        computingInfoProperties.load(new FileInputStream(computingInfoFile));

        String tokenContentFromZip = getTokenContentFromZip(rootDirectory, currentEvent);
        assertThat(BaseXx.getBase64(currentEvent.getTimeStampToken())).isEqualTo(tokenContentFromZip);

        if (previousOperation == null) {
            assertThat(currentEvent.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        } else {
            assertThat(currentEvent.getStartDate()).isEqualTo(getTraceabilityEvent(previousOperation).getEndDate());
        }
        assertThat(currentEvent.getEndDate()).isGreaterThan(currentEvent.getStartDate());
        assertThat(currentEvent.getEndDate()).isLessThanOrEqualTo(logbookOperation.getLastPersistedDate());

        if (previousMonthOperation != null) {
            assertThat(
                LocalDateUtil.parseMongoFormattedDate(logbookOperation.getEvDateTime()).minusMinutes(5).minusMonths(1)
            ).isAfter(previousMonthOperation.getEvDateTime());
        }
        if (previousYearOperation != null) {
            assertThat(
                LocalDateUtil.parseMongoFormattedDate(logbookOperation.getEvDateTime()).minusMinutes(5).minusYears(1)
            ).isAfter(previousYearOperation.getEvDateTime());
        }

        checkPreviousOperation(
            previousOperation,
            currentEvent.getPreviousLogbookOperationId(),
            currentEvent.getPreviousLogbookTraceabilityDate(),
            computingInfoProperties.getProperty("previousTimestampToken")
        );

        checkPreviousOperation(
            previousMonthOperation,
            currentEvent.getMinusOneMonthLogbookOperationId(),
            currentEvent.getMinusOneMonthLogbookTraceabilityDate(),
            computingInfoProperties.getProperty("previousTimestampTokenMinusOneMonth")
        );

        checkPreviousOperation(
            previousYearOperation,
            currentEvent.getMinusOneYearLogbookOperationId(),
            currentEvent.getMinusOneYearLogbookTraceabilityDate(),
            computingInfoProperties.getProperty("previousTimestampTokenMinusOneYear")
        );
    }

    private static void checkPreviousOperation(
        LogbookOperation previousOperation,
        String operationId,
        String dateTime,
        String timestampToken
    ) throws InvalidParseOperationException {
        if (previousOperation == null) {
            assertThat(operationId).isNull();
            assertThat(dateTime).isNull();
            assertThat(timestampToken).isEqualTo("null");
        } else {
            assertThat(operationId).isEqualTo(previousOperation.getId());
            assertThat(dateTime).isEqualTo(getTraceabilityEvent(previousOperation).getStartDate());
            assertThat(timestampToken).isEqualTo(
                BaseXx.getBase64(getTraceabilityEvent(previousOperation).getTimeStampToken())
            );
        }
    }

    /**
     * Gets the token content from a traceability zip file
     */
    private static String getTokenContentFromZip(File rootDirectory, TraceabilityEvent traceabilityEvent)
        throws IOException {
        // Use already unzipped dedicated folder
        File dedicatedFolder = new File(rootDirectory, traceabilityEvent.getFileName());

        // Read token content
        File tokenFile = new File(dedicatedFolder, "token.tsp");
        return FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);
    }

    /**
     * Verifies the contents of a traceability zip file
     */
    private static void verifyTraceabilityZip(
        File rootDirectory,
        TraceabilityEvent traceabilityEvent,
        String expectedSecurisationVersion
    ) throws IOException {
        File tmpFolder = new File(rootDirectory, traceabilityEvent.getFileName());

        // Verify operation-related files exist
        verifyOperationFiles(tmpFolder);

        // Verify additional_information.txt contains correct securisationVersion
        verifyAdditionalInformation(expectedSecurisationVersion, tmpFolder);

        // Verify computing_information.txt and token verification
        verifyComputingInformationAndToken(tmpFolder);
    }

    /**
     * Verifies that all expected operation-related files exist in the zip
     */
    private static void verifyOperationFiles(File tmpFolder) {
        File merkleTreeFile = new File(tmpFolder, "merkleTree.json");
        File additionalInfoFile = new File(tmpFolder, "additional_information.txt");
        File computingInfoFile = new File(tmpFolder, "computing_information.txt");
        File tokenFile = new File(tmpFolder, "token.tsp");

        assertThat(merkleTreeFile).exists();
        assertThat(additionalInfoFile).exists();
        assertThat(computingInfoFile).exists();
        assertThat(tokenFile).exists();
    }

    /**
     * Verifies additional_information.txt contains the correct securisationVersion
     */
    private static void verifyAdditionalInformation(String expectedSecurisationVersion, File tmpFolder)
        throws IOException {
        File additionalInfoFile = new File(tmpFolder, "additional_information.txt");
        String additionalInfoContent = FileUtils.readFileToString(additionalInfoFile, StandardCharsets.UTF_8);

        String securisationVersionFromFile = extractValueFromContent(additionalInfoContent, "securisationVersion");
        assertThat(securisationVersionFromFile).isEqualTo(expectedSecurisationVersion);
    }

    /**
     * Extracts a value from content based on key=value format
     */
    private static String extractValueFromContent(String content, String key) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        return properties.getProperty(key);
    }

    /**
     * Verifies computing_information.txt and handles token verification logic
     */
    private static void verifyComputingInformationAndToken(File tmpFolder) throws IOException {
        File computingInfoFile = new File(tmpFolder, "computing_information.txt");
        File tokenFile = new File(tmpFolder, "token.tsp");

        String computingInfoContent = FileUtils.readFileToString(computingInfoFile, StandardCharsets.UTF_8);
        String tokenContent = FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);

        // Verify that the fields exist
        assertThat(computingInfoContent).isNotNull();
        assertThat(tokenContent).isNotNull();
    }

    public static TraceabilityEvent getTraceabilityEvent(String operationId)
        throws InvalidParseOperationException, LogbookClientException {
        return getTraceabilityEvent(VitamTestHelper.selectLogbookOperation(operationId));
    }

    public static TraceabilityEvent getTraceabilityEvent(LogbookOperation logbookOperation)
        throws InvalidParseOperationException {
        if (StringUtils.isEmpty(logbookOperation.getEvDetData())) {
            return null;
        }
        return JsonHandler.getFromString(logbookOperation.getEvDetData(), TraceabilityEvent.class);
    }
}
