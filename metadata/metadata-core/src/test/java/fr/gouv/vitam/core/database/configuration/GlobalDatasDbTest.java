package fr.gouv.vitam.core.database.configuration;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import fr.gouv.vitam.common.UUIDMultiple;

public class GlobalDatasDbTest {
	@Test
	public void givenGlobalDatasDbWhenGetStaticValueThenReturnCorrectly() {
		assertEquals(UUIDMultiple.class, GlobalDatasDb.UUID_MULTIPLE.getClass());
		assertEquals(new HashSet<>(), GlobalDatasDb.ROOTS);
		assertEquals(false, GlobalDatasDb.useNewNode);
		assertEquals(true, GlobalDatasDb.useFilter);
		assertEquals(true, GlobalDatasDb.useFilteredRequest);
		assertEquals("vitamidx", GlobalDatasDb.INDEXNAME);
		assertEquals(null, GlobalDatasDb.localNetworkAddress);
		assertEquals(10001, GlobalDatasDb.limitES);
		assertEquals(10000, GlobalDatasDb.LIMIT_ES_NEW_INDEX);
		assertEquals(10000, GlobalDatasDb.LIMIT_MDB_NEW_INDEX);
		assertEquals(false, GlobalDatasDb.PRINT_REQUEST);
		assertEquals(false, GlobalDatasDb.BLOCKING);
		assertEquals(true, GlobalDatasDb.SAVERESULT);
		assertEquals(false, GlobalDatasDb.USELRUCACHE);
		assertEquals(3600000, GlobalDatasDb.TTLMS);
		assertEquals(3600, GlobalDatasDb.TTL);
		assertEquals(1000000, GlobalDatasDb.MAXLRU);
		assertEquals(false, GlobalDatasDb.USEREDIS);
	}
}
