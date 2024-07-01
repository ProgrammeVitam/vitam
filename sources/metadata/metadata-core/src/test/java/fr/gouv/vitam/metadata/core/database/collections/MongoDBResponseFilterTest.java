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
package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.fail;

public class MongoDBResponseFilterTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    public void givenObjectGroupFilterIsDone() throws InvalidParseOperationException {
        ObjectGroup group = new ObjectGroup(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp1_1.json"))
        );
        MongoDbMetadataResponseFilter.filterFinalResponse(group);
        checkAllDocument(group);

        group = new ObjectGroup(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp1_sp2_2.json"))
        );
        MongoDbMetadataResponseFilter.filterFinalResponse(group);
        checkAllDocument(group);

        group = new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp2_4.json")));
        MongoDbMetadataResponseFilter.filterFinalResponse(group);
        checkAllDocument(group);

        group = new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp2.json")));
        MongoDbMetadataResponseFilter.filterFinalResponse(group);
        checkAllDocument(group);

        group = new ObjectGroup(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_group_full.json"))
        );
        MongoDbMetadataResponseFilter.filterFinalResponse(group);
        checkAllDocument(group);
    }

    @Test
    public void givenUnitFilterIsDone() throws InvalidParseOperationException {
        Unit unit = new Unit(JsonHandler.getFromInputStream(getClass().getResourceAsStream("/unit_full.json")));
        MongoDbMetadataResponseFilter.filterFinalResponse(unit);
        checkAllDocument(unit);
    }

    private void checkAllDocument(Document document) {
        ArrayList<String> report = new ArrayList<String>();
        checkDocument(document, report);
        if (report.size() > 0) {
            fail("report should be empty " + report.toString());
        }
    }

    private void checkDocument(Document document, ArrayList<String> report) {
        final Set<String> keys = document.keySet();
        for (String key : keys) {
            if (key.startsWith("_")) {
                report.add(key);
            }
            Object obj = document.get(key);
            if (obj instanceof ArrayList) {
                for (Object item : (ArrayList) obj) {
                    if (item instanceof Document) {
                        checkAllDocument((Document) item);
                    }
                }
            } else if (obj instanceof Document) {
                checkAllDocument((Document) obj);
            }
        }
    }
}
