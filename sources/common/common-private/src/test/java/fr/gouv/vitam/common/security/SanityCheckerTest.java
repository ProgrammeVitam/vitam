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
package fr.gouv.vitam.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SanityCheckerTest {

    private static final String HTTPD_CERTIFICATE_FORMAT =
        "" +
        "-----BEGIN CERTIFICATE----- " +
        "MIIDeTCCAmGgAwIBAgIUSCGUFJwxYU4vMvX1nbRiqHIDUOUwDQYJKoZIhvcNAQEL " +
        "BQAwGjEYMBYGA1UEAwwPaW50ZXJtZWRpYXRlLWNhMB4XDTIzMDMxNTA3NDcyMloX " +
        "DTQzMDMxMDA3NDcyMlowETEPMA0GA1UEAwwGY2xpZW50MIIBIjANBgkqhkiG9w0B " +
        "AQEFAAOCAQ8AMIIBCgKCAQEApvEtXTG0+XF9jjKZJANVVvz09uHAegZk/31PaWCr " +
        "1Uakq3UlG4sMO7/gYpam/sbOnA/6qNGmVWsh1O0kI6+eKTA3H4cWpqrGJ1UCNFpV " +
        "Uu5rWg8NCFR0Vc40TRaJYb0mCE0Tz5qSXw/tJwiHekpI3xyAlIZRax8hxiViOCKs " +
        "bhgCTTTjiVwcjwyBnlCOqT/d3ZGOLWaHDMvSKc20TctuhLlmblTwYoWoaCHN3zlC " +
        "d3j3oyZ+WQKU3KvRF6lKhS1pug5thEQPGrP4/W1A7W1367BOEnYZI3rzAnTO8WB4 " +
        "kBi/j3pmoPJRBT8fmlIW4qecRiGlhksLgkeLHaAAC0ZH0QIDAQABo4G/MIG8MB0G " +
        "A1UdDgQWBBSXiTHUVeNgrAKV/XGt4J7QEKmMazBNBgNVHSMERjBEgBSmY2e3H14U " +
        "4xvnvLwZDxVkeI1xG6EWpBQwEjEQMA4GA1UEAwwHcm9vdC1jYYIUO/EBBATyqs07 " +
        "wOzMDxZ/D9QlHLswCQYDVR0SBAIwADAMBgNVHRMBAf8EAjAAMAsGA1UdDwQEAwIH " +
        "gDARBglghkgBhvhCAQEEBAMCB4AwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDQYJKoZI " +
        "hvcNAQELBQADggEBAETePX/A5kUDedD9jJR1aPhMF6VSU55Fh9DXeDrVyTRae46P " +
        "TV4LYF7fmgI+jBTSxTCzAzZzMXmLK5UIUGH62X9vgT34F5Btk3KE4jBfsaWMn3Mc " +
        "WNtZsomhnVekrLe1ZBBhlNwRF5WaX9zYk8kyMw3ZWKDwb/dXnikqqIK2+E2WuPgU " +
        "t1ef0wHJRmaDnoox6vm/K1rYYo4jykuhdVYXxBXz7Vm0i7jUoN+BYmAgQ3zRFIv9 " +
        "bwPhO3KheGRPkB8ZUtZuSfxBTTLX+AVGjCHGC/SB4O5HMdwe+QANdyr61RQZV39C " +
        "WK31zmcgpI+s9vol2bn/VQL6szy47RXmctnkZVk= " +
        "-----END CERTIFICATE----- ";

    private static final String NGINX_CERTIFICATE_FORMAT =
        "" +
        "-----BEGIN%20CERTIFICATE-----%0A" +
        "MIIDeTCCAmGgAwIBAgIUSCGUFJwxYU4vMvX1nbRiqHIDUOUwDQYJKoZIhvcNAQEL%0A" +
        "BQAwGjEYMBYGA1UEAwwPaW50ZXJtZWRpYXRlLWNhMB4XDTIzMDMxNTA3NDcyMloX%0A" +
        "DTQzMDMxMDA3NDcyMlowETEPMA0GA1UEAwwGY2xpZW50MIIBIjANBgkqhkiG9w0B%0A" +
        "AQEFAAOCAQ8AMIIBCgKCAQEApvEtXTG0%2BXF9jjKZJANVVvz09uHAegZk%2F31PaWCr%0A" +
        "1Uakq3UlG4sMO7%2FgYpam%2FsbOnA%2F6qNGmVWsh1O0kI6%2BeKTA3H4cWpqrGJ1UCNFpV%0A" +
        "Uu5rWg8NCFR0Vc40TRaJYb0mCE0Tz5qSXw%2FtJwiHekpI3xyAlIZRax8hxiViOCKs%0A" +
        "bhgCTTTjiVwcjwyBnlCOqT%2Fd3ZGOLWaHDMvSKc20TctuhLlmblTwYoWoaCHN3zlC%0A" +
        "d3j3oyZ%2BWQKU3KvRF6lKhS1pug5thEQPGrP4%2FW1A7W1367BOEnYZI3rzAnTO8WB4%0A" +
        "kBi%2Fj3pmoPJRBT8fmlIW4qecRiGlhksLgkeLHaAAC0ZH0QIDAQABo4G%2FMIG8MB0G%0A" +
        "A1UdDgQWBBSXiTHUVeNgrAKV%2FXGt4J7QEKmMazBNBgNVHSMERjBEgBSmY2e3H14U%0A" +
        "4xvnvLwZDxVkeI1xG6EWpBQwEjEQMA4GA1UEAwwHcm9vdC1jYYIUO%2FEBBATyqs07%0A" +
        "wOzMDxZ%2FD9QlHLswCQYDVR0SBAIwADAMBgNVHRMBAf8EAjAAMAsGA1UdDwQEAwIH%0A" +
        "gDARBglghkgBhvhCAQEEBAMCB4AwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDQYJKoZI%0A" +
        "hvcNAQELBQADggEBAETePX%2FA5kUDedD9jJR1aPhMF6VSU55Fh9DXeDrVyTRae46P%0A" +
        "TV4LYF7fmgI%2BjBTSxTCzAzZzMXmLK5UIUGH62X9vgT34F5Btk3KE4jBfsaWMn3Mc%0A" +
        "WNtZsomhnVekrLe1ZBBhlNwRF5WaX9zYk8kyMw3ZWKDwb%2FdXnikqqIK2%2BE2WuPgU%0A" +
        "t1ef0wHJRmaDnoox6vm%2FK1rYYo4jykuhdVYXxBXz7Vm0i7jUoN%2BBYmAgQ3zRFIv9%0A" +
        "bwPhO3KheGRPkB8ZUtZuSfxBTTLX%2BAVGjCHGC%2FSB4O5HMdwe%2BQANdyr61RQZV39C%0A" +
        "WK31zmcgpI%2Bs9vol2bn%2FVQL6szy47RXmctnkZVk%3D%0A" +
        "-----END%20CERTIFICATE-----%0A";

    private final String pathXMLOK = "testOK.xml";
    private final String pathXMLKO = "testKO.xml";
    private final String[] pathsXMLowaspKO = {
        "testOwaspException1.xml",
        "testOwaspException2.xml",
        "testOwaspException3.xml",
    };
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
    public void givenJsonWhenValueIsTooBigORContainXMLTag() throws InvalidParseOperationException, IOException {
        final File file = PropertiesUtils.findFile(JSON_TEST_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        assertNotNull(json);
        SanityChecker.checkJsonSanity(json);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenJsonWhenValueIsTooBigORContainXMLTagUsingAll() throws InvalidParseOperationException, IOException {
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
    public void givenJsonWhenGoodSanityThenReturnTrue() throws FileNotFoundException, InvalidParseOperationException {
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

    @Test
    public void test_should_failed_when_input_is_too_large() throws Exception {
        final String textContent = new String(
            Files.readAllBytes(PropertiesUtils.getResourceFile("text-content.txt").toPath())
        );
        assertThatCode(
            () -> SanityChecker.checkJsonAll(JsonHandler.createObjectNode().put("TextContent", textContent.repeat(20)))
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void test_should_success_when_input_is_not_too_large() throws Exception {
        final String textContent = new String(
            Files.readAllBytes(PropertiesUtils.getResourceFile("text-content.txt").toPath())
        );
        SanityChecker.checkJsonAll(JsonHandler.createObjectNode().put("TextContent", textContent.repeat(19)));
    }

    @Test
    public void test_header_validation_success() throws Exception {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("Host", "my-server");
        requestHeaders.putSingle("User-Agent", "my-client");
        requestHeaders.putSingle("Accept", "application/json");
        requestHeaders.putSingle("Content-Type", "application/json");
        requestHeaders.putSingle("x-access-contract-id", "ContratTNR");
        requestHeaders.putSingle("x-tenant-id", "0");
        requestHeaders.putSingle("Accept-Encoding", "gzip, deflate");
        requestHeaders.putSingle("X-Forwarded-Proto", "https");
        requestHeaders.putSingle("X-Forwarded-Port", "443");
        requestHeaders.putSingle("X-SSL-CLIENT-CERT", NGINX_CERTIFICATE_FORMAT);
        requestHeaders.put("X-Forwarded-For", List.of("123.123.123.123", "1.1.1.1"));
        requestHeaders.putSingle("X-Forwarded-Host", "my.host.fr");
        requestHeaders.putSingle("X-Forwarded-Server", "my-server");
        requestHeaders.putSingle("Content-Length", "186");
        requestHeaders.putSingle("Connection", "keep-alive");

        assertThatCode(() -> SanityChecker.checkHeadersMap(requestHeaders)).doesNotThrowAnyException();
    }

    @Test
    public void test_certificate_header_validation_nginx_format() throws Exception {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("X-SSL-CLIENT-CERT", NGINX_CERTIFICATE_FORMAT);

        assertThatCode(() -> SanityChecker.checkHeadersMap(requestHeaders)).doesNotThrowAnyException();
    }

    @Test
    public void test_certificate_header_validation_httpd_format() throws Exception {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("X-SSL-CLIENT-CERT", HTTPD_CERTIFICATE_FORMAT);

        assertThatCode(() -> SanityChecker.checkHeadersMap(requestHeaders)).doesNotThrowAnyException();
    }

    @Test
    public void test_certificate_header_validation_with_illegal_characters_ko() {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("X-SSL-CLIENT-CERT", "Illegal###");
        assertThatThrownBy(() -> SanityChecker.checkHeadersMap(requestHeaders))
            .isInstanceOf(InvalidParseOperationException.class)
            .hasMessageContaining("X-SSL-CLIENT-CERT header has wrong value");
    }

    @Test
    public void test_certificate_header_multivalued_ko() {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.put("X-SSL-CLIENT-CERT", List.of(HTTPD_CERTIFICATE_FORMAT, NGINX_CERTIFICATE_FORMAT));

        assertThatThrownBy(() -> SanityChecker.checkHeadersMap(requestHeaders))
            .isInstanceOf(InvalidParseOperationException.class)
            .hasMessageContaining("Multiple X-SSL-CLIENT-CERT headers detected");
    }
}
