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
package fr.gouv.vitam.security.internal.rest.repository;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;


import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;

/**
 * store Personal certificate in mongo.
 */
public class PersonalRepository {
    public static final String CERTIFICATE_COLLECTION = "PersonalCertificate";

    private final MongoCollection<Document> personnalCollection;

    public PersonalRepository(MongoDbAccess mongoDbAccess) {
        personnalCollection = mongoDbAccess.getMongoDatabase().getCollection(CERTIFICATE_COLLECTION);
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
    public Optional<PersonalCertificateModel> findIPersonalCertificateByHash(String hash)
        throws InvalidParseOperationException {

        FindIterable<Document> models = personnalCollection.find(eq(PersonalCertificateModel.TAG_HASH, hash));

        Document first = models.first();

        if (first == null) {
            return Optional.empty();
        }

        return Optional.of(JsonHandler.getFromString(first.toJson(), PersonalCertificateModel.class));
    }

}
