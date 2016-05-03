package fr.gouv.vitam.processing.engine;

import java.util.Map;

import fr.gouv.vitam.processing.handler.api.Action;


public interface ExecutionContextInterface {
	
	public Map<String,  ? extends Action> getActions();

}
