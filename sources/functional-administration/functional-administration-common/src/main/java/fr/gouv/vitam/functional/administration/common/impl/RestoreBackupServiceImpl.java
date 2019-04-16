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
package fr.gouv.vitam.functional.administration.common.impl;

import java.io.InputStream;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

/**
 * Service used to recover a Backup copy of the given Vitam collection.<br/>
 */

public class RestoreBackupServiceImpl implements RestoreBackupService {
    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RestoreBackupServiceImpl.class);

    private static final String OBJECT_ID_TAG = "objectId";
    private static final String EXTENSION_JSON = ".json";

    @Override
    public Optional<String> getLatestSavedFileName(String strategy, DataCategory type,
        FunctionalAdminCollections collection) {
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            // listing the content of the storage -> list of backup files.
            VitamRequestIterator<JsonNode> listing = storageClient.listContainer(strategy, type);

            // recover an intact backup copy for the reconstruction.
            Iterable<JsonNode> iterable = () -> listing;
            Stream<JsonNode> stream = StreamSupport.stream(iterable.spliterator(), false);
            // regex -> filter on json and the sequence's version.
            String regex = "\\d+_+(\\w+)_+(\\d+)?" + EXTENSION_JSON + "$";
            Pattern pattern = Pattern.compile(regex);

            Optional<Integer> result = stream.map(n -> n.get(OBJECT_ID_TAG).asText())
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .filter(matcher -> collection.getName().equals(matcher.group(1)))
                .map(matcher -> Integer.valueOf(matcher.group(2)))
                .max(Comparator.naturalOrder());

            // get the last version of the json backup files.
            if (result.isPresent()) {
                return Optional.of(
                    FunctionalBackupService
                        .getBackupFileName(collection, ParameterHelper.getTenantParameter(), result.get()));
            }
        } catch (StorageServerClientException e) {
            LOGGER.error("ERROR: Exception has been thrown when using storage service:", e);
        }
        return Optional.empty();

    }

    @Override
    public Optional<CollectionBackupModel> readLatestSavedFile(String strategy, FunctionalAdminCollections collection) {

        // get the last version of the json backup files.
        Optional<String> lastBackupVersion = getLatestSavedFileName(strategy, DataCategory.BACKUP, collection);

        if (lastBackupVersion.isPresent()) {
            Response response = null;
            try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                response =
                    storageClient.getContainerAsync(strategy, lastBackupVersion.get(), DataCategory.BACKUP);
                final InputStream inputStream =
                    response.readEntity(InputStream.class);

                    // get backup collections to reconstruct.
                    return Optional.of(JsonHandler.getFromInputStream(inputStream, CollectionBackupModel.class));
            } catch (StorageServerClientException | StorageNotFoundException | InvalidParseOperationException e) {
                LOGGER.error("ERROR: Exception has been thrown when using storage service:", e);
            } finally {
                StreamUtils.consumeAnyEntityAndClose(response);
            }
        }
        return Optional.empty();
    }
}
