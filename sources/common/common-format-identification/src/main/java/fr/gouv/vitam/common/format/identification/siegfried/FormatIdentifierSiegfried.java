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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierInfo;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Siegfried implementation of format identifier
 */
public class FormatIdentifierSiegfried implements FormatIdentifier {

    /**
     * Pronom namespace
     */
    public static final String PRONOM_NAMESPACE = "pronom";
    /**
     * Unknown namespace
     */
    public static final String UNKNOW_NAMESPACE = "UNKNOWN";
    private final Path versionPath;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentifierSiegfried.class);

    private final SiegfriedClientFactory siegfriedClientFactory;

    /**
     * Configuration should come with 'client', 'rootPath' and 'versionPath' mandatory parameters. If client is 'http':
     * 'host' and 'port' are mandatory. If not, mock client is used.
     *
     * @param configurationProperties the configuration properties needed to instantiate Siegfried format identifier
     * @throws FormatIdentifierTechnicalException If a technical error occures when the version path is created
     * @throws IllegalArgumentException if mandatory parameter are not given or null
     */
    public FormatIdentifierSiegfried(Map<String, Object> configurationProperties)
        throws FormatIdentifierTechnicalException {
        ParametersChecker.checkParameter("Client type cannot be null", configurationProperties.get("client"));
        ParametersChecker.checkParameter("Root path cannot be null", configurationProperties.get("rootPath"));
        ParametersChecker.checkParameter("Version pathcannot be null", configurationProperties.get("versionPath"));

        final String clientType = (String) configurationProperties.get("client");
        final String version = (String) configurationProperties.get("versionPath");

        this.siegfriedClientFactory = SiegfriedClientFactory.getInstance();
        if ("http".equals(clientType)) {
            ParametersChecker.checkParameter("Host cannot be null", configurationProperties.get("host"));
            ParametersChecker.checkParameter("Port cannot be null", configurationProperties.get("port"));

            final String host = (String) configurationProperties.get("host");
            final int port = (Integer) configurationProperties.get("port");

            siegfriedClientFactory.changeConfiguration(host, port);
            versionPath = Paths.get(version);

            final Boolean createVersionPath = (Boolean) configurationProperties.get("createVersionPath");
            if (createVersionPath == null || createVersionPath) {
                try {
                    // Create directory already check for file existence and possibility to create the directory.
                    Files.createDirectories(versionPath);
                } catch (final IOException e) {
                    throw new FormatIdentifierTechnicalException(e);
                }
            }
        } else {
            // Mock configuration
            LOGGER.info("Bad value of client. Use mock");
            siegfriedClientFactory.changeConfiguration(null, 0);
            versionPath = Paths.get(version);
        }
    }

    /**
     * For JUnit ONLY
     *
     * @param siegfriedClientFactory a custom instance of siegfried client
     * @param versionPath the version request path
     */
    @VisibleForTesting
    public FormatIdentifierSiegfried(SiegfriedClientFactory siegfriedClientFactory, Path versionPath) {
        this.siegfriedClientFactory = siegfriedClientFactory;
        this.versionPath = versionPath;
    }

    @Override
    public FormatIdentifierInfo status() throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Check Siegfried status");
        }
        try (SiegfriedClient siegfriedClient = siegfriedClientFactory.getClient()) {
            final RequestResponse<JsonNode> response = siegfriedClient.status(versionPath);

            final String version = response.toJsonNode().get("$results").get(0).get("siegfried").asText();
            return new FormatIdentifierInfo(version, "Siegfried");
        }
    }

    @Override
    public List<FormatIdentifierResponse> analysePath(Path path)
        throws FileFormatNotFoundException, FormatIdentifierBadRequestException, FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("identify format for " + path);
        }
        try (SiegfriedClient siegfriedClient = siegfriedClientFactory.getClient()) {
            final RequestResponse<JsonNode> response = siegfriedClient.analysePath(path);
            return extractFormat(response.toJsonNode().get("$results").get(0), path);
        }
    }

    private List<FormatIdentifierResponse> extractFormat(JsonNode siegfriedResponse, Path path)
        throws FileFormatNotFoundException, FormatIdentifierBadRequestException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("extract format from siegfried response");
        }

        final List<FormatIdentifierResponse> matchesFormats = new ArrayList<>();

        final ArrayNode files = (ArrayNode) siegfriedResponse.get("files");
        if (files == null || files.size() != 1) {
            throw new FormatIdentifierBadRequestException("The given path is not link to an unique file");
        }
        final JsonNode file = files.get(0);

        final ArrayNode matches = (ArrayNode) file.get("matches");
        for (final JsonNode match : matches) {
            LOGGER.debug("Check match {}", match);
            final String formatId = match.get("id").asText();
            final String namespace = match.get("ns").asText();
            if (formatResolved(formatId, namespace)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Find a format " + formatId + " for " + namespace);
                }
                final String mimetype = match.get("mime").asText();
                final String format = match.get("format").asText();
                final FormatIdentifierResponse formatIdentifier = new FormatIdentifierResponse(
                    format,
                    mimetype,
                    formatId,
                    namespace
                );
                matchesFormats.add(formatIdentifier);
            } else if (PRONOM_NAMESPACE.equals(namespace)) {
                final JsonNode warnNode = match.get("warning");
                if (warnNode != null) {
                    final String warn = warnNode.asText();
                    final int pos = warn.indexOf("fmt/");
                    final int xpos = warn.indexOf("x-fmt/");
                    int start = -1;
                    if (pos > 0 && xpos > 0) {
                        start = pos < xpos ? pos : xpos;
                    } else if (pos > 0) {
                        start = pos;
                    } else {
                        start = xpos;
                    }
                    if (start > 0) {
                        int end = warn.indexOf(',', start);
                        if (end == -1) {
                            end = warn.length();
                        }
                        if (end > start) {
                            final String newFormatId = warn.substring(start, end);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Find a format " + formatId + " for " + namespace);
                            }
                            final String mimetype = MediaType.APPLICATION_OCTET_STREAM;
                            final String format = "Approximative format: " + newFormatId;
                            final FormatIdentifierResponse formatIdentifier = new FormatIdentifierResponse(
                                format,
                                mimetype,
                                newFormatId,
                                namespace
                            );
                            matchesFormats.add(formatIdentifier);
                        }
                    }
                }
            }
        }

        if (matchesFormats.isEmpty()) {
            LOGGER.warn("No format match found for file " + path);
            throw new FileFormatNotFoundException("No match found");
        }

        return matchesFormats;
    }

    private boolean formatResolved(String formatId, String nameSpace) {
        if (PRONOM_NAMESPACE.equals(nameSpace) && UNKNOW_NAMESPACE.equals(formatId)) {
            return false;
        }
        return true;
    }
}
