/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.core.profile;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * This service manages the creation, update, find, ... profiles
 */
public interface ProfileService extends VitamAutoCloseable {
    /**
     * Create a collections of profile After passing the validation steps. If all the profiles are valid, they are
     * stored in the collection and indexed. </BR>
     * The profiles are not valid in the following situations : </BR>
     * <ul>
     * <li>The collection contains 2 ore many profile having the same identifier</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many profile already exist in the database</li>
     *
     * @param profileModelList the list of profiles to be created
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating contracts
     */
    RequestResponse<ProfileModel> createProfiles(List<ProfileModel> profileModelList) throws VitamException;

    /**
     * 1. Check that the document with given id exists else return VitamError
     * 2. Check that the document is (xsd, rng, ...) valid format else return VitamError
     * 3. If ok, save the file is the storage with the name (the given profile id)
     * TODO 4. In case of rng, check if RG exists ?!
     *
     * @param profileIdentifier the profile identifier
     * @param profileFile the profile file as an input stream
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException thrown if the profiles could not be imported
     */
    RequestResponse<ProfileModel> importProfileFile(String profileIdentifier, InputStream profileFile)
        throws VitamException;

    /**
     * download file corresponding to profileIdentifier
     *
     * @param profileIdentifier the profile identifier
     * @return Response
     * @throws ProfileNotFoundException thrown if the profile could not be found
     * @throws InvalidParseOperationException thrown if the query could not be executed
     * @throws ReferentialException thrown if the query could not be executed
     * @throws VitamException thrown if another error is encountered
     */
    Response downloadProfileFile(String profileIdentifier)
        throws ProfileNotFoundException, InvalidParseOperationException, ReferentialException;

    /**
     * Update profiles after passing validation steps : </BR>
     * Field modified :
     * <ul>
     * <li>- ActivationDate</li>
     * <li>- DesactivationDate</li>
     * <li>- LastUpdate</li>
     * <li>- Status</li>
     *
     * @param identifier identifier of the profile to update
     * @param jsonDsl the given profile dsl for update
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating contracts
     */
    RequestResponse<ProfileModel> updateProfile(String identifier, JsonNode jsonDsl) throws VitamException;

    /**
     * Update profile
     *
     * @param profileModel the updated ProfileModel
     * @param jsonDsl the query as a json
     * @return a response as a RequestResponse<ProfileModel> object
     * @throws VitamException thrown if the update could not be executed
     */
    RequestResponse<ProfileModel> updateProfile(ProfileModel profileModel, JsonNode jsonDsl) throws VitamException;

    /**
     * Find profile by identifier
     *
     * @param identifier the Profile identifier
     * @return ProfileModel
     * @throws ReferentialException thrown if the query could not be executed
     * @throws InvalidParseOperationException thrown if the query could not be executed
     */
    ProfileModel findByIdentifier(String identifier) throws ReferentialException, InvalidParseOperationException;

    /**
     * find Profile by QueryDsl
     *
     * @param queryDsl the query as a json to be executed
     * @return list of ProfileModel
     * @throws ReferentialException thrown if the query could not be executed
     * @throws InvalidParseOperationException thrown if the query could not be executed
     */
    RequestResponseOK<ProfileModel> findProfiles(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException;
}
