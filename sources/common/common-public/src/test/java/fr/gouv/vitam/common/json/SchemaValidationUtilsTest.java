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
package fr.gouv.vitam.common.json;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;

public class SchemaValidationUtilsTest {

    private static final String AU_JSON_FILE = "archive-unit_OK.json";
    private static final String AU_INVALID_JSON_FILE = "archive-unit_Invalid.json";
    private static final String COMPLEX_JSON_FILE = "complex_archive_unit.json";

    public static final String TAG_ARCHIVE_UNIT = "ArchiveUnit";

    @Test
    public void givenDefaultConstructorThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test
    public void givenConstructorWithCorrectSchemaThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation =
            new SchemaValidationUtils("json-schema/archive-unit-schema.json");
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test
    public void givenComplexArchiveUnitJsonThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(COMPLEX_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test(expected = FileNotFoundException.class)
    public void givenConstructorWithInexistingSchemaThenException() throws Exception {
        new SchemaValidationUtils("json-schema/archive-unit-schema-inexisting.json");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenConstructorWithIncorrectSchemaThenException() throws Exception {
        new SchemaValidationUtils("manifestOK.xml");
    }


    @Test
    public void givenInvalidJsonFileThenValidateKO() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_INVALID_JSON_FILE)));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.NOT_AU_JSON_VALID));
    }

}
