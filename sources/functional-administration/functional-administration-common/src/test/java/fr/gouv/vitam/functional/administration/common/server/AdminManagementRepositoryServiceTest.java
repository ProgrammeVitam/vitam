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
package fr.gouv.vitam.functional.administration.common.server;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.ReferentialAccessionRegisterSummaryUtil;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryProvider;

public class AdminManagementRepositoryServiceTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @RunWithCustomExecutor
    @Test
    public void testSave() throws DatabaseException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String id = GUIDFactory.newIngestContractGUID(0).getId();
        VitamRepositoryProvider vitamRP = Mockito.mock(VitamRepositoryProvider.class);
        VitamMongoRepository vitamMongoRepository = Mockito.mock(VitamMongoRepository.class);
        VitamElasticsearchRepository vitamElasticsearchRepository = Mockito.mock(VitamElasticsearchRepository.class);
        AdminManagementRepositoryService adminManagementRS =
            new AdminManagementRepositoryService(vitamRP, new ReferentialAccessionRegisterSummaryUtil());

        Mockito.when(vitamRP.getVitamMongoRepository(Matchers.any())).thenReturn(vitamMongoRepository);
        Mockito.when(vitamMongoRepository.saveOrUpdate(Matchers.any(Document.class)))
            .thenReturn(VitamRepositoryStatus.CREATED);
        Mockito.when(vitamRP.getVitamESRepository(Matchers.any()))
            .thenReturn(vitamElasticsearchRepository);
        Mockito.when(vitamElasticsearchRepository.saveOrUpdate(Matchers.any(Document.class)))
            .thenReturn(VitamRepositoryStatus.CREATED);

        FindIterable<Document> findIterableDocuments = Mockito.mock(FindIterable.class);
        Mockito.when(findIterableDocuments.iterator()).thenReturn(new EmptyMongoCursor<>());
        Mockito
            .when(vitamMongoRepository.findByFieldsDocuments(Matchers.anyMap(), Matchers.anyInt(), Matchers.anyInt()))
            .thenReturn(findIterableDocuments);

        JsonNode accessionRegisterItem = getAccessionRegisterDetail(id);

        assertThatCode(() -> {
            adminManagementRS.save(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, accessionRegisterItem, 0);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testGet() throws DatabaseException, InvalidParseOperationException, FileNotFoundException {
        Document document = Document.parse(JsonHandler.unprettyPrint(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("accession-register-detail.json"))));

        VitamRepositoryProvider vitamRP = Mockito.mock(VitamRepositoryProvider.class);
        VitamMongoRepository vitamMongoRepository = Mockito.mock(VitamMongoRepository.class);
        VitamElasticsearchRepository vitamElasticsearchRepository = Mockito.mock(VitamElasticsearchRepository.class);
        AdminManagementRepositoryService adminManagementRS =
            new AdminManagementRepositoryService(vitamRP, new ReferentialAccessionRegisterSummaryUtil());

        Mockito.when(vitamRP.getVitamMongoRepository(Matchers.any())).thenReturn(vitamMongoRepository);
        Mockito.when(vitamMongoRepository.getByID(Matchers.any(), Matchers.any())).thenReturn(Optional.of(document));
        Mockito.when(vitamRP.getVitamESRepository(Matchers.any()))
            .thenReturn(vitamElasticsearchRepository);
        Mockito.when(vitamElasticsearchRepository.saveOrUpdate(Matchers.any(Document.class)))
            .thenReturn(VitamRepositoryStatus.CREATED);

        JsonNode accessionRegisterItem = JsonHandler.createObjectNode();
        assertThatCode(() -> {
            adminManagementRS.save(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                accessionRegisterItem, 0);
        }).doesNotThrowAnyException();
    }

    private JsonNode getAccessionRegisterDetail(String id) throws InvalidParseOperationException {
        AccessionRegisterDetail accessionRegister = new AccessionRegisterDetail();
        accessionRegister.setId(id);
        accessionRegister.setOriginatingAgency("FRAN_NP_005568");
        accessionRegister.setSubmissionAgency("FRAN_NP_005061");
        accessionRegister.setAcquisitionInformation("Versement");
        accessionRegister.setLegalStatus("Public Archives");
        accessionRegister.setArchivalAgreement("archivalAgreement");
        accessionRegister.setStartDate("2016-11-04T21:40:47.912+01:00");
        accessionRegister.setEndDate("2016-11-04T21:40:47.912+01:00");
        accessionRegister.setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED);
        accessionRegister.setOperationIds(Arrays.asList(id));
        accessionRegister
            .setTotalObjectGroups(new RegisterValueDetailModel().setIngested(3).setDeleted(0).setRemained(3));
        accessionRegister.setTotalUnits(new RegisterValueDetailModel().setIngested(3).setDeleted(0).setRemained(3));
        accessionRegister.setTotalObjects(new RegisterValueDetailModel().setIngested(12).setDeleted(0).setRemained(12));
        accessionRegister
            .setObjectSize(new RegisterValueDetailModel().setIngested(1035126).setDeleted(0).setRemained(1035126));
        return JsonHandler.toJsonNode(accessionRegister);
    }

}
