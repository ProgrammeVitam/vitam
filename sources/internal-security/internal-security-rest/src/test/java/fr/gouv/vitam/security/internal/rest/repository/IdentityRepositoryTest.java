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
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test @IdentityRepository
 */
public class IdentityRepositoryTest {

    private static final String CERTIFICATE_COLLECTION = "Certificate" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), CERTIFICATE_COLLECTION);

    private IdentityRepository identityRepository;

    private MongoCollection<Document> certificateCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        identityRepository = new IdentityRepository(mongoDbAccess, CERTIFICATE_COLLECTION);
        certificateCollection = mongoRule.getMongoCollection(CERTIFICATE_COLLECTION);
    }

    @Test
    public void should_store_certificate() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("1");
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        identityModel.setId(id.toString());

        // When
        identityRepository.createIdentity(identityModel);

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("IssuerDN", "issuerDN")
            .containsEntry("SubjectDN", "distinguishedName")
            .containsEntry("SerialNumber", "10");
    }

    @Test
    public void should_find_identity() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("1");
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        identityModel.setId(id.toString());

        identityRepository.createIdentity(identityModel);

        // When
        Optional<IdentityModel> result = identityRepository.findIdentity(
            "distinguishedName",
            String.valueOf(BigInteger.TEN)
        );

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
        Optional<IdentityModel> result = identityRepository.findIdentity("invalid_dn", String.valueOf(BigInteger.ZERO));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_update_identity_with_contextId() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();
        String contextId = "123456";

        IdentityModel identityModel = new IdentityModel();
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        identityModel.setId(id.toString());

        identityRepository.createIdentity(identityModel);

        // When
        identityModel.setContextId(contextId);
        identityRepository.linkContextToIdentity(
            identityModel.getSubjectDN(),
            identityModel.getContextId(),
            identityModel.getSerialNumber()
        );

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document).isNotNull().containsEntry("ContextId", contextId);
    }

    @Test
    public void shouldFindContextIsUsed() throws InvalidParseOperationException {
        final String CONTEXT_ID = "1";

        // Given
        GUID id = GUIDFactory.newGUID();

        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId(CONTEXT_ID);
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber(String.valueOf(BigInteger.TEN));
        identityModel.setId(id.toString());

        identityRepository.createIdentity(identityModel);

        // When / Then
        assertTrue(identityRepository.contextIsUsed(CONTEXT_ID));
    }

    @Test
    public void should_store_certificate_with_big_serial_number() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("1");
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber("127890284121982523460526876445101669455454");
        identityModel.setId(id.toString());

        // When
        identityRepository.createIdentity(identityModel);

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("IssuerDN", "issuerDN")
            .containsEntry("SubjectDN", "distinguishedName")
            .containsEntry("SerialNumber", "127890284121982523460526876445101669455454");
    }
}
