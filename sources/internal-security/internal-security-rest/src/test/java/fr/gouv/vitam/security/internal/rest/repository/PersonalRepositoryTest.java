package fr.gouv.vitam.security.internal.rest.repository;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.security.internal.rest.repository.PersonalRepository.PERSONAL_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;

public class PersonalRepositoryTest {
    private static final String CERTIFICATE_HASH =
        "2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7";
    public static final String PERSONAL_COLLECTION = "PersonalCertificate" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(getMongoClientOptions(), PERSONAL_COLLECTION);

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
        personalCertificateModel.setCertificateHash(
            CERTIFICATE_HASH);


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
        personalCertificateModel.setCertificateHash(
            CERTIFICATE_HASH);

        personalRepository.createPersonalCertificate(personalCertificateModel);

        // When
        Optional<PersonalCertificateModel> result =
            personalRepository
                .findPersonalCertificateByHash(
                    CERTIFICATE_HASH);

        // Then
        assertThat(result).isPresent().hasValueSatisfying(identity -> {
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
