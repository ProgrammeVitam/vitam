/**
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
 */
package fr.gouv.vitam.security.internal.rest.repository;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.security.internal.rest.SimpleMongoDBAccess;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.security.internal.rest.repository.IdentityRepository.CERTIFICATE_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test @IdentityRepository
 */
public class IdentityRepositoryTest {

    private final static String CLUSTER_NAME = "vitam-cluster";

    @Rule
    public MongoRule mongoRule = new MongoRule(getMongoClientOptions(), CLUSTER_NAME, CERTIFICATE_COLLECTION);

    private IdentityRepository identityRepository;

    private MongoCollection<Document> certificateCollection;

    @Before
    public void setUp() throws Exception {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), CLUSTER_NAME);
        identityRepository = new IdentityRepository(mongoDbAccess);
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
        identityModel.setSerialNumber(BigInteger.TEN);
        identityModel.setId(id.toString());

        // When
        identityRepository.createIdentity(identityModel);

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("IssuerDN", "issuerDN")
            .containsEntry("SubjectDN", "distinguishedName")
            .containsEntry("SerialNumber", 10);
    }

    @Test
    public void should_find_identity() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("1");
        identityModel.setIssuerDN("issuerDN");
        identityModel.setSubjectDN("distinguishedName");
        identityModel.setSerialNumber(BigInteger.TEN);
        identityModel.setId(id.toString());

        identityRepository.createIdentity(identityModel);

        // When
        Optional<IdentityModel> result = identityRepository.findIdentity("distinguishedName", BigInteger.TEN);

        // Then
        assertThat(result).isPresent().hasValueSatisfying(identity -> {
            assertThat(identity.getIssuerDN()).isEqualTo("issuerDN");
            assertThat(identity.getSubjectDN()).isEqualTo("distinguishedName");
            assertThat(identity.getSerialNumber()).isEqualTo(BigInteger.TEN);
            assertThat(identity.getId()).isEqualTo(id.toString());
        });
    }

    @Test
    public void should_return_empty_when_identity_is_missing() throws InvalidParseOperationException {
        // Given / When
        Optional<IdentityModel> result = identityRepository.findIdentity("invalid_dn", BigInteger.ZERO);

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
        identityModel.setSerialNumber(BigInteger.TEN);
        identityModel.setId(id.toString());

        identityRepository.createIdentity(identityModel);

        // When
        identityModel.setContextId(contextId);
        identityRepository.linkContextToIdentity(identityModel.getSubjectDN(), identityModel.getContextId(),
            identityModel.getSerialNumber());

        // Then
        Document document = certificateCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("ContextId", contextId);
    }

}