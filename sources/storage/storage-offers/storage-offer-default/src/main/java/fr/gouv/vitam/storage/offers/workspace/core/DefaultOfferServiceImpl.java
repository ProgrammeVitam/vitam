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

package fr.gouv.vitam.storage.offers.workspace.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteStreams;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;
import fr.gouv.vitam.workspace.core.filesystem.FileSystem;

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private static final DefaultOfferService INSTANCE = new DefaultOfferServiceImpl();
    private static final String TMP_DIRECTORY = "/tmp/";
    private final ContentAddressableStorage defaultStorage;
    private static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private final Map<String, DigestType> digestTypeFor;
    private final Map<String, String> objectTypeFor;

    private DefaultOfferServiceImpl() {
        StorageConfiguration configuration;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                StorageConfiguration.class);
        } catch (final IOException exc) {
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = new FileSystem(configuration);
        digestTypeFor = new HashMap<>();
        objectTypeFor = new HashMap<>();
    }

    /**
     * @return the default instance
     */
    public static DefaultOfferService getInstance() {
        return INSTANCE;
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        return defaultStorage.computeObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public InputStream getObject(String containerName, String objectId) throws ContentAddressableStorageException {
        return defaultStorage.getObject(containerName, objectId);
    }

    @Override
    public ObjectInit initCreateObject(String containerName, ObjectInit objectInit, String objectGUID)
        throws ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException {
        if (!defaultStorage.isExistingContainer(containerName)) {
            defaultStorage.createContainer(containerName);
        }
        if (!defaultStorage.isExistingFolder(containerName, objectInit.getType().getFolder())) {
            createFolder(containerName, objectInit.getType().getFolder());
        }
        objectInit.setId(objectGUID);
        objectTypeFor.put(objectGUID, objectInit.getType().getFolder());
        if (objectInit.getDigestAlgorithm() != null) {
            digestTypeFor.put(objectGUID, objectInit.getDigestAlgorithm());
        } else {
            digestTypeFor.put(objectGUID, DigestType.SHA256);
        }

        return objectInit;
    }

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException {
        defaultStorage.createFolder(containerName, folderName);
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart, boolean ending)
        throws IOException, ContentAddressableStorageException {
        // check container
        if (!defaultStorage.isExistingContainer(containerName)) {
            throw new ContentAddressableStorageException("Container does not exist");
        }
        // check the folder
        if (!defaultStorage.isExistingFolder(containerName, objectTypeFor.get(objectId))) {
            throw new ContentAddressableStorageException("Container's folder does not exist");
        }
        final String path = TMP_DIRECTORY + objectId;
        Digest messageDigest;
        try {
            messageDigest = new Digest(getDigestAlgoFor(objectId));
        } catch (final IllegalArgumentException exc) {
            LOGGER.error("Wrong digest algorithm " + getDigestAlgoFor(objectId).getName());
            throw new ContentAddressableStorageException(exc);
        }
        try (final InputStream digestObjectPart = messageDigest.getDigestInputStream(objectPart);
            FileOutputStream fOut = new FileOutputStream(path, true)) {
            // FIXME très très mauvaise pratique (si le fichier fait 2 To => 2 To en mémoire)
            fOut.write(ByteStreams.toByteArray(digestObjectPart));
            fOut.flush();
        } catch (final IOException exc) {
            LOGGER.error("Error on temporary file to transfert", exc);
            throw exc;
        }
        // ending remove it
        if (ending) {
            // FIXME double écriture !!! DigestInputStream est un InputStream, donc le passer directement en paramètre
            // de putObject
            // ou mieux : faire une écriture par bloc (buffer) et mettre à jour le Digest au fur et à mesure ainsi
            try (InputStream in = new FileInputStream(path)) {
                defaultStorage.putObject(containerName, objectTypeFor.get(objectId) + "/" + objectId, in);
                // do we validate the transfer before remove temp file ?
                Files.deleteIfExists(Paths.get(path));
                // TODO: to optimize (big file case) !
                final String digest = defaultStorage.computeObjectDigest(containerName,
                    objectTypeFor.get(objectId) + "/" + objectId, messageDigest.type());
                // remove digest algo
                digestTypeFor.remove(objectId);
                return digest;
            } catch (final IOException exc) {
                LOGGER.error("Error on temporary file to transfert", exc);
                throw exc;
            } catch (final ContentAddressableStorageException exc) {
                LOGGER.error("Error with storage service", exc);
                throw exc;
            }
        } else {
            // TODO: to optimize (big file case) !
            return BaseXx.getBase16(messageDigest.digest());
        }
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId) {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public JsonNode getCapacity(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        if (!defaultStorage.isExistingContainer(containerName)) {
            // FIXME logique incorrecte: ne devrait pas être créé dynamiquement mais uniquement si demandé
            // Devrait donc retourner une valeur du type NOT_EXIST
            try {
                defaultStorage.createContainer(containerName);
            } catch (final ContentAddressableStorageAlreadyExistException e) {
                // Log it but it's not a problem
                LOGGER.debug(e);
            }
        }
        final ObjectNode result = JsonHandler.createObjectNode();
        final ContainerInformation containerInformation = defaultStorage.getContainerInformation(containerName);
        result.put("usableSpace", containerInformation.getUsableSpace());
        result.put("usedSpace", containerInformation.getUsedSpace());
        result.put("tenantId", containerName);
        return result;
    }

    // FIXME : si cela avait été un enum spécifique, il n'y aurait pas le risque d'avoir un digest de type inconnu, ce
    // qui rend alors
    // le calcul faux sémantiquement ici (aurait dans ce cas dû générer une exception)
    private DigestType getDigestAlgoFor(String id) {
        return digestTypeFor.get(id) != null ? digestTypeFor.get(id) : DigestType.SHA256;
    }
}
