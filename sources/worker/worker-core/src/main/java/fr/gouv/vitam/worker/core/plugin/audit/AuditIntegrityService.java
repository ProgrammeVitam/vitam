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
package fr.gouv.vitam.worker.core.plugin.audit;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.StorageJson;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.audit.exception.AuditException;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObject;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;

/**
 * AuditIntegrityService
 */
public class AuditIntegrityService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuditIntegrityService.class);

    public static final String CHECK_INTEGRITY_ID = "AUDIT_FILE_INTEGRITY";
    private static final String PHYSICAL_MASTER = "PhysicalMaster";

    private final StorageClientFactory storageClientFactory;

    public AuditIntegrityService() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public AuditIntegrityService(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * Check the integrity of all binary objects in GOT.
     * 
     * @param gotDetail got details
     * @return result of integrity check
     * @throws AuditException exception
     */
    public AuditCheckObjectGroupResult check(AuditObjectGroup gotDetail) throws AuditException {
        AuditCheckObjectGroupResult result = new AuditCheckObjectGroupResult();
        result.setIdObjectGroup(gotDetail.getId());

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            for (AuditObject object : gotDetail.getObjects()) {
                if (PHYSICAL_MASTER.equals(object.getQualifier())) {
                    continue;
                }

                AuditCheckObjectResult auditCheckObjectResult = new AuditCheckObjectResult();
                auditCheckObjectResult.setIdObject(object.getId());
                StorageJson storageInformation = object.getStorage();
                JsonNode offerToMetadata = storageClient.getInformation(storageInformation.getStrategyId(),
                        DataCategory.OBJECT, object.getId(), storageInformation.getOfferIds(), true);
                for (String offerId : storageInformation.getOfferIds()) {
                    JsonNode metadata = offerToMetadata.findValue(offerId);
                    if (metadata == null || object.getMessageDigest() == null || !metadata.has("digest")) {
                        auditCheckObjectResult.getOfferStatuses().put(offerId, StatusCode.KO);
                    } else {
                        String digest = metadata.get("digest").asText();
                        if (StringUtils.equals(object.getMessageDigest(), digest)) {
                            auditCheckObjectResult.getOfferStatuses().put(offerId, StatusCode.OK);
                        } else {
                            auditCheckObjectResult.getOfferStatuses().put(offerId, StatusCode.KO);
                        }
                    }
                }
                result.getObjectStatuses().add(auditCheckObjectResult);
            }
            result.setStatus(result.getObjectsGlobalStatus());
        } catch (StorageClientException e) {
            LOGGER.error("Storage server errors : ", e);
            throw new AuditException(StatusCode.FATAL, String.format("Storage server errors : %s", e));
        }

        if (result.getStatus() == null) {
            result.setStatus(StatusCode.OK);
        }

        return result;
    }

}
