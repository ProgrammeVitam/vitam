/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class ReportGeneratorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldGenerateCSVReportFromLogbookOperation()
        throws InvalidParseOperationException, ParseException, IOException {
        // Given
        String lineSeparator = System.getProperty("line.separator");
        Path path = temporaryFolder.newFile().toPath();
        ReportGenerator reportGenerator = new ReportGenerator(path);

        JsonNode logbook = JsonHandler.getFromInputStream(getClass().getResourceAsStream("/logbook-operation.json"));
        String operationId = "123456789";

        // When
        reportGenerator.generateReport(operationId, logbook);
        reportGenerator.generateReport(operationId, logbook);
        reportGenerator.close();

        // Then
        assertThat(path).hasContent(
            "operationId,PROCESS_SIP_UNITARY,STP_SANITY_CHECK_SIP,SANITY_CHECK_SIP,CHECK_CONTAINER,STP_UPLOAD_SIP,STP_INGEST_CONTROL_SIP,CHECK_SEDA,CHECK_MANIFEST_DATAOBJECT_VERSION,CHECK_MANIFEST_OBJECTNUMBER,CHECK_MANIFEST,CHECK_CONSISTENCY,STP_OG_CHECK_AND_TRANSFORME,CHECK_DIGEST,OG_OBJECTS_FORMAT_CHECK,STP_UNIT_CHECK_AND_PROCESS,UNITS_RULES_COMPUTE,STP_STORAGE_AVAILABILITY_CHECK,STP_OG_STORING,OG_STORAGE,OG_METADATA_INDEXATION,STP_UNIT_STORING,UNIT_METADATA_INDEXATION,STP_ACCESSION_REGISTRATION,ACCESSION_REGISTRATION,STP_INGEST_FINALISATION,ATR_NOTIFICATION" +
                lineSeparator + "123456789,3562,113,9,59,141,550,0,0,0,0,1,550,0,0,468,0,101,574,0,0,651,0,82,0,177,0" +
                lineSeparator + "123456789,3562,113,9,59,141,550,0,0,0,0,1,550,0,0,468,0,101,574,0,0,651,0,82,0,177,0");
    }

}
