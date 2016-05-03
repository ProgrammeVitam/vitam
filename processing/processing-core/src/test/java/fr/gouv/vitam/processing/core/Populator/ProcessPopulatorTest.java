package fr.gouv.vitam.processing.core.Populator;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.processing.beans.Process;
import fr.gouv.vitam.processing.core.Populator.ProcessPopulator;

public class ProcessPopulatorTest {

	@Test
	public void test_shouldbePopulateProcess() {

		Process process = ProcessPopulator.populate();

		assertNotNull(process);

	}

}
