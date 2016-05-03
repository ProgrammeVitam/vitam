package fr.gouv.vitam.api;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;

public interface MetaData {

	// Unit
	/**
	 * Insert Unit
	 * 
	 * @param Insert
	 *            { $roots: roots, $query : query, $filter : multi, $data : data
	 *            }
	 * 
	 * @throws InvalidParseOperationException
	 * @throws MetaDataNotFoundException
	 * @throws MetaDataAlreadyExistException
	 * @throws MetaDataExecutionException
	 */
	public void insertUnit(String insertRequest) throws InvalidParseOperationException, MetaDataNotFoundException,
			MetaDataAlreadyExistException, MetaDataExecutionException;
	// TODO Select, Update, Delete
}
