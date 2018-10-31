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
package fr.gouv.vitam.common.storage.cas.container.api;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

/**
 * Abstract class of CAS that contains common methos
 */
public abstract class ContentAddressableStorageAbstract implements ContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContentAddressableStorageAbstract.class);

    /**
     * Max result for listing option TODO: have to be configurable ?
     */
    public static final int LISTING_MAX_RESULTS = 100;

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(),
            algo);
        Response response = null;
        InputStream stream = null;
        try  {
            response = getObject(containerName, objectName);
            stream = response.readEntity(InputStream.class);
            final Digest digest = new Digest(algo);
            digest.update(stream);
            return digest.toString();
        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            StreamUtils.closeSilently(stream);
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }
}
