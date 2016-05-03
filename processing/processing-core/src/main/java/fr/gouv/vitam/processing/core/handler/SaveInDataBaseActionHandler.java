package fr.gouv.vitam.processing.core.handler;

import fr.gouv.vitam.api.MetaData;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.ProcessResponse;
import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.constants.StatusCode;

/**
 * Save/insert unit 's metaData
 * 
 *
 */
public class SaveInDataBaseActionHandler extends ActionHandler {

	public static final String HANDLER_ID = "saveInDataBaseAction";

	private MetaData metaData;

	@Override
	public boolean isExecuted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Response execute(fr.gouv.vitam.processing.beans.Process process, WorkParams params) {
		LOGGER.info("SaveInDataBaseActionHandler running ...");
		Response response = new ProcessResponse();
		try {

			if (params == null || params.getMetaDataRequest() == null) {
				LOGGER.info("parameters or metatDataRequest is null");
				return messageKo("parameters or metatDataRequest is null");
			}
			LOGGER.info("ProcessID :" + process.getId() + "| insertRequest:" + params.getMetaDataRequest() + "|");

			metaData.insertUnit(params.getMetaDataRequest());
			response.setStatus(StatusCode.OK);

		} catch (MetaDataNotFoundException e) {
			LOGGER.error("MetaData Not Found", e);
			return messageKo("MetaData Not Found");
		} catch (MetaDataAlreadyExistException e) {
			LOGGER.error("MetaData Already Exist", e);
			return messageKo("MetaData Already Exist");

		} catch (MetaDataExecutionException e) {
			LOGGER.error("a problem encountered when inserting(Metadata)", e);
			return messageKo("a problem encountered when inserting (Metadata)");

		} catch (InvalidParseOperationException e) {
			LOGGER.error("a problem encountered when parsing metaData", e);
			return messageKo("a problem encountered when parsing metaData");

		} catch (Exception e) {
			LOGGER.error("Exception thrown when excuting SaveInDataBaseAction ", e);
			return messageError("a problem encountered when parsing metaData");
		}

		return response;
	}

}
