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

import com.google.common.collect.Iterables;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessImpl.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;

public class LogbookMongoDbAccessImplTest {

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions());

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void beforeClass() {
        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), GUIDFactory.newGUID().getId(), null, 0);
    }

    @After
    public void after() {
        LogbookCollections.afterTest();
    }

    private LogbookMongoDbAccessImpl logbookMongoDbAccess =
        new LogbookMongoDbAccessImpl(mongoRule.getMongoClient(), "vitam-test", true, null, new ArrayList<>(),
            new LogbookTransformData(), Collections::emptyList);


    @Test
    @RunWithCustomExecutor
    public void should_update_logbook_in_bulk_mode() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        GUID guidLFC = GUIDFactory.newEventGUID(0);
        LogbookLifeCycleParameters masterLFC = getLogbookLifecyleParameters(GUIDFactory.newObjectGroupGUID(0), guidLFC);
        logbookMongoDbAccess.createLogbook(LogbookCollections.LIFECYCLE_UNIT, masterLFC);

        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk = new ArrayList<>();
        List<LogbookLifeCycleParameters> lifeCycleParameters = Lists.newArrayList();
        lifeCycleParameters
            .add(getLogbookLifecyleParameters(GUIDFactory.newEventGUID(0), GUIDFactory.newObjectGroupGUID(0)));
        lifeCycleParameters
            .add(getLogbookLifecyleParameters(GUIDFactory.newEventGUID(0), GUIDFactory.newObjectGroupGUID(0)));

        logbookLifeCycleParametersBulk.add(new LogbookLifeCycleParametersBulk(guidLFC.toString(), lifeCycleParameters));

        // When
        logbookMongoDbAccess.updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT, logbookLifeCycleParametersBulk);

        // Then
        FindIterable<LogbookLifeCycle> id =
            LogbookCollections.LIFECYCLE_UNIT.getCollection().find(eq("_id", guidLFC.toString()));
        LogbookLifeCycle lifeCycle = Iterables.getOnlyElement(id);

        assertThat((List) lifeCycle.get("events")).hasSize(2);
    }

    /**
     * @param eventIdentifierProcess
     * @param objectIdentifierLFC
     * @return parameters
     */
    private LogbookLifeCycleParameters getLogbookLifecyleParameters(GUID eventIdentifierProcess,
        GUID objectIdentifierLFC) {
        GUID eventIdentifierLFC = GUIDFactory.newEventGUID(0);
        String eventTypeLFC = "logbook lifecycle unit version test";
        LogbookTypeProcess eventTypeProcessLFC = LogbookTypeProcess.INGEST_TEST;
        StatusCode outcomeLFC = StatusCode.OK;
        String outcomeDetailLFC = StatusCode.OK.name();
        String outcomeDetailMessageLFC = StatusCode.OK.name();
        return LogbookParameterHelper.newLogbookLifeCycleUnitParameters
            (eventIdentifierLFC, eventTypeLFC, eventIdentifierProcess, eventTypeProcessLFC, outcomeLFC,
                outcomeDetailLFC, outcomeDetailMessageLFC, objectIdentifierLFC);

    }
}
