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
package fr.gouv.vitam.functional.administration.core.backup;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.functional.administration.common.exception.FunctionalBackupServiceException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.json.JsonHandler.createJsonGenerator;
import static fr.gouv.vitam.functional.administration.common.counter.SequenceType.fromFunctionalAdminCollections;

/**
 * Functional backupService
 */
public class FunctionalBackupService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FunctionalBackupService.class);
    public static final String FIELD_COLLECTION = "collection";
    public static final String FIELD_SEQUENCE = "sequence";
    public static final String FIELD_BACKUP_SEQUENCE = "backup_sequence";
    public static final String DEFAULT_EXTENSION = "json";
    private static final int DEFAULT_ADMIN_TENANT = VitamConfiguration.getAdminTenant();
    private final BackupService backupService;
    private final VitamCounterService vitamCounterService;
    private final BackupLogbookManager backupLogbookManager;

    public FunctionalBackupService(VitamCounterService vitamCounterService) {
        this.vitamCounterService = vitamCounterService;
        this.backupService = new BackupService();
        this.backupLogbookManager = new BackupLogbookManager();
    }

    @VisibleForTesting
    public FunctionalBackupService(
        BackupService backupService,
        VitamCounterService vitamCounterService,
        BackupLogbookManager backupLogbookManager
    ) {
        this.backupService = backupService;
        this.vitamCounterService = vitamCounterService;
        this.backupLogbookManager = backupLogbookManager;
    }

    /**
     * @param objectIdentifier
     * @param eipMaster logbookMaster
     * @param eventCode logbook evType
     * @param collection collection
     * @throws VitamException vitamException
     */
    public void saveCollectionAndSequence(
        GUID eipMaster,
        String eventCode,
        FunctionalAdminCollections collection,
        String objectIdentifier
    ) throws VitamException {
        try {
            // FIXME : Cross-tenant collections should only be updated using admin tenant (1).
            // There would be no need for hacking tenant here

            int initialTenant = ParameterHelper.getTenantParameter();
            int storageTenant = getStorageTenant(collection, initialTenant);

            VitamSequence backupSequence = vitamCounterService.getNextBackupSequenceDocument(
                storageTenant,
                fromFunctionalAdminCollections(collection)
            );

            String fileName = getBackupFileName(collection, storageTenant, backupSequence.getCounter());

            String digestStr;
            File file = null;
            try {
                // Force storage tenant to 1 for cross-tenant collections (impersonate admin tenant)
                VitamThreadUtils.getVitamSession().setTenantId(storageTenant);

                file = saveFunctionalCollectionToTempFile(collection, storageTenant, backupSequence);

                digestStr = storeBackupFileInStorage(fileName, file);
            } finally {
                // Restore initial tenant before logbook update (success or ko)
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);

                deleteTmpFile(file);
            }

            backupLogbookManager.logEventSuccess(eipMaster, eventCode, digestStr, fileName, objectIdentifier);
        } catch (ReferentialException | IOException | BackupServiceException e) {
            try {
                backupLogbookManager.logError(eipMaster, eventCode, e.getMessage());
            } catch (VitamException | RuntimeException logbookErrorException) {
                LOGGER.error("Could not persist backup error in logbook operation", logbookErrorException);
            }
            throw new FunctionalBackupServiceException("Could not backup collection " + collection, e);
        }
    }

    public void saveDocument(FunctionalAdminCollections collection, Document document)
        throws FunctionalBackupServiceException {
        try {
            int initialTenant = ParameterHelper.getTenantParameter();
            Integer storageTenant = document.getInteger("_tenant");

            String fileName = String.format("%d_%s.%s", storageTenant, document.get("_id"), DEFAULT_EXTENSION);

            InputStream is = null;

            try {
                // Force storage tenant to 1 for cross-tenant collections (impersonate admin tenant)
                is = new ByteArrayInputStream(BsonHelper.stringify(document).getBytes());
                VitamThreadUtils.getVitamSession().setTenantId(storageTenant);
                switch (collection) {
                    case ACCESSION_REGISTER_DETAIL:
                        storeBackupFileInStorage(fileName, is, DataCategory.ACCESSION_REGISTER_DETAIL);
                        break;
                    case ACCESSION_REGISTER_SYMBOLIC:
                        storeBackupFileInStorage(fileName, is, DataCategory.ACCESSION_REGISTER_SYMBOLIC);
                        break;
                    default:
                        storeBackupFileInStorage(fileName, is, DataCategory.BACKUP);
                        break;
                }
            } finally {
                // Restore initial tenant before logbook update (success or ko)
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);
                StreamUtils.closeSilently(is);
            }
        } catch (BackupServiceException e) {
            throw new FunctionalBackupServiceException("Could not backup document " + document.get("_id"), e);
        } catch (Exception e) {
            throw new FunctionalBackupServiceException("Could not backup collection " + collection.toString(), e);
        }
    }

    private void deleteTmpFile(File file) {
        if (file != null) {
            if (!file.delete()) {
                LOGGER.warn("Could not delete temp file {0}", file.getAbsolutePath());
            }
        }
    }

    /**
     * Forces "1" for cross-tenant collections.
     *
     * @param collection
     * @param sessionTenant
     * @return
     */
    private int getStorageTenant(FunctionalAdminCollections collection, int sessionTenant) {
        if (collection.isMultitenant()) return sessionTenant;
        else return DEFAULT_ADMIN_TENANT;
    }

    public static String getBackupFileName(
        FunctionalAdminCollections functionalAdminCollections,
        int tenant,
        Integer sequence
    ) {
        return String.format("%d_%s_%d.%s", tenant, functionalAdminCollections.getName(), sequence, DEFAULT_EXTENSION);
    }

    /**
     * save  file and log in logbook
     *
     * @param inputStream
     * @param eipMaster
     * @param eventCode
     * @param dataCategory
     * @param fileName
     * @throws VitamException
     */
    public void saveFile(
        InputStream inputStream,
        GUID eipMaster,
        String eventCode,
        DataCategory dataCategory,
        String fileName
    ) throws VitamException {
        final DigestType digestType = VitamConfiguration.getDefaultDigestType();
        final Digest digest = new Digest(digestType);
        InputStream digestInputStream = digest.getDigestInputStream(inputStream);

        // Save data to storage
        try {
            backupService.backup(digestInputStream, dataCategory, fileName);

            backupLogbookManager.logEventSuccess(eipMaster, eventCode, digest.digestHex(), fileName, null);
        } catch (BackupServiceException e) {
            LOGGER.error(e);
            backupLogbookManager.logError(eipMaster, eventCode, e.getMessage());
        }
    }

    private File saveFunctionalCollectionToTempFile(
        FunctionalAdminCollections collectionToSave,
        int tenant,
        VitamSequence backupSequence
    ) throws ReferentialException, IOException {
        String uniqueFileId = GUIDFactory.newGUID().getId();
        File file = PropertiesUtils.fileFromTmpFolder(uniqueFileId);

        try (
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
            JsonGenerator jsonGenerator = createJsonGenerator(buffOut)
        ) {
            /**
             * Backup format:
             * {
             *   "collection": [
             *      json_doc1,
             *      json_doc2,
             *      ...
             *   ],
             *   "sequence": { ... },
             *   "backup_sequence": { ... }
             * }
             */

            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName(FIELD_COLLECTION);
            jsonGenerator.writeStartArray();

            MongoCursor<Document> mongoCursor = getCurrentCollection(collectionToSave, tenant);

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                jsonGenerator.writeRawValue(BsonHelper.stringify(document));
            }

            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName(FIELD_SEQUENCE);
            VitamSequence sequence = vitamCounterService.getSequenceDocument(
                tenant,
                fromFunctionalAdminCollections(collectionToSave)
            );
            jsonGenerator.writeRawValue(BsonHelper.stringify(sequence));

            jsonGenerator.writeFieldName(FIELD_BACKUP_SEQUENCE);
            jsonGenerator.writeRawValue(BsonHelper.stringify(backupSequence));

            jsonGenerator.writeEndObject();

            jsonGenerator.flush();
        }

        return file;
    }

    /**
     * get the documents from functional admin collections
     *
     * @param collections
     * @param tenant
     * @return MongoCursor
     */
    private MongoCursor<Document> getCurrentCollection(FunctionalAdminCollections collections, int tenant) {
        return collections
            .getCollection()
            .find(getMangoFilter(collections, tenant))
            .sort(Sorts.ascending(VitamDocument.ID))
            .iterator();
    }

    /**
     * transfer the collection to json
     *
     * @param mongoCursor
     * @return ArrayNode
     * @throws InvalidParseOperationException
     */
    public ArrayNode getCollectionInJson(MongoCursor<Document> mongoCursor) throws InvalidParseOperationException {
        ArrayNode arrayNode = JsonHandler.createArrayNode();
        while (mongoCursor.hasNext()) {
            Document document = mongoCursor.next();
            arrayNode.add(JsonHandler.toJsonNode(document));
        }
        return arrayNode;
    }

    public ArrayNode getCollectionInJson(FunctionalAdminCollections collections, int tenant)
        throws InvalidParseOperationException {
        return getCollectionInJson(getCurrentCollection(collections, tenant));
    }

    private Bson getMangoFilter(FunctionalAdminCollections collectionToSave, int tenant) {
        if (collectionToSave.isMultitenant()) {
            // Filter by tenant
            return eq(VitamDocument.TENANT_ID, tenant);
        } else {
            // No filter
            return new BsonDocument();
        }
    }

    private String storeBackupFileInStorage(String fileName, File file) throws IOException, BackupServiceException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return storeBackupFileInStorage(fileName, fileInputStream, DataCategory.BACKUP);
        }
    }

    private String storeBackupFileInStorage(String fileName, InputStream is, DataCategory dataCategory)
        throws BackupServiceException {
        if (null == dataCategory) {
            dataCategory = DataCategory.BACKUP;
        }
        String digestStr;
        final DigestType digestType = VitamConfiguration.getDefaultDigestType();
        final Digest digest = new Digest(digestType);
        InputStream digestInputStream = digest.getDigestInputStream(is);

        // Save data to storage
        backupService.backup(digestInputStream, dataCategory, fileName);
        digestStr = digest.digestHex();

        return digestStr;
    }
}
