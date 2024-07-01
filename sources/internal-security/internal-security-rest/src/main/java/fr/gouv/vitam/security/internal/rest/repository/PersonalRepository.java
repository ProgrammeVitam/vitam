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
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * store Personal certificate in mongo.
 */
public class PersonalRepository
    implements CertificateRepository, CertificateCRLCheckStateUpdater<PersonalCertificateModel> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PersonalRepository.class);

    public static final String PERSONAL_COLLECTION = "PersonalCertificate";

    private final MongoCollection<Document> personnalCollection;

    private final CertificateCRLCheckRepositoryHelper crlRepositoryHelper;

    @VisibleForTesting
    public PersonalRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        personnalCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
        crlRepositoryHelper = new CertificateCRLCheckRepositoryHelper(personnalCollection);
    }

    public PersonalRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, PERSONAL_COLLECTION);
    }

    /**
     * create a personal certificate
     *
     * @param personalCertificateModel
     * @throws InvalidParseOperationException
     */
    public void createPersonalCertificate(PersonalCertificateModel personalCertificateModel)
        throws InvalidParseOperationException {
        String json = JsonHandler.writeAsString(personalCertificateModel);

        personnalCollection.insertOne(Document.parse(json));
    }

    /**
     * return certificate by hash
     *
     * @param hash
     * @return
     * @throws InvalidParseOperationException
     */
    public Optional<PersonalCertificateModel> findPersonalCertificateByHash(String hash)
        throws InvalidParseOperationException {
        FindIterable<Document> models = personnalCollection.find(
            and(
                eq(PersonalCertificateModel.TAG_HASH, hash),
                eq(PersonalCertificateModel.STATUS_TAG, CertificateStatus.VALID.name())
            )
        );

        Document first = models.first();

        if (first == null) {
            return Optional.empty();
        }

        return Optional.of(BsonHelper.fromDocumentToObject(first, PersonalCertificateModel.class));
    }

    public List<PersonalCertificateModel> findAll() throws InvalidParseOperationException {
        List<PersonalCertificateModel> result = new ArrayList<>();
        FindIterable<Document> models = personnalCollection
            .find()
            .projection(Projections.exclude(IdentityModel.CERTIFICATE_TAG));
        for (Document model : models) {
            result.add(BsonHelper.fromDocumentToObject(model, PersonalCertificateModel.class));
        }
        return result;
    }

    /**
     * return certificate by hash
     *
     * @param hash
     */
    public void deletePersonalCertificate(String hash) {
        DeleteResult deleteResult = personnalCollection.deleteOne(eq(PersonalCertificateModel.TAG_HASH, hash));
        LOGGER.debug("Deleted document count: " + deleteResult.getDeletedCount());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FindIterable<Document> findCertificate(String issuerDN, CertificateStatus certificateStatus)
        throws InvalidParseOperationException {
        return crlRepositoryHelper.findCertificate(issuerDN, certificateStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCertificateState(List<String> certificatesToUpdate, CertificateStatus certificateStatus) {
        crlRepositoryHelper.updateCertificateState(certificatesToUpdate, certificateStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PersonalCertificateModel> getEntityModelType() {
        return PersonalCertificateModel.class;
    }
}
