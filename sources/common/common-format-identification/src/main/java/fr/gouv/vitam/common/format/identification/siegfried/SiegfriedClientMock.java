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
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import java.nio.file.Path;

/**
 * Mock client implementation for siegfried
 */
class SiegfriedClientMock extends AbstractMockClient implements SiegfriedClient {

    @Override
    public RequestResponse<JsonNode> status(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        return new RequestResponseOK().addResult(getVersionJson());
    }

    @Override
    public RequestResponse<JsonNode> analysePath(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        return new RequestResponseOK().addResult(getFormatJson(filePath));
    }

    private JsonNode getVersionJson() {
        final String versionResponse = "{\"siegfried\":\"mock-1.0\"}";
        return getJsonNode(versionResponse);
    }

    private JsonNode getFormatJson(Path path) {
        final String okResponse;
        if (path.endsWith(".zip")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/263\",\"format\": \"ZIP Format\",\"mime\": \"application/zip\"}]}]}";
        } else if (path.endsWith(".tar.gz")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/266\",\"format\": \"TAR GZIP Format\",\"mime\": \"application/x-tar\"}]}]}";
        } else if (path.endsWith(".tar.bz2")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/267\",\"format\": \"TAR BZ2 Format\",\"mime\": \"application/x-bzip2\"}]}]}";
        } else if (path.endsWith(".tar")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/265\",\"format\": \"TAR Format\",\"mime\": \"application/x-tar\"}]}]}";
        } else if (path.endsWith(".pdf")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/18\",\"format\": \"PDF Format\",\"mime\": \"application/pdf\"}]}]}";
        } else if (path.endsWith(".jpg")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/43\",\"format\": \"JPG Format\",\"mime\": \"image/jpeg\"}]}]}";
        } else if (path.endsWith(".png")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/12\",\"format\": \"PNG Format\",\"mime\": \"image/png\"}]}]}";
        } else if (path.endsWith(".ods")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/295\",\"format\": \"ODS Format\",\"mime\": \"application/vnd.oasis.opendocument.spreadsheet\"}]}]}";
        } else if (path.endsWith(".odt")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/290\",\"format\": \"ODT Format\",\"mime\": \"application/vnd.oasis.opendocument.text\"}]}]}";
        } else if (path.endsWith(".doc")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/40\",\"format\": \"DOC Format\",\"mime\": \"application/msword\"}]}]}";
        } else if (path.endsWith(".xls")) {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"fmt/61\",\"format\": \"XLS Format\",\"mime\": \"application/vnd.ms-excel\"}]}]}";
        } else {
            okResponse =
                "{\"files\":[{\"matches\":[{\"ns\": \"pronom\",\"id\": \"x-fmt/263\",\"format\": \"ZIP Format\",\"mime\": \"application/zip\"}]}]}";
        }
        return getJsonNode(okResponse);
    }

    private static JsonNode getJsonNode(String jsonString) {
        try {
            return JsonHandler.getFromString(jsonString);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
