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
package fr.gouv.vitam.common.format.identification;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierConfiguration;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Format Identifier Factory : used to retrieve the FormatIdentifier implementations
 */
public class FormatIdentifierFactory {

    private static final String FORMAT_IDENTIFIER_ID_NOT_NULL = "formatIdentifierId cannot be null";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentifierFactory.class);

    private static final FormatIdentifierFactory FORMAT_IDENTIFIER_FACTORY = new FormatIdentifierFactory();
    private final Map<String, FormatIdentifierConfiguration> configurationsFormatIdentifiers =
        Collections.synchronizedMap(new HashMap<>());

    private static final String FORMAT_IDENTIFIERS_CONF_FILE = "format-identifiers.conf";

    /**
     * Constructor
     */
    private FormatIdentifierFactory() {
        changeConfigurationFile(FORMAT_IDENTIFIERS_CONF_FILE);
    }

    /**
     * Change client configuration from the Json file
     *
     * @param configurationPath the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        try {
            final File configurationFile = PropertiesUtils.findFile(configurationPath);
            if (configurationFile != null) {
                final Map<String, FormatIdentifierConfiguration> configMap = PropertiesUtils.readYaml(
                    configurationFile,
                    new TypeReference<Map<String, FormatIdentifierConfiguration>>() {}
                );
                for (final FormatIdentifierConfiguration configuration : configMap.values()) {
                    checkConfiguration(configuration);
                }
                configurationsFormatIdentifiers.clear();
                configurationsFormatIdentifiers.putAll(configMap);
            }
        } catch (final IOException e) {
            LOGGER.warn(
                "could not load format identifiers configuration for file {}, no format identifier available",
                configurationPath,
                e
            );
        }
    }

    /**
     * Get the FormatIdentifierFactory instance
     *
     * @return the instance
     */
    public static FormatIdentifierFactory getInstance() {
        return FORMAT_IDENTIFIER_FACTORY;
    }

    /**
     * Instantiate the format identifier identified
     *
     * @param formatIdentifierId format identifier id
     * @return the Format Identifier implementation
     * @throws FormatIdentifierNotFoundException if the configuration was not found
     * @throws FormatIdentifierFactoryException if the implementation was not found
     * @throws FormatIdentifierTechnicalException if any technical error occurs
     * @throws IllegalArgumentException if formatIdentifierId parameter is null
     */
    public FormatIdentifier getFormatIdentifierFor(String formatIdentifierId)
        throws FormatIdentifierNotFoundException, FormatIdentifierFactoryException, FormatIdentifierTechnicalException {
        ParametersChecker.checkParameter(FORMAT_IDENTIFIER_ID_NOT_NULL, formatIdentifierId);
        return instanciate(formatIdentifierId);
    }

    /**
     * Add a format identifier configuration
     *
     * @param formatIdentifierId format identifier id
     * @param configuration format identifier configuration
     * @throws IllegalArgumentException if the parameters are null : formatIdentifierId, configuration,
     * configuration.type
     */
    public void addFormatIdentifier(String formatIdentifierId, FormatIdentifierConfiguration configuration) {
        ParametersChecker.checkParameter(FORMAT_IDENTIFIER_ID_NOT_NULL, formatIdentifierId);
        checkConfiguration(configuration);
        configurationsFormatIdentifiers.put(formatIdentifierId, configuration);
    }

    /**
     * Remove a format identifier configuration by its id
     *
     * @param formatIdentifierId format identifier id
     * @throws FormatIdentifierNotFoundException if no configuration is registered for the given formatIdentifierId
     * @throws IllegalArgumentException if the parameters are null : formatIdentifierId
     */
    public void removeFormatIdentifier(String formatIdentifierId) throws FormatIdentifierNotFoundException {
        ParametersChecker.checkParameter(FORMAT_IDENTIFIER_ID_NOT_NULL, formatIdentifierId);
        if (configurationsFormatIdentifiers.remove(formatIdentifierId) == null) {
            throw new FormatIdentifierNotFoundException(
                "Can't remove " + formatIdentifierId + " because no configuration with this name is registered"
            );
        }
    }

    /**
     * Instanciate the format identifier using the configuration linked to the format identifier id given
     *
     * @param formatIdentifierId format identifier id
     * @return the Format Identifier implementation
     * @throws FormatIdentifierNotFoundException if the configuration was not found
     * @throws FormatIdentifierFactoryException if the implementation was not found
     * @throws FormatIdentifierTechnicalException If any technical error occurs
     * @throws IllegalArgumentException if the configuration don't match mandatory parameter of implementations
     */
    private FormatIdentifier instanciate(String formatIdentifierId)
        throws FormatIdentifierFactoryException, FormatIdentifierNotFoundException, FormatIdentifierTechnicalException {
        final FormatIdentifierConfiguration infos = configurationsFormatIdentifiers.get(formatIdentifierId);
        if (infos == null) {
            throw new FormatIdentifierNotFoundException(
                "Format Identifier configuration can't be found for id " + formatIdentifierId
            );
        }
        switch (infos.getType()) {
            case MOCK:
                return new FormatIdentifierMock();
            case SIEGFRIED:
                return new FormatIdentifierSiegfried(infos.getConfigurationProperties());
            default:
                throw new FormatIdentifierFactoryException(
                    "Format Identifier Configuration implementation can't be found for id " + formatIdentifierId
                );
        }
    }

    /**
     * Check the format identifier configuration
     *
     * @param configuration the format identifier configuration
     * @throws IllegalArgumentException if the parameters are null : configuration, configuration.type
     */
    private void checkConfiguration(FormatIdentifierConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        ParametersChecker.checkParameter("Type cannot be null", configuration.getType());
    }
}
