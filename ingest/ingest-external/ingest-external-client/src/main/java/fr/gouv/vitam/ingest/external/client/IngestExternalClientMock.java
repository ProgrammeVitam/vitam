/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.filesystem.FileSystem;

/**
 * Mock client implementation for IngestExternal
 */
public class IngestExternalClientMock implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientMock.class);
    private final IngestExternalConfiguration config = new IngestExternalConfiguration();
    private static final String DATA_FOLDER = "/tmp";
    private static final String SCRIPT_SCAN_CLAMAV = "scan-clamav.sh";

    @Override
    public void upload(InputStream stream){
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        GUID containerName = GUIDFactory.newGUID();
        GUID objectName = GUIDFactory.newGUID();
        FileSystem workspaceFileSystem = new FileSystem(new StorageConfiguration().setStoragePath(DATA_FOLDER));
        int result = 3;
            try {
                workspaceFileSystem.createContainer(containerName.toString());
                workspaceFileSystem.putObject(containerName.getId(), objectName.getId(), stream);
                String filePath = DATA_FOLDER + "/" + containerName.getId() + "/" + objectName.getId();
                result = JavaExecuteScript.executeCommand(SCRIPT_SCAN_CLAMAV,filePath);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.error("cannot create container");
            } catch (ContentAddressableStorageException e) {
                LOGGER.error("cannot store file");
            } catch (IngestExternalException e) {
                LOGGER.error("cannot scan virus!");
            } finally {
                try {
                    workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
                } catch (ContentAddressableStorageNotFoundException e) {
                    LOGGER.error("cannot find container");
                }
            }
            
        if (result == 0 || result == 1){
            LOGGER.debug(Response.Status.OK.getReasonPhrase());
        } else {
            LOGGER.debug(Response.Status.ACCEPTED.getReasonPhrase());
        }
        
    }

    @Override
    public Status status() {
        return Status.OK;
    }

}
