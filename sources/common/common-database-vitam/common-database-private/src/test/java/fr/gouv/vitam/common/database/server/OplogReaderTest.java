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

package fr.gouv.vitam.common.database.server;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

public class OplogReaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MongoClient mockClient;

    @Mock
    private MongoDatabase mockDB;

    @Mock
    private MongoCollection mockCollection;

    @Mock
    private FindIterable mockIterable;

    @Mock
    private MongoCursor mockCursor;

    @InjectMocks
    OplogReader oplogReader;

    final String docFromOplogSameId1 =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601472042, \"i\": 2}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"u\", \"ns\": \"metadata.Unit\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\"}, \"wall\": {\"$date\": 1601472042692}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 18\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    final String docFromOplogSameId2 =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601479799, \"i\": 1}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"u\", \"ns\": \"metadata.Unit\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\"}, \"wall\": {\"$date\": 1601479799256}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 19\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    final String docFromOplogSameId3 =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601480262, \"i\": 1}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"u\", \"ns\": \"metadata.Unit\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\"}, \"wall\": {\"$date\": 1601480262149}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 20\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    Document docWithSameId1 = Document.parse(docFromOplogSameId1);
    Document docWithSameId2 = Document.parse(docFromOplogSameId2);
    Document docWithSameId3 = Document.parse(docFromOplogSameId3);

    final String docFromOplogDifferentId =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601480262, \"i\": 1}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"u\", \"ns\": \"metadata.Unit\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i34jiaaaaq\"}, \"wall\": {\"$date\": 1601480262149}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 20\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    Document docWithDifferentId = Document.parse(docFromOplogDifferentId);

    final String docFromOplogDifferentCollection =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601480262, \"i\": 1}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"u\", \"ns\": \"metadata.test\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i34jiaaaaq\"}, \"wall\": {\"$date\": 1601480262149}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 20\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    Document docWithDifferentCollection = Document.parse(docFromOplogDifferentCollection);

    final String docFromOplogSelectOperation =
        "{\"ts\": {\"$timestamp\": {\"t\": 1601480262, \"i\": 1}}, \"t\": {\"$numberLong\": \"16\"}, \"h\": {\"$numberLong\": \"0\"}, \"v\": 2, \"op\": \"s\", \"ns\": \"metadata.Unit\", \"ui\": {\"$binary\": \"yURgEL/TmodlCIIXLe3dog==\", \"$type\": \"03\"}, \"o2\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i34jiaaaaq\"}, \"wall\": {\"$date\": 1601480262149}, \"o\": {\"_id\": \"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq\", \"DescriptionLevel\": \"Item\", \"Title\": \"Image de lac - 1\", \"Description\": \"L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 20\", \"TransactedDate\": \"2016-06-03T15:28:00\", \"_og\": \"aebaaaaaaah2skkzabq7waluf4i3z7aaaaaq\", \"_mgt\": {}, \"_sedaVersion\": \"2.1\", \"_unitType\": \"INGEST\", \"_opi\": \"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\", \"_ops\": [\"aeeaaaaaach2skkzabsacaluf4i2pmiaaaaq\"], \"_storage\": {\"strategyId\": \"default\"}, \"_sps\": [\"Identifier4\"], \"_sp\": \"Identifier4\", \"_up\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_us\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_graph\": [\"aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq/aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"], \"_uds\": {\"1\": [\"aeaqaaaaaah2skkzabq7waluf4i32fyaaaaq\"]}, \"_min\": 1, \"_max\": 2, \"_glpd\": \"2020-08-27T08:38:51.614\", \"_v\": 0, \"_av\": 0, \"_tenant\": 0}}";
    Document docWithSelectOperation = Document.parse(docFromOplogSelectOperation);

    final String description3 =
        "L'objet rattaché à cette unité archivistique a un usage de master (BinaryMaster) test 20";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(mockDB).when(mockClient).getDatabase(anyString());
        doReturn(mockCollection).when(mockDB).getCollection(anyString());
        oplogReader = new OplogReader(mockClient, 100);
    }

    @Test
    public void givenOplogThenGenerateData() {
        doReturn(mockIterable).when(mockCollection).find(any(Bson.class));
        doReturn(mockIterable).when(mockIterable).sort(any());
        doReturn(mockIterable).when(mockIterable).limit(100);
        doReturn(mockCursor).when(mockIterable).iterator();
        doReturn(true).doReturn(true).doReturn(false).when(mockCursor).hasNext();
        doReturn(docWithSameId1).doReturn(docWithDifferentId).doReturn(null).when(mockCursor).next();

        Map<String, Document> stringDocumentMap = oplogReader.readDocumentsFromOplogByShardAndCollections(
            Collections.emptyList(),
            new BsonTimestamp()
        );
        assertEquals(2, stringDocumentMap.size());
        assertTrue(stringDocumentMap.containsKey("aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq"));
        assertTrue(stringDocumentMap.containsKey("aeaqaaaaaah2skkzabq7waluf4i34jiaaaaq"));
    }

    @Test
    public void givenOplogThenGenerateRecentData() {
        doReturn(mockIterable).when(mockCollection).find(any(Bson.class));
        doReturn(mockIterable).when(mockIterable).sort(any());
        doReturn(mockIterable).when(mockIterable).limit(100);
        doReturn(mockCursor).when(mockIterable).iterator();
        doReturn(true).doReturn(true).doReturn(true).doReturn(false).when(mockCursor).hasNext();
        doReturn(docWithSameId1)
            .doReturn(docWithSameId2)
            .doReturn(docWithSameId3)
            .doReturn(null)
            .when(mockCursor)
            .next();

        Map<String, Document> stringDocumentMap = oplogReader.readDocumentsFromOplogByShardAndCollections(
            Collections.emptyList(),
            new BsonTimestamp()
        );
        assertEquals(1, stringDocumentMap.size());
        assertTrue(stringDocumentMap.containsKey("aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq"));
        // get recent value of description
        assertEquals(
            description3,
            ((Document) stringDocumentMap.get("aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq").get("o")).get("Description")
        );
    }

    @Test
    public void givenOplogThenGenerateOnlyFilteredData() {
        doReturn(mockIterable).when(mockCollection).find(any(Bson.class));
        doReturn(mockIterable).when(mockIterable).sort(any());
        doReturn(mockIterable).when(mockIterable).limit(100);
        doReturn(mockCursor).when(mockIterable).iterator();
        doReturn(true).doReturn(true).doReturn(true).doReturn(true).doReturn(false).when(mockCursor).hasNext();
        doReturn(docWithSameId1)
            .doReturn(docWithDifferentId)
            .doReturn(docWithSelectOperation)
            .doReturn(docWithDifferentCollection)
            .doReturn(null)
            .when(mockCursor)
            .next();

        Map<String, Document> stringDocumentMap = oplogReader.readDocumentsFromOplogByShardAndCollections(
            Collections.emptyList(),
            new BsonTimestamp()
        );
        assertEquals(2, stringDocumentMap.size());
        assertTrue(stringDocumentMap.containsKey("aeaqaaaaaah2skkzabq7waluf4i33eiaaaaq"));
        assertTrue(stringDocumentMap.containsKey("aeaqaaaaaah2skkzabq7waluf4i34jiaaaaq"));
    }
}
