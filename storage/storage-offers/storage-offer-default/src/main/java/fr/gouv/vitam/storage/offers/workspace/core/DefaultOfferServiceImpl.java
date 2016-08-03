/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.offers.workspace.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.ByteStreams;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
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

    private Map<String, DigestType> digestTypeFor;

    private DefaultOfferServiceImpl() {
        StorageConfiguration configuration;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                StorageConfiguration.class);
        } catch (IOException exc) {
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = new FileSystem(configuration);
        digestTypeFor = new HashMap<>();
    }

    public static DefaultOfferService getInstance() {
        return INSTANCE;
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        return defaultStorage.computeObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public InputStream getObject(String id) {
        throw new UnsupportedOperationException("getObject not actually implemented");
    }

    @Override
    public ObjectInit createContainer(String containerName, ObjectInit objectInit, String objectGUID)
        throws ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException {
        if (!defaultStorage.isExistingContainer(containerName)) {
            defaultStorage.createContainer(containerName);
        }
        objectInit.setId(objectGUID);
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
        ContentAddressableStorageAlreadyExistException {
        defaultStorage.createFolder(containerName, folderName);
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart, boolean ending)
        throws IOException, ContentAddressableStorageException {
        // check container
        if (!defaultStorage.isExistingContainer(containerName)) {
            throw new ContentAddressableStorageException("Container does not exist");
        }
        String path = TMP_DIRECTORY + objectId;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(getDigestAlgoFor(objectId).getName());
        } catch(NoSuchAlgorithmException exc) {
            LOGGER.error("Wrong digest algorithm " + getDigestAlgoFor(objectId).getName());
            throw new ContentAddressableStorageException(exc);
        }
        DigestInputStream digestObjectPArt = new DigestInputStream(objectPart, messageDigest);
        try(FileOutputStream fOut = new FileOutputStream(path, true)) {
            fOut.write(ByteStreams.toByteArray(digestObjectPArt));
            fOut.flush();
        } catch (IOException exc) {
            LOGGER.error("Error on temporary file to transfert", exc);
            throw exc;
        }
        // ending remove it
        if (ending) {
            try (InputStream in = new FileInputStream(path)) {
                defaultStorage.putObject(containerName, objectId, in);
                // do we validate the transfer before remove temp file ?
                Files.deleteIfExists(Paths.get(path));
                // TODO: to optimize (big file case) !
                String digest = defaultStorage.computeObjectDigest(containerName, objectId, getDigestAlgoFor(objectId));
                // remove digest algo
                digestTypeFor.remove(objectId);
                return digest;
            } catch (IOException exc) {
                LOGGER.error("Error on temporary file to transfert", exc);
                throw exc;
            } catch (ContentAddressableStorageException exc) {
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

    private DigestType getDigestAlgoFor(String id) {
        return digestTypeFor.get(id) != null ? digestTypeFor.get(id) : DigestType.SHA256;
    }
}
