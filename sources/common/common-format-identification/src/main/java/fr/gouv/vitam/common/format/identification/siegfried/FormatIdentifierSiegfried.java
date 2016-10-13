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

package fr.gouv.vitam.common.format.identification.siegfried;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

/**
 * Siegfried implementation of format identifier
 */
public class FormatIdentifierSiegfried implements FormatIdentifier {
    public static final String PRONOM_NAMESPACE = "pronom";
    public static final String UNKNOW_NAMESPACE = "UNKNOW";
    private final SiegfriedClient client;
    private final Path rootPath;
    private final Path versionPath;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentifierSiegfried.class);

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

        String clientType = (String) configurationProperties.get("client");
        String root = (String) configurationProperties.get("rootPath");
        String version = (String) configurationProperties.get("versionPath");

        SiegfriedClientFactory factory = SiegfriedClientFactory.getInstance();

        if ("http".equals(clientType)) {
            ParametersChecker.checkParameter("Host cannot be null", configurationProperties.get("host"));
            ParametersChecker.checkParameter("Port cannot be null", configurationProperties.get("port"));

            String host = (String) configurationProperties.get("host");
            int port = (Integer) configurationProperties.get("port");

            factory.changeConfiguration(host, port);
            this.client = factory.getSiegfriedClient();
            this.rootPath = Paths.get(root);
            this.versionPath = Paths.get(version);

            Boolean createVersionPath = (Boolean) configurationProperties.get("createVersionPath");
            if (BooleanUtils.isNotFalse(createVersionPath)) {
                try {
                    // Create directory already check for file existance and possibility to create the directory.
                    Files.createDirectories(versionPath);
                } catch (IOException e) {
                    throw new FormatIdentifierTechnicalException(e);
                }
            }

        } else {
            // Mock configuration
            LOGGER.info("Bad value of client. Use mock");
            factory.changeConfiguration(null, 0);
            this.client = factory.getSiegfriedClient();
            this.rootPath = Paths.get(root);
            this.versionPath = Paths.get(version);
        }
    }

    /**
     * For JUnit ONLY
     * 
     * @param mockedClient a custom instance of siegfried client
     * @param rootPath the siegfried data root path
     * @param versionPath the version request path
     */
    FormatIdentifierSiegfried(SiegfriedClient mockedClient, Path rootPath, Path versionPath) {
        this.client = mockedClient;
        this.rootPath = rootPath;
        this.versionPath = versionPath;
    }

    @Override
    public FormatIdentifierInfo status() throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Check Siegfried status");
        }

        JsonNode response = client.status(versionPath);

        String version = response.get("siegfried").asText();
        return new FormatIdentifierInfo(version, "Siegfried");
    }

    @Override
    public List<FormatIdentifierResponse> analysePath(Path path)
        throws FileFormatNotFoundException, FormatIdentifierTechnicalException, FormatIdentifierBadRequestException,
        FormatIdentifierNotFoundException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("identify format for " + path);
        }
        Path filePath = Paths.get(rootPath.toString() + "/" + path.toString());

        JsonNode response = client.analysePath(filePath);

        return extractFormat(response);
    }

    private List<FormatIdentifierResponse> extractFormat(JsonNode siegfriedResponse)
        throws FileFormatNotFoundException, FormatIdentifierBadRequestException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("extract format from siegfried response");
        }

        List<FormatIdentifierResponse> matchesFormats = new ArrayList<>();

        ArrayNode files = (ArrayNode) siegfriedResponse.get("files");
        if (files == null || files.size() != 1) {
            throw new FormatIdentifierBadRequestException("The given path is not link to an unique file");
        }
        JsonNode file = files.get(0);

        ArrayNode matches = (ArrayNode) file.get("matches");
        for (JsonNode match : matches) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Check match " + match.toString());
            }
            String formatId = match.get("id").asText();
            String namespace = match.get("ns").asText();
            if (formatResolved(formatId, namespace)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Find a format " + formatId + " for " + namespace);
                }
                String mimetype = match.get("mime").asText();
                String format = match.get("format").asText();
                FormatIdentifierResponse formatIdentifier =
                    new FormatIdentifierResponse(format, mimetype, formatId, namespace);
                matchesFormats.add(formatIdentifier);
            }
        }

        if (matchesFormats.isEmpty()) {
            LOGGER.warn("No format match found for file");
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
