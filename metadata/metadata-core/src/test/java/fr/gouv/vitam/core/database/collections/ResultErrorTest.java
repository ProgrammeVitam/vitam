package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;

public class ResultErrorTest {
	@Test
	public void givenResultErrorConstructorWhenCreateWithoutCollectionThenAddNothing() {
		ResultError resultError = new ResultError(FILTERARGS.UNITS);
		assertEquals(0, resultError.getCurrentIds().size());
		assertEquals(true, resultError.isError());
	}
	
	@Test
	public void givenResultErrorConstructorWhenCreateWithCollectionThenAddAllCollections() {
		Set<String> errorIdsSet = new HashSet<>();
		errorIdsSet.add("id1");
		errorIdsSet.add("id2");
		ResultError resultError = new ResultError(FILTERARGS.UNITS, errorIdsSet);
		assertEquals(2, resultError.getCurrentIds().size());
	}
	
	@Test
	public void givenResultErrorWhenAddingErrorThenAddErrorIds() {
		ResultError resultError = new ResultError(FILTERARGS.UNITS);
		resultError.addError("errorId");
		assertEquals(1, resultError.getCurrentIds().size());
	}
	
	@Test
	public void givenResultErrorWhenPutFromThenAddAllComponents() {
		ResultError resultError1 = new ResultError(FILTERARGS.UNITS);
		ResultError resultError2 = new ResultError(FILTERARGS.UNITS);
		assertEquals(0, resultError1.getCurrentIds().size());
		resultError1.addError("errorId");
		assertEquals(1, resultError1.getCurrentIds().size());
		resultError2.putFrom(resultError1);
		assertEquals(1, resultError2.getCurrentIds().size());
	}
}
