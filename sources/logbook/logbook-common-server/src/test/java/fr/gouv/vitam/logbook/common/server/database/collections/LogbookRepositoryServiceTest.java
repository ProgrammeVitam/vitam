/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.logbook.common.server.database.collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.json.JsonHandler;

public class LogbookRepositoryServiceTest {

    @Test
    public void testSaveBulk() throws DatabaseException {
        
        VitamRepositoryProvider vitamRepositoryProvider = Mockito.mock(VitamRepositoryProvider.class);
        VitamMongoRepository vitamMongoRepository = Mockito.mock(VitamMongoRepository.class);
        VitamElasticsearchRepository vitamElasticsearchRepository = Mockito.mock(VitamElasticsearchRepository.class);
        LogbookRepositoryService logbookRepositoryService = new LogbookRepositoryService(vitamRepositoryProvider);
        
        Mockito.when(vitamRepositoryProvider.getVitamMongoRepository(any())).thenReturn(vitamMongoRepository);
        Mockito.doNothing().when(vitamMongoRepository).saveOrUpdate(any(List.class));
        Mockito.when(vitamRepositoryProvider.getVitamESRepository(any())).thenReturn(vitamElasticsearchRepository);
        Mockito.doNothing().when(vitamElasticsearchRepository).save(any(List.class));

        List<JsonNode> logbookItems = new ArrayList<>();
        logbookItems.add(JsonHandler.createObjectNode());
        logbookItems.add(JsonHandler.createObjectNode());
        assertThatCode(() -> {
            logbookRepositoryService.saveBulk(LogbookCollections.LIFECYCLE_UNIT, logbookItems);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            logbookRepositoryService.saveBulk(LogbookCollections.LIFECYCLE_OBJECTGROUP, logbookItems);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            logbookRepositoryService.saveBulk(LogbookCollections.OPERATION, logbookItems);
        }).doesNotThrowAnyException();

    }

}
