package fr.gouv.vitam.processing.core;

import java.util.Map;

import fr.gouv.vitam.processing.engine.ExecutionContextInterface;
import fr.gouv.vitam.processing.core.handler.ActionHandler;

/**
 * 
 * 
 *         ExecutionContext contains the different beans or parameters necessary
 *         for Process Engine
 * 
 *         //TODO this class will be managed by Spring context
 *
 */
public class ExecutionContext implements ExecutionContextInterface {

	private Map<String, ? extends ActionHandler> actions;

	public ExecutionContext() {
		// TODO Auto-generated constructor stub
	}

	public ExecutionContext(Map<String, ? extends ActionHandler> actions) {
		this.actions = actions;
	}

	@Override
	public Map<String, ? extends ActionHandler> getActions() {
		return actions;
	}

	public void setActions(Map<String, ? extends ActionHandler> actions) {
		this.actions = actions;
	}

}
