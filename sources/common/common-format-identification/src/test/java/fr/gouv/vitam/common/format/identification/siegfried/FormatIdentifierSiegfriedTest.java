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
package fr.gouv.vitam.common.format.identification.siegfried;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierInfo;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class FormatIdentifierSiegfriedTest {

    private static final String SAMPLE_VERSION_RESPONSE = "version-response.json";
    private static final String SAMPLE_OK_RESPONSE = "ok-response.json";
    private static final String SAMPLE_UNKNOW_RESPONSE = "unknown-response.json";
    private static final String SAMPLE_UNKNOW_NOWARN_RESPONSE = "unknown-nowarn-response.json";
    private static final String SAMPLE_BAD_REQUEST_RESPONSE = "bad-request-response.json";

    private static final JsonNode JSON_NODE_VERSION = getJsonNode(SAMPLE_VERSION_RESPONSE);
    private static final JsonNode JSON_NODE_RESPONSE_OK = getJsonNode(SAMPLE_OK_RESPONSE);
    private static final JsonNode JSON_NODE_RESPONSE_UNKNOW = getJsonNode(SAMPLE_UNKNOW_RESPONSE);
    private static final JsonNode JSON_NODE_RESPONSE_UNKNOW_NOWARN = getJsonNode(SAMPLE_UNKNOW_NOWARN_RESPONSE);
    private static final JsonNode JSON_NODE_RESPONSE_BAD = getJsonNode(SAMPLE_BAD_REQUEST_RESPONSE);

    private static final Path VERSION_PATH = Paths.get("version/path");
    private static final Path FILE_PATH = Paths.get("file/path");
    private static SiegfriedClientFactory siegfriedClientFactory;
    private static SiegfriedClientRest siegfriedClientRest;

    private static FormatIdentifierSiegfried siegfried;

    private static JsonNode getJsonNode(String file) {
        try {
            return JsonHandler.getFromFile(PropertiesUtils.findFile(file));
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @BeforeClass
    public static void initStatic() {
        siegfriedClientFactory = mock(SiegfriedClientFactory.class);
        siegfriedClientRest = mock(SiegfriedClientRest.class);
        when(siegfriedClientFactory.getClient()).thenReturn(siegfriedClientRest);

        siegfried = new FormatIdentifierSiegfried(siegfriedClientFactory, VERSION_PATH);
    }

    @Test
    public void testSiegfriedStatus() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.status(VERSION_PATH)).thenReturn(new RequestResponseOK().addResult(JSON_NODE_VERSION));

        final FormatIdentifierInfo infos = siegfried.status();
        assertNotNull(infos);
        assertEquals("1.6.4", infos.getVersion());
        assertEquals("Siegfried", infos.getSoftwareName());
    }

    @Test(expected = FormatIdentifierNotFoundException.class)
    public void testSiegfriedStatusNotFound() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.status(VERSION_PATH)).thenThrow(FormatIdentifierNotFoundException.class);
        siegfried.status();
    }

    @Test(expected = FormatIdentifierTechnicalException.class)
    public void testSiegfriedStatusInternalError() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.status(VERSION_PATH)).thenThrow(FormatIdentifierTechnicalException.class);
        siegfried.status();
    }

    @Test
    public void testSiegfriedIdentify() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenReturn(
            new RequestResponseOK().addResult(JSON_NODE_RESPONSE_OK)
        );

        final List<FormatIdentifierResponse> response = siegfried.analysePath(FILE_PATH);
        assertNotNull(response);
        assertEquals(1, response.size());
        final FormatIdentifierResponse format = response.get(0);
        assertEquals("ZIP Format", format.getFormatLiteral());
        assertEquals("x-fmt/263", format.getPuid());
        assertEquals("pronom", format.getMatchedNamespace());
        assertEquals("application/zip", format.getMimetype());
    }

    @Test
    public void testSiegfriedIdentifyUnknownFormatFileButWarnWithFMT() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenReturn(
            new RequestResponseOK().addResult(JSON_NODE_RESPONSE_UNKNOW)
        );
        List<FormatIdentifierResponse> result = siegfried.analysePath(FILE_PATH);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test(expected = FileFormatNotFoundException.class)
    public void testSiegfriedIdentifyNoFormatFile() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenReturn(
            new RequestResponseOK().addResult(JSON_NODE_RESPONSE_UNKNOW_NOWARN)
        );
        siegfried.analysePath(FILE_PATH);
    }

    @Test(expected = FormatIdentifierBadRequestException.class)
    public void testSiegfriedIdentifyBadPath() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenReturn(
            new RequestResponseOK().addResult(JSON_NODE_RESPONSE_BAD)
        );
        siegfried.analysePath(FILE_PATH);
    }

    @Test(expected = FormatIdentifierTechnicalException.class)
    public void testSiegfriedIdentifyParseError() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenThrow(FormatIdentifierTechnicalException.class);
        siegfried.analysePath(FILE_PATH);
    }

    @Test(expected = FormatIdentifierNotFoundException.class)
    public void testSiegfriedIdentifyNotFound() throws Exception {
        reset(siegfriedClientRest);
        when(siegfriedClientRest.analysePath(any())).thenThrow(FormatIdentifierNotFoundException.class);
        siegfried.analysePath(FILE_PATH);
    }
}
