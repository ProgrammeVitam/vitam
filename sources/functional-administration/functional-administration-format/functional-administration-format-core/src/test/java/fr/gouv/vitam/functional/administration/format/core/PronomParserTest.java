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
package fr.gouv.vitam.functional.administration.format.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;

public class PronomParserTest {
    String FILE_TO_TEST = "FF-vitam.xml";
    String FILE_TO_TEST_KO = "FF-vitam-KO.xml";
    ArrayNode jsonFileFormat = null;

    @Test
    public void testPronomFormat() throws FileFormatException, FileNotFoundException {
        jsonFileFormat = PronomParser.getPronom(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST)));
        final JsonNode node = jsonFileFormat.get(jsonFileFormat.size() - 1);
        assertTrue(node.get("Name").toString().contains("RDF/XML"));
        assertEquals(node.get("PUID").textValue(), "fmt/875");
        assertTrue(node.get("MIMEType").toString().contains("application/rdf+xml"));
        assertFalse(node.get("Alert").asBoolean());
        assertEquals(node.get("Group").textValue(), "");
        assertEquals(node.get("Comment").textValue(), "");
    }

    @Test(expected = FileNotFoundException.class)
    public void testPronomFormatFileKO() throws FileNotFoundException, FileFormatException {
        jsonFileFormat = PronomParser.getPronom(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
    }
}
