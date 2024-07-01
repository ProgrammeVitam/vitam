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
package fr.gouv.vitam.storage.engine.server.spi.mapper;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverMapperException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The driver mapper implementation
 *
 * Using file to persist driver / offer association. One file by driver (the
 * filename is the driver name). In the file, offers are isolated by delimiter.
 */

public class FileDriverMapper implements DriverMapper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileDriverMapper.class);
    private static final String DRIVER_MAPPING_CONF_FILE = "driver-mapping.conf";
    private static FileDriverMapperConfiguration configuration;
    private static final FileDriverMapper INSTANCE = new FileDriverMapper();

    private FileDriverMapper() {
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(DRIVER_MAPPING_CONF_FILE),
                FileDriverMapperConfiguration.class
            );
        } catch (final IOException exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_MAPPING_INITIALIZE), exc);
        }
    }

    /**
     * Get the driver mapper instance
     *
     * @return the FileDriverMapper instance
     * @throws StorageDriverMapperException if cannot initialize FileDriverMapper (error with the
     * configuration file)
     */
    public static FileDriverMapper getInstance() throws StorageDriverMapperException {
        if (configuration == null) {
            throw new StorageDriverMapperException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_MAPPING_INITIALIZE)
            );
        }
        return INSTANCE;
    }

    @Override
    public List<String> getOffersFor(String driverName) throws StorageDriverMapperException {
        try {
            return getOfferIdsFrom(getDriverFile(driverName));
        } catch (final FileNotFoundException exc) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(exc);
            LOGGER.warn("Configuration file not found for {}, then return empty list", driverName);
            return new ArrayList<>();
        } catch (final IOException exc) {
            final String log = VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_MAPPER_FILE_CONTENT, driverName);
            LOGGER.error(log);
            throw new StorageDriverMapperException(log, exc);
        }
    }

    @Override
    public void addOfferTo(String offerId, String driverName) throws StorageDriverMapperException {
        List<String> offerIds = getOfferIdsList(driverName);
        offerIds = addOfferTo(offerId, offerIds);
        persistDriverMapping(driverName, offerIds);
    }

    @Override
    public void addOffersTo(List<String> offersIdsToAdd, String driverName) throws StorageDriverMapperException {
        List<String> offerIds = getOfferIdsList(driverName);
        for (final String offerIdToAdd : offersIdsToAdd) {
            offerIds = addOfferTo(offerIdToAdd, offerIds);
        }
        persistDriverMapping(driverName, offerIds);
    }

    @Override
    public void removeOfferTo(String offerId, String driverName) throws StorageDriverMapperException {
        List<String> offerIds = getOfferIdsList(driverName);
        offerIds = removeOfferTo(offerId, offerIds);
        persistDriverMapping(driverName, offerIds);
    }

    @Override
    public void removeOffersTo(List<String> offersIdsToRemove, String driverName) throws StorageDriverMapperException {
        List<String> offerIds = getOfferIdsList(driverName);
        for (final String offerIdToRemove : offersIdsToRemove) {
            offerIds = removeOfferTo(offerIdToRemove, offerIds);
        }
        persistDriverMapping(driverName, offerIds);
    }

    private File getDriverFile(String driverName) throws FileNotFoundException {
        return PropertiesUtils.findFile(configuration.getDriverMappingPath() + driverName);
    }

    private List<String> getOfferIdsFrom(File fileDriverMapping) throws IOException {
        final String content = com.google.common.io.Files.readFirstLine(fileDriverMapping, Charset.defaultCharset());
        return content == null
            ? new ArrayList<>()
            : Pattern.compile(configuration.getDelimiter()).splitAsStream(content).collect(Collectors.toList());
    }

    private List<String> addOfferTo(String offerId, List<String> offerMapping) {
        final int index = offerMapping.indexOf(offerId);
        if (index >= 0) {
            LOGGER.warn("Offer ID {} already associated to the driver !", offerId);
        } else {
            offerMapping.add(offerId);
        }
        return offerMapping;
    }

    private List<String> removeOfferTo(String offerId, List<String> offerMapping) {
        final int index = offerMapping.indexOf(offerId);
        if (index >= 0) {
            offerMapping.remove(index);
        } else {
            LOGGER.warn("Offer ID {} not found ! Cannot remove it in mapping configuration", offerId);
        }
        return offerMapping;
    }

    private String getContentFrom(List<String> offerIds) {
        return offerIds.stream().collect(Collectors.joining(";"));
    }

    private List<String> getOfferIdsList(String driverName) throws StorageDriverMapperException {
        List<String> offerIds = null;
        try {
            offerIds = getOfferIdsFrom(getDriverFile(driverName));
        } catch (final FileNotFoundException exc) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(exc);
            LOGGER.warn("Configuration file not found for {}, then return empty list", driverName);
        } catch (final IOException exc) {
            final String log = VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_MAPPER_FILE_CONTENT, driverName);
            LOGGER.error(log);
            throw new StorageDriverMapperException(log, exc);
        }
        if (offerIds == null) {
            offerIds = new ArrayList<>();
        }
        return offerIds;
    }

    private void persistDriverMapping(String driverName, List<String> offerIds) throws StorageDriverMapperException {
        try {
            Files.write(
                Paths.get(configuration.getDriverMappingPath() + driverName),
                getContentFrom(offerIds).getBytes()
            );
        } catch (final IOException exc) {
            final String log = VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_MAPPING_SAVE, driverName);
            LOGGER.error(
                log + " File: " + Paths.get(configuration.getDriverMappingPath() + driverName).toAbsolutePath()
            );
            throw new StorageDriverMapperException(log, exc);
        }
    }
}
