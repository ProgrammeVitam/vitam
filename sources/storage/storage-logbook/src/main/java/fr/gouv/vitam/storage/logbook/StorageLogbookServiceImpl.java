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
package fr.gouv.vitam.storage.logbook;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.LogInformation;
import fr.gouv.vitam.storage.StorageLogAppender;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.logbook.parameters.StorageLogbookParameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Use as a singleton
 * Implementation of the mock of the storage logbook Only log informations
 */
public class StorageLogbookServiceImpl implements StorageLogbookService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogbookServiceImpl.class);
    private StorageLogAppender appender;

    public StorageLogbookServiceImpl(List<Integer> tenants, Path path) throws IOException {
        appender = new StorageLogAppender(tenants, path);
    }


    @Override
    public void append(Integer tenant, StorageLogbookParameters parameters) throws StorageException, IOException {
        appender.append(tenant, parameters);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<StorageLogbookParameters> selectOperationsbyObjectId(String objectId) throws StorageException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<StorageLogbookParameters> selectOperationsbyObjectGroupId(String objectGroupId)
        throws StorageException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<StorageLogbookParameters> selectOperationsWithASelect(JsonNode select)
        throws StorageException, InvalidParseOperationException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public LogInformation generateSecureStorage(Integer tenantId) throws IOException {
        return appender.secureAndCreateNewlogByTenant(tenantId);

    }
    //FIXME Secure when server restart
    @Override
    public void stopAppenderLoggerAndSecureLastLogs(Integer tenantId) throws IOException {

        LogInformation info = appender.secureWithoutCreatingNewLogByTenant(tenantId);

    }


}
