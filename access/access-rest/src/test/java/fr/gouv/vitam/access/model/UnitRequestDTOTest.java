package fr.gouv.vitam.access.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UnitRequestDTOTest {

	private UnitRequestDTO unitRequetsDTO;
	private static final String SAMPLE_QUERY_DSL = "{ \"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {}}";

	@Before
	public void intUnitDTO() {
		unitRequetsDTO = new UnitRequestDTO();
	}

	@Test
	public void testGetQueryDsl() {
		unitRequetsDTO = new UnitRequestDTO();
		Assert.assertNull(unitRequetsDTO.getQueryDsl());
	}

	@Test
	public void testSetQueryDsl() {
		unitRequetsDTO.setQueryDsl(SAMPLE_QUERY_DSL);
		Assert.assertEquals(SAMPLE_QUERY_DSL, unitRequetsDTO.getQueryDsl());
	}
}
