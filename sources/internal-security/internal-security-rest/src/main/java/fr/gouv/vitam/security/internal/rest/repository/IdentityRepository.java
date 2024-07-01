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
package fr.gouv.vitam.security.internal.rest.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

/**
 * repository for identity certificate entities  management in mongo.
 */
public class IdentityRepository implements CertificateRepository, CertificateCRLCheckStateUpdater<IdentityModel> {

    public static final String CERTIFICATE_COLLECTION = "Certificate";

    private final MongoCollection<Document> identityCollection;

    private final CertificateCRLCheckRepositoryHelper crlRepositoryHelper;

    @VisibleForTesting
    public IdentityRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        identityCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
        crlRepositoryHelper = new CertificateCRLCheckRepositoryHelper(identityCollection);
    }

    public IdentityRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, CERTIFICATE_COLLECTION);
    }

    /**
     * create a certificate with contextId and many information
     *
     * @param identityModel
     * @throws InvalidParseOperationException
     */
    public void createIdentity(IdentityModel identityModel) throws InvalidParseOperationException {
        String json = JsonHandler.writeAsString(identityModel);
        identityCollection.insertOne(Document.parse(json));
    }

    /**
     * return certificate according to subjectDN and serilanumber
     *
     * @param subjectDN
     * @param serialNumber
     * @return
     * @throws InvalidParseOperationException
     */
    public Optional<IdentityModel> findIdentity(String subjectDN, String serialNumber)
        throws InvalidParseOperationException {
        FindIterable<Document> models = identityCollection.find(
            filterBySubjectDNAndSerialNumber(subjectDN, serialNumber)
        );
        Document first = models.first();
        if (first == null) {
            return Optional.empty();
        }
        return Optional.of(BsonHelper.fromDocumentToObject(first, IdentityModel.class));
    }

    public List<IdentityModel> findAll() throws InvalidParseOperationException {
        List<IdentityModel> result = new ArrayList<>();
        FindIterable<Document> models = identityCollection
            .find()
            .projection(Projections.exclude(IdentityModel.CERTIFICATE_TAG));
        for (Document model : models) {
            result.add(BsonHelper.fromDocumentToObject(model, IdentityModel.class));
        }
        return result;
    }

    /**
     * @param subjectDN
     * @param contextId
     * @param serialNumber
     */
    public void linkContextToIdentity(String subjectDN, String contextId, String serialNumber) {
        identityCollection.updateOne(
            filterBySubjectDNAndSerialNumber(subjectDN, serialNumber),
            set("ContextId", contextId)
        );
    }

    private Bson filterBySubjectDNAndSerialNumber(String subjectDN, String serialNumber) {
        return and(
            eq("SubjectDN", subjectDN),
            eq("SerialNumber", serialNumber),
            eq(CertificateBaseModel.STATUS_TAG, CertificateStatus.VALID.name())
        );
    }

    /**
     * Check if a context is used
     *
     * @param contextId
     * @return true if the context is used by Identity
     */
    public boolean contextIsUsed(String contextId) {
        return identityCollection.countDocuments(eq("ContextId", contextId)) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FindIterable<Document> findCertificate(String issuerDN, CertificateStatus certificateStatus) {
        return crlRepositoryHelper.findCertificate(issuerDN, certificateStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCertificateState(List<String> certificatesToUpdate, CertificateStatus certificateStatus) {
        crlRepositoryHelper.updateCertificateState(certificatesToUpdate, certificateStatus);
    }

    @Override
    public Class<IdentityModel> getEntityModelType() {
        return IdentityModel.class;
    }
}
