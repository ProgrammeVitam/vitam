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
package fr.gouv.vitam.security.internal.rest.service.migration;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import org.bson.Document;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Updates.set;

/**
 * Repository for mongo security data migration
 */
public class SecurityDataMigrationRepository {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecurityDataMigrationRepository.class);

    private final MongoCollection<Document> identityCertificateCollection;
    private final MongoCollection<Document> personalCertificateCollection;

    public SecurityDataMigrationRepository(MongoDbAccess mongoDbAccess) {
        this.identityCertificateCollection = mongoDbAccess
            .getMongoDatabase()
            .getCollection(IdentityRepository.CERTIFICATE_COLLECTION);

        this.personalCertificateCollection = mongoDbAccess
            .getMongoDatabase()
            .getCollection(PersonalRepository.PERSONAL_COLLECTION);
    }

    public void migrateCertificatesData(CertificateStatus certificateStatus) {
        addCertificateStateField(identityCertificateCollection, certificateStatus);
        addCertificateStateField(personalCertificateCollection, certificateStatus);
    }

    /**
     * add field "Status" to certificate's document
     */
    private void addCertificateStateField(
        MongoCollection<Document> certificateMongoCollection,
        CertificateStatus certificateStatus
    ) {
        LOGGER.info(
            "About to migrate certificates for {} collection",
            certificateMongoCollection.getNamespace().getFullName()
        );

        certificateMongoCollection.updateMany(
            exists(CertificateBaseModel.STATUS_TAG, false),
            set(CertificateBaseModel.STATUS_TAG, certificateStatus.name())
        );

        LOGGER.info(
            "Certificate's record successfully migrated for {} collection",
            certificateMongoCollection.getNamespace().getFullName()
        );
    }
}
