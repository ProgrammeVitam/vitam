package fr.gouv.vitam.common.storage.cas.container.api;

import java.io.IOException;
import java.io.InputStream;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;

/**
 * Abstract class of CAS that contains common methos 
 * 
 */
public abstract class ContentAddressableStorageAbstract implements ContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContentAddressableStorageAbstract.class);
    
    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(),
            algo);
        try (final InputStream stream = (InputStream) getObject(containerName, objectName).getEntity()) {
            final Digest digest = new Digest(algo);
            digest.update(stream);
            return digest.toString();
        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean checkObject(String containerName, String objectId, String digest,
        DigestType digestAlgorithm) throws ContentAddressableStorageException {
        String offerDigest = computeObjectDigest(containerName, objectId, digestAlgorithm);
        return offerDigest.equals(digest);
    }
    
    
}
