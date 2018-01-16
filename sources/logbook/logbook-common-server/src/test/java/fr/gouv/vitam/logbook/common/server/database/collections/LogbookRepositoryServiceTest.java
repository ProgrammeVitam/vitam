package fr.gouv.vitam.logbook.common.server.database.collections;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;
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
        
        Mockito.when(vitamRepositoryProvider.getVitamMongoRepository(Matchers.any())).thenReturn(vitamMongoRepository);
        Mockito.doNothing().when(vitamMongoRepository).saveOrUpdate(Matchers.any());
        Mockito.when(vitamRepositoryProvider.getVitamESRepository(Matchers.any())).thenReturn(vitamElasticsearchRepository);
        Mockito.doNothing().when(vitamElasticsearchRepository).saveOrUpdate(Matchers.any());
        
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
