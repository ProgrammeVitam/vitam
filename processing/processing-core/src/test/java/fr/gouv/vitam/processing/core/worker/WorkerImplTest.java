package fr.gouv.vitam.processing.core.worker;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.model.Step;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.api.worker.Worker;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment

public class WorkerImplTest {

	private Worker workerImpl;

	@Before
	public void setUp() throws Exception {
		workerImpl = new WorkerImpl();
	}

	@Test(expected = EngineIllegalArgumentException.class)
	public void should_throws_EngineIllegalArgumentException_workparams_null() {
		workerImpl.run(null, new Step());
	}

	@Test(expected = EngineIllegalArgumentException.class)
	public void should_throws_EngineIllegalArgumentException_step_null() {
		workerImpl.run(new WorkParams(), null);
	}

	@Test(expected = EngineIllegalArgumentException.class)
	public void should_throws_EngineIllegalArgumentException_empty_actions() {
		workerImpl.run(new WorkParams(), new Step());
	}
	// FIXME REVIEW and a test in success ?

}
