/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class SanityCheckerTest {

    private final String pathXMLOK = "testOK.xml";
    private final String pathXMLKO = "testKO.xml";
    private final String[] pathsXMLowaspKO =
        {"testOwaspException1.xml", "testOwaspException2.xml", "testOwaspException3.xml"};
    private File fileOK = null;
    private File fileKO = null;
    private final List<File> filesOwaspKO = new ArrayList<>();

    private final String JSON_TEST_FILE = "json";
    private final String JSON_TEST_FILE2 = "json_good_sanity";
    private final double limitFileSize = SanityChecker.getLimitFileSize();


    @Before
    public void setUp() throws FileNotFoundException {
        fileOK = PropertiesUtils.findFile(pathXMLOK);
        fileKO = PropertiesUtils.findFile(pathXMLKO);
        for (final String pathFile : pathsXMLowaspKO) {
            filesOwaspKO.add(PropertiesUtils.findFile(pathFile));
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkXMLFileSize() throws IOException, InvalidParseOperationException {
        final long limit = SanityChecker.getLimitFileSize();
        try {
            SanityChecker.setLimitFileSize(100);
            SanityChecker.checkXmlSanityFileSize(fileOK);
        } finally {
            SanityChecker.setLimitFileSize(limit);
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkXMLTagSize() throws IOException, InvalidParseOperationException {
        final int limit = SanityChecker.getLimitFieldSize();
        try {
            SanityChecker.setLimitFieldSize(100);
            SanityChecker.checkXmlSanityTagValueSize(fileKO);
        } finally {
            SanityChecker.setLimitFieldSize(limit);
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkXMLTags() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXmlSanityTags(fileKO);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkInvalidTags_script() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXmlSanityTags(filesOwaspKO.get(0));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkInvalidTags_xssLocator() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXmlSanityTags(filesOwaspKO.get(1));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkInvalidTags_nullChar() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXmlSanityTags(filesOwaspKO.get(2));
    }

    @Test
    public void checkXMLAllOK() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXmlAll(fileOK);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenJsonWhenValueIsTooBigORContainXMLTag()
        throws InvalidParseOperationException, IOException {
        final File file = PropertiesUtils.findFile(JSON_TEST_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        assertNotNull(json);
        SanityChecker.checkJsonSanity(json);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenJsonWhenValueIsTooBigORContainXMLTagUsingAll()
        throws InvalidParseOperationException, IOException {
        final File file = PropertiesUtils.findFile(JSON_TEST_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        assertNotNull(json);
        SanityChecker.checkJsonAll(json);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenJsonStringWhenValueIsTooBigORContainXMLTagUsingAll()
        throws InvalidParseOperationException, IOException {
        final File file = PropertiesUtils.findFile(JSON_TEST_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        assertNotNull(json);
        SanityChecker.checkJsonAll(json.toString());
    }

    @Test
    public void givenJsonWhenGoodSanityThenReturnTrue()
        throws FileNotFoundException, InvalidParseOperationException {
        final long limit = SanityChecker.getLimitJsonSize();
        try {
            SanityChecker.setLimitJsonSize(100);
            final File file = PropertiesUtils.findFile(JSON_TEST_FILE2);
            final JsonNode json = JsonHandler.getFromFile(file);
            try {
                SanityChecker.checkJsonAll(json);
                fail("Should failed with an exception");
            } catch (final InvalidParseOperationException e) {}
            SanityChecker.setLimitJsonSize(10000);
            SanityChecker.checkJsonAll(json);
            SanityChecker.checkJsonAll(json.toString());
        } finally {
            SanityChecker.setLimitJsonSize(limit);
        }
    }

    @Test
    public void givenStringGoodSanity() throws InvalidParseOperationException {
        final String good = "abcdef";
        SanityChecker.checkParameter(good);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringBadSize() throws InvalidParseOperationException {
        final int limit = SanityChecker.getLimitParamSize();
        try {
            final String bad = new String(StringUtils.getRandom(40));
            SanityChecker.setLimitParamSize(bad.length() - 5);
            SanityChecker.checkParameter(bad);
        } finally {
            SanityChecker.setLimitParamSize(limit);
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringScript() throws InvalidParseOperationException {
        final String bad = "aa<script>bb";
        SanityChecker.checkParameter(bad);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringCdata() throws InvalidParseOperationException {
        final String bad = "aa<![CDATA[bb";
        SanityChecker.checkParameter(bad);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringEntity() throws InvalidParseOperationException {
        final String bad = "aa<!ENTITYbb";
        SanityChecker.checkParameter(bad);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringXml() throws InvalidParseOperationException {
        final String bad = "aa<strong>bb</strong>bb";
        SanityChecker.checkParameter(bad);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenStringNotPrintable() throws InvalidParseOperationException {
        final String bad = "aa\u0003bb";
        SanityChecker.checkParameter(bad);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void testHeaders() throws InvalidParseOperationException {
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        final HttpHeaders headers = new HttpHeaders() {

            @Override
            public MultivaluedMap<String, String> getRequestHeaders() {
                return map;
            }

            @Override
            public List<String> getRequestHeader(String name) {
                return map.get(name);
            }

            @Override
            public MediaType getMediaType() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Locale getLanguage() {
                return null;
            }

            @Override
            public String getHeaderString(String name) {
                return map.get(name).get(0);
            }

            @Override
            public Date getDate() {
                return null;
            }

            @Override
            public Map<String, Cookie> getCookies() {
                return null;
            }

            @Override
            public List<MediaType> getAcceptableMediaTypes() {
                return null;
            }

            @Override
            public List<Locale> getAcceptableLanguages() {
                return null;
            }
        };
        map.add("test", "ok");
        try {
            SanityChecker.checkHeaders(headers);
        } catch (final InvalidParseOperationException e) {
            fail("Should not raized an exception");
        }
        final String bad = "aa<![CDATA[bb";
        map.add("test", bad);
        SanityChecker.checkHeaders(headers);
    }
}
