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
package fr.gouv.vitam.functional.administration.format.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * ReferentialFormatFileImpl implementing the ReferentialFormatFile interface
 */
public class ReferentialFormatFileImpl implements ReferentialFile<FileFormat>, VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialFormatFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String COLLECTION_NAME = "FileFormat";

    private static final String STP_REFERENTIAL_FORMAT_IMPORT = "STP_REFERENTIAL_FORMAT_IMPORT";
    private static final String VERSION = " version ";
    private static final String FILE_PRONOM = " du fichier de signature PRONOM (DROID_SignatureFile)";

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access for reference format configuration
     */
    public ReferentialFormatFileImpl(MongoDbAccessAdminImpl dbConfiguration) {
        mongoAccess = dbConfiguration;
    }

    @Override
    public void importFile(InputStream xmlPronom) throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        final ArrayNode pronomList = this.checkFile(xmlPronom);
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            final GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
                .newLogbookOperationParameters(eip, STP_REFERENTIAL_FORMAT_IMPORT, eip,
                    LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(STP_REFERENTIAL_FORMAT_IMPORT, StatusCode.STARTED), eip);
            try {
                client.create(logbookParametersStart);
            } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                LogbookClientServerException e) {
                LOGGER.error(e);
                throw new ReferentialException(e);
            }

            final GUID eip1 = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            try {
                if (mongoAccess.getMongoDatabase().getCollection(COLLECTION_NAME).count() == 0) {
                    mongoAccess.insertDocuments(pronomList, FunctionalAdminCollections.FORMATS);

                    final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                        .newLogbookOperationParameters(eip1, STP_REFERENTIAL_FORMAT_IMPORT, eip,
                            LogbookTypeProcess.MASTERDATA, StatusCode.OK,
                            VitamLogbookMessages.getCodeOp(STP_REFERENTIAL_FORMAT_IMPORT, StatusCode.OK) + VERSION +
                                pronomList.get(0).get("VersionPronom").textValue() + FILE_PRONOM,
                            eip1);

                    try {
                        client.update(logbookParametersEnd);
                    } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                        LogbookClientServerException e) {
                        LOGGER.error(e);
                        throw new ReferentialException(e);
                    }
                } else {
                    final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                        .newLogbookOperationParameters(eip1, STP_REFERENTIAL_FORMAT_IMPORT, eip,
                            LogbookTypeProcess.MASTERDATA, StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(STP_REFERENTIAL_FORMAT_IMPORT, StatusCode.KO), eip1);
                    try {
                        client.update(logbookParametersEnd);
                    } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                        LogbookClientServerException e) {
                        LOGGER.error(e);
                        throw new ReferentialException(e);
                    }

                    throw new DatabaseConflictException("File format collection is not empty");
                }
            } catch (final ReferentialException e) {
                LOGGER.error(e.getMessage());
                final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                    .newLogbookOperationParameters(eip1, STP_REFERENTIAL_FORMAT_IMPORT, eip,
                        LogbookTypeProcess.MASTERDATA, StatusCode.KO,
                        VitamLogbookMessages.getCodeOp(STP_REFERENTIAL_FORMAT_IMPORT, StatusCode.KO), eip1);
                try {
                    client.update(logbookParametersEnd);
                } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                    LogbookClientServerException e1) {
                    LOGGER.error(e1);
                    throw new ReferentialException(e1);
                }
                throw new ReferentialException(e);
            }
        }
    }

    @Override
    public ArrayNode checkFile(InputStream xmlPronom) throws ReferentialException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        /*
         * Deserialize as json arrayNode, this operation will will ensure the format is valid first, else Exception is
         * thrown
         */
        ArrayNode deserializeFormatsAsJson = PronomParser.getPronom(xmlPronom);
        StreamUtils.closeSilently(xmlPronom);
        return deserializeFormatsAsJson;
    }

    @Override
    public FileFormat findDocumentById(String id) throws ReferentialException {
        try {
            return (FileFormat) mongoAccess.getDocumentById(id, FunctionalAdminCollections.FORMATS);
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

    @Override
    public List<FileFormat> findDocuments(JsonNode select) throws FileFormatNotFoundException, ReferentialException {
        try (final MongoCursor<VitamDocument<?>> formats = mongoAccess.findDocuments(select,
            FunctionalAdminCollections.FORMATS)) {

            final List<FileFormat> result = new ArrayList<>();
            if (formats == null || !formats.hasNext()) {
                throw new FileFormatNotFoundException("Format not found");
            }

            while (formats.hasNext()) {
                result.add((FileFormat) formats.next());
            }

            return result;
        } catch (final FileFormatNotFoundException e) {
            throw e;
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

    @Override
    public void close() {
        // Empty
    }
}
