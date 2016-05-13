package fr.gouv.vitam.core;

import fr.gouv.vitam.core.database.collections.DbRequest;

@FunctionalInterface
public interface DbRequestFactory {

	public DbRequest create();
	
}
