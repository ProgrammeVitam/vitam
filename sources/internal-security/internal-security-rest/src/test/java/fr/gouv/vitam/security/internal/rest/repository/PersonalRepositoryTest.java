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

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class PersonalRepositoryTest {

    private static final String CERTIFICATE_HASH = "2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7";
    public static final String PERSONAL_COLLECTION = "PersonalCertificate" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), PERSONAL_COLLECTION);

    private PersonalRepository personalRepository;

    private MongoCollection<Document> certificateCollection;

    @Before
    public void setUp() throws Exception {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        personalRepository = new PersonalRepository(mongoDbAccess, PERSONAL_COLLECTION);
        certificateCollection = mongoRule.getMongoCollection(PERSONAL_COLLECTION);
    }

    @Test
    public void should_store_certificate() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        personalCertificateModel.setIssuerDN("issuerDN");
        personalCertificateModel.setSubjectDN("distinguishedName");
        personalCertificateModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        personalCertificateModel.setId(id.toString());
        personalCertificateModel.setCertificateHash(CERTIFICATE_HASH);

        // When
        personalRepository.createPersonalCertificate(personalCertificateModel);

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("IssuerDN", "issuerDN")
            .containsEntry("SubjectDN", "distinguishedName")
            .containsEntry("SerialNumber", String.valueOf(10))
            .containsEntry("Hash", CERTIFICATE_HASH);
    }

    @Test
    public void should_find_identity() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        personalCertificateModel.setIssuerDN("issuerDN");
        personalCertificateModel.setSubjectDN("distinguishedName");
        personalCertificateModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        personalCertificateModel.setId(id.toString());
        personalCertificateModel.setCertificateHash(CERTIFICATE_HASH);

        personalRepository.createPersonalCertificate(personalCertificateModel);

        // When
        Optional<PersonalCertificateModel> result = personalRepository.findPersonalCertificateByHash(CERTIFICATE_HASH);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(identity -> {
                assertThat(identity.getIssuerDN()).isEqualTo("issuerDN");
                assertThat(identity.getSubjectDN()).isEqualTo("distinguishedName");
                assertThat(identity.getSerialNumber()).isEqualTo(String.valueOf(BigInteger.TEN));
                assertThat(identity.getId()).isEqualTo(id.toString());
            });
    }

    @Test
    public void should_return_empty_when_identity_is_missing() throws InvalidParseOperationException {
        // Given / When
        Optional<PersonalCertificateModel> result = personalRepository.findPersonalCertificateByHash("invalid_dn");

        // Then
        assertThat(result).isEmpty();
    }
}
