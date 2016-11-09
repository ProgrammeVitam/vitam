/**
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
 */
package fr.gouv.vitam.worker.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * This class test Xml 2 Json conversion
 */
public class Xml2JsonTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Xml2JsonTest.class);

    private static final String JSON_EXTENSION = ".json";

    private static final String XML_SRC =
        "<Aaa><Bbb>val1</Bbb><Bbb>val2</Bbb><Bbb>val3</Bbb><Ccc>val4</Ccc><Bbb>val5</Bbb>" +
            "<Ddd><Bbb>val6</Bbb><Bbb myattr2='value12'>val7</Bbb><Bbb myattr2='value11'>val8</Bbb></Ddd><Eee myattr='value9'>value10</Eee>" +
            "<Fff/><Ggg></Ggg><Hhh><Bbb>value15</Bbb></Hhh></Aaa>";

    private static XMLEventReader getXMLEventReader() throws XMLStreamException {
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        return xmlInputFactory.createXMLEventReader(new ByteArrayInputStream(XML_SRC.getBytes()));
    }

    @Test
    public void testUsingCurrentImplementation()
        throws IOException, XMLStreamException, FactoryConfigurationError, InvalidParseOperationException {
        XMLEventReader reader = getXMLEventReader();
        String elementGuid = GUIDFactory.newGUID().getId();
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(elementGuid + JSON_EXTENSION);
        final JsonXMLConfig config = new JsonXMLConfigBuilder().build();
        final FileWriter tmpFileWriter = new FileWriter(tmpFile);
        final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

        writer.add(reader);
        writer.close();
        reader.close();
        tmpFileWriter.close();
        LOGGER.warn(FileUtil.readFile(tmpFile));
        JsonNode json = JsonHandler.getFromFile(tmpFile);
        LOGGER.warn(JsonHandler.prettyPrint(json));
        assertFalse(json.get("Aaa").get("Bbb").isArray());
    }

    @Test
    public void testUsingStaxonExtended()
        throws IOException, XMLStreamException, FactoryConfigurationError, InvalidParseOperationException {
        XMLEventReader reader = getXMLEventReader();
        String elementGuid = GUIDFactory.newGUID().getId();
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(elementGuid + JSON_EXTENSION);
        final JsonXMLConfig config = new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
            .namespaceDeclarations(false).build();
        final FileWriter tmpFileWriter = new FileWriter(tmpFile);
        final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);

        writer.add(reader);
        writer.close();
        reader.close();
        tmpFileWriter.close();
        LOGGER.warn(FileUtil.readFile(tmpFile));
        JsonNode json = JsonHandler.getFromFile(tmpFile);
        LOGGER.warn(JsonHandler.prettyPrint(json));
        assertFalse(json.get("Aaa").get("Bbb").isArray());
        assertTrue(json.get("Aaa").get("Ddd").get("Bbb").size() == 3);
    }

    @Test
    public void testUsingJSONObject() throws InvalidParseOperationException {
        JSONObject xml = XML.toJSONObject(XML_SRC);
        JsonNode json = JsonHandler.getFromString(xml.toString());
        LOGGER.warn(JsonHandler.prettyPrint(json));
        assertTrue(json.get("Aaa").get("Bbb").isArray());
        assertTrue(json.get("Aaa").get("Bbb").size() == 4);
        assertTrue(json.get("Aaa").get("Ddd").get("Bbb").size() == 3);
        LOGGER.warn(XML.toString(xml));
    }

    @Test
    public void testUsingJSONObjectVitam() throws InvalidParseOperationException {
        fr.gouv.vitam.worker.common.utils.XML.ARRAY_PATH.addAll(Arrays.asList("Eee", "Ggg", "Ddd", "Bbb"));
        JSONObject xml = fr.gouv.vitam.worker.common.utils.XML.toJSONObject(XML_SRC);
        JsonNode json = JsonHandler.getFromString(xml.toString());
        LOGGER.warn(JsonHandler.prettyPrint(json));
        assertTrue(json.get("Aaa").get("Bbb").isArray());
        assertTrue(json.get("Aaa").get("Bbb").size() == 4);
        assertTrue(json.get("Aaa").get("Ddd").size() == 1);
        assertTrue(json.get("Aaa").get("Ddd").get(0).get("Bbb").size() == 3);
        LOGGER.warn(fr.gouv.vitam.worker.common.utils.XML.toString(xml));
        JSONObject xml2 =
            fr.gouv.vitam.worker.common.utils.XML.toJSONObject(fr.gouv.vitam.worker.common.utils.XML.toString(xml));
        JsonNode json2 = JsonHandler.getFromString(xml2.toString());
        LOGGER.warn(JsonHandler.prettyPrint(json2));
        assertEquals(JsonHandler.prettyPrint(json), JsonHandler.prettyPrint(json2));
    }
}
