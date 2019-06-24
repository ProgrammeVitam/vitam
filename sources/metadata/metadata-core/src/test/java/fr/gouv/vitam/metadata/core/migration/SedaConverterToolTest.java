/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.bson.Document;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * utils for converting objects from seda 2.0 to seda 2.1 spec
 */
public class SedaConverterToolTest {
    private static final String UNIT_SEDA_2_0_FILE_1 = "DataMigrationR6/SedaUnitDataSet/AU_SEDA_2.0_1.json",
                                UNIT_SEDA_2_0_FILE_2 = "DataMigrationR6/SedaUnitDataSet/AU_SEDA_2.0_2.json",
                                UNIT_SEDA_2_1_FILE_1 =
                                    "DataMigrationR6/SedaUnitDataSet/AU_SEDA_2.1_NotToBeMigrated.json";

    @Test
    public void convertUnitToSeda21Test() throws InvalidParseOperationException, FileNotFoundException {

        JsonNode jsonUnit = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNIT_SEDA_2_0_FILE_1));

        Unit unit = new Unit(jsonUnit);

        assertTrue(unit.get("RestrictionEndDate") != null);

        Unit convertedUnit = SedaConverterTool.convertUnitToSeda21(unit);

        System.out.println(BsonHelper.stringify(convertedUnit));

        assertConvertedUnitOk(convertedUnit);
        assertTrue(((Document) ((List)convertedUnit.get("Signature")).get(0)).get("DateSignature") == null);

        //test 2
        jsonUnit = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNIT_SEDA_2_0_FILE_2));

         unit = new Unit(jsonUnit);

        assertTrue(unit.get("Signature") != null);
        assertTrue(((Document)unit.get("Signature")).get("DateSignature") != null);

        convertedUnit = SedaConverterTool.convertUnitToSeda21(unit);

        System.out.println("========================================================================");
        System.out.println(BsonHelper.stringify(convertedUnit));

        assertConvertedUnitOk(convertedUnit);
        assertTrue(((Document) ((List)convertedUnit.get("Signature")).get(0)).get("DateSignature") == null);

        //not to be migrated
        jsonUnit = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNIT_SEDA_2_1_FILE_1));

        assertConvertedUnitOk(SedaConverterTool.convertUnitToSeda21(new Unit(jsonUnit)));

        System.out.println("========================================================================");
        System.out.println(BsonHelper.stringify(convertedUnit));

    }

    private void assertConvertedUnitOk(Unit convertedUnit) {
        assertTrue(convertedUnit.get("OriginatingSystemId") instanceof List);
        assertTrue(convertedUnit.get("AuthorizedAgent") instanceof List);
        assertTrue(convertedUnit.get("ArchivalAgencyArchiveUnitIdentifier")  instanceof List);
        assertTrue(convertedUnit.get("RestrictionEndDate") == null);
        assertTrue(convertedUnit.get("Href") == null);
        assertTrue(convertedUnit.get("Signature") instanceof List);
        assertTrue(((Document) ((List)convertedUnit.get("Signature")).get(0)).get("Signer")  instanceof List);
    }
}
