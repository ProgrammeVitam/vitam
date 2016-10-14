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
package fr.gouv.vitam.worker.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 * Worker IO Gestion Helper
 */
public class WorkerIOManagementHelper {

    // empty constructor
    private WorkerIOManagementHelper() {}

    /**
     * Get the File associated with this filename, trying in this order: as fullpath, as in Vitam Config Folder, as
     * Resources file
     * 
     * @param client workspace client
     * @param containerName container name
     * @param objectName object name
     * @param workerId worker id
     * @param optional if file is optional
     * @return file if found, if not found, null if optional
     * @throws FileNotFoundException if file is not found and not optional
     */
    public static final File findFileFromWorkspace(WorkspaceClient client, String containerName, String objectName,
        String workerId, boolean optional) throws FileNotFoundException {
        // First try as full path
        File file = PropertiesUtils.fileFromTmpFolder(containerName + "_" + workerId + "/" + objectName);
        // TODO : this optional situation would be treated later when lazy file loading is implemented
        if (optional) {
            try {
                if (file == null || !file.exists()) {
                    InputStream input = client.getObject(containerName, objectName);
                    file = PropertiesUtils.fileFromTmpFolder(containerName + "_" + workerId + "/" + objectName);
                    file.getParentFile().mkdirs();
                    IOUtils.copy(input, new FileOutputStream(file));
                }
            } catch (final ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                file = null;
            }
            if (file != null && !file.exists()) {
                file = null;
            }
        } else {
            try {
                if (!file.exists()) {
                    InputStream input = client.getObject(containerName, objectName);
                    file = PropertiesUtils.fileFromTmpFolder(containerName + "_" + workerId + "/" + objectName);
                    file.getParentFile().mkdirs();
                    IOUtils.copy(input, new FileOutputStream(file));
                }
            } catch (final ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                // need to rewrite the exception
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                throw new FileNotFoundException("File not found: " + objectName);
            }
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: " + objectName);
            }
        }
        return file;
    }

}
