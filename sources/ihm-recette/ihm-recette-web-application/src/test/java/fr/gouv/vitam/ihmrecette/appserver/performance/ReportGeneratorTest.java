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
package fr.gouv.vitam.ihmrecette.appserver.performance;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

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

        JsonNode logbookJsonNode = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/logbook-operation.json")
        );
        LogbookOperation logbook = JsonHandler.getFromJsonNode(logbookJsonNode, LogbookOperation.class);
        String operationId = "123456789";

        // When
        reportGenerator.generateReport(operationId, logbook);
        reportGenerator.generateReport(operationId, logbook);
        reportGenerator.close();

        // Then
        assertThat(path).hasContent(
            "operationId,PROCESS_SIP_UNITARY,STP_SANITY_CHECK_SIP,SANITY_CHECK_SIP,CHECK_CONTAINER,STP_UPLOAD_SIP,STP_INGEST_CONTROL_SIP,PREPARE_STORAGE_INFO,CHECK_SEDA,CHECK_HEADER,CHECK_HEADER.CHECK_AGENT,CHECK_HEADER.CHECK_CONTRACT_INGEST,CHECK_DATAOBJECTPACKAGE,CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_DATAOBJECT_VERSION,CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_OBJECTNUMBER,CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST,CHECK_DATAOBJECTPACKAGE.CHECK_CONSISTENCY,STP_OG_CHECK_AND_TRANSFORME,CHECK_DIGEST,OG_OBJECTS_FORMAT_CHECK,STP_UNIT_CHECK_AND_PROCESS,CHECK_UNIT_SCHEMA,CHECK_CLASSIFICATION_LEVEL,UNITS_RULES_COMPUTE,STP_STORAGE_AVAILABILITY_CHECK,STORAGE_AVAILABILITY_CHECK,STORAGE_AVAILABILITY_CHECK.STORAGE_AVAILABILITY_CHECK,STP_OBJ_STORING,OBJ_STORAGE,OG_METADATA_INDEXATION,STP_UNIT_METADATA,UNIT_METADATA_INDEXATION,STP_OG_STORING,COMMIT_LIFE_CYCLE_OBJECT_GROUP,OG_METADATA_STORAGE,STP_UNIT_STORING,COMMIT_LIFE_CYCLE_UNIT,UNIT_METADATA_STORAGE,STP_ACCESSION_REGISTRATION,ACCESSION_REGISTRATION,STP_INGEST_FINALISATION,ATR_NOTIFICATION,ROLL_BACK" +
            lineSeparator +
            "123456789,10439,58,59,0,85,434,434,0,0,0,0,0,0,0,0,0,471,471,0,820,820,0,0,346,346,0,695,695,0,813,813,768,768,0,1482,1482,1,652,652,582,582,0" +
            lineSeparator +
            "123456789,10439,58,59,0,85,434,434,0,0,0,0,0,0,0,0,0,471,471,0,820,820,0,0,346,346,0,695,695,0,813,813,768,768,0,1482,1482,1,652,652,582,582,0"
        );
    }
}
