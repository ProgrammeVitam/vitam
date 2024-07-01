/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalNotFoundException;
import fr.gouv.vitam.access.external.common.exception.LogbookExternalClientException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.BasicClient;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSymbolicModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Admin External Client Interface
 */
public interface AdminExternalClient extends BasicClient, OperationStatusClient {
    /**
     * checkRules<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param rules the input stream to be checked
     * @return response including InputStream
     * @throws VitamClientException
     */
    Response checkRules(VitamContext vitamContext, InputStream rules) throws VitamClientException;

    /**
     * checkFormats<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param formats the input stream to be checked
     * @return response including InputStream
     * @throws VitamClientException
     */
    Response checkFormats(VitamContext vitamContext, InputStream formats) throws VitamClientException;

    /**
     * Find formats.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of formats
     * @throws VitamClientException
     */
    RequestResponse<FileFormatModel> findFormats(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find rules.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of rules
     * @throws VitamClientException
     */
    RequestResponse<FileRulesModel> findRules(VitamContext vitamContext, JsonNode select) throws VitamClientException;

    /**
     * Find entry contracts.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of ingest contrats
     * @throws VitamClientException
     */
    RequestResponse<IngestContractModel> findIngestContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find access contracts.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of access contrats
     * @throws VitamClientException
     */
    RequestResponse<AccessContractModel> findAccessContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find management contracts.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of management contrats
     * @throws VitamClientException
     */
    RequestResponse<ManagementContractModel> findManagementContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find contexts.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of contexts
     * @throws VitamClientException
     */
    RequestResponse<ContextModel> findContexts(VitamContext vitamContext, JsonNode select) throws VitamClientException;

    /**
     * Find profiles.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of profiles
     * @throws VitamClientException
     */
    RequestResponse<ProfileModel> findProfiles(VitamContext vitamContext, JsonNode select) throws VitamClientException;

    /**
     * Find accession registers.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of accession registers
     * @throws VitamClientException
     */
    RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find accession register details.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of accession register details
     * @throws VitamClientException
     */
    RequestResponse<AccessionRegisterDetailModel> findAccessionRegisterDetails(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException;

    /**
     * Find accession registers symbolic.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of accession registers
     * @throws VitamClientException
     */
    RequestResponse<AccessionRegisterSymbolicModel> findAccessionRegisterSymbolic(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException;

    /**
     * Find archive unit profiles
     *
     * @param vitamContext the vitam context
     * @param query select query
     * @return list of archive unit profiles
     * @throws VitamClientException
     */
    RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfiles(VitamContext vitamContext, JsonNode query)
        throws VitamClientException;

    /**
     * Get the accession register details matching the given query
     *
     * @param vitamContext the vitam context
     * @param id the id of accession register
     * @param query The DSL Query as a JSON Node
     * @return The AccessionregisterDetails list as a response jsonNode
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    RequestResponse getAccessionRegisterDetail(VitamContext vitamContext, String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException;

    /**
     * create a set of ingest contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 or many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts elready exist in the database</li>
     * </ul>
     *
     * @param vitamContext the vitam context
     * @param ingestContracts as InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createIngestContracts(VitamContext vitamContext, InputStream ingestContracts)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * create a set of access contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 or many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts already exist in the database</li>
     * </ul>
     *
     * @param vitamContext the vitam context
     * @param accessContracts as InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createAccessContracts(VitamContext vitamContext, InputStream accessContracts)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * create a set of access contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 or many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts already exist in the database</li>
     * <li>One or many strategies are invalid</li>
     * </ul>
     *
     * @param vitamContext the vitam context
     * @param accessContracts as InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createManagementContracts(VitamContext vitamContext, InputStream accessContracts)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given access contract by query dsl
     *
     * @param vitamContext the vitam context
     * @param accessContractId the given id of the access contract
     * @param queryDsl the given dsl query
     * @return Response status ok or vitam error
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateAccessContract(VitamContext vitamContext, String accessContractId, JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given ingest contract by query dsl
     *
     * @param vitamContext the vitam context
     * @param ingestContractId the given id of the ingest contract
     * @param queryDsl the given dsl query
     * @return Response status ok or vitam error
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateIngestContract(VitamContext vitamContext, String ingestContractId, JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given management contract by query dsl
     *
     * @param vitamContext the vitam context
     * @param managementContractId the given id of the management contract
     * @param queryDsl the given dsl query
     * @return Response status ok or vitam error
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateManagementContract(VitamContext vitamContext, String managementContractId, JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given archive unit profile by query DSL
     *
     * @param vitamContext the vitam context
     * @param archiveUnitprofileId the id of the archive unit profile update target
     * @param queryDSL the given DSL query
     * @return Response status OK or a VitamError
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateArchiveUnitProfile(VitamContext vitamContext, String archiveUnitprofileId, JsonNode queryDSL)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Create a profile metadata after passing the validation steps. If profile are json and valid, they are stored in
     * the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json of file is invalid</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>Profile already exist in the database</li>
     * </ul>
     *
     * @param vitamContext the vitam context
     * @param profiles as Json InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createProfiles(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Save profile file (xsd, rng, ...) corresponding to the profile metadata. As the id of profile metadata is
     * required, this method should be called after creation of profile metadata
     *
     * The profile file will be saved in storage with the name of id of profile metadata
     *
     * @param vitamContext the vitam context
     * @param profileMetadataId
     * @param profile as InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createProfileFile(VitamContext vitamContext, String profileMetadataId, InputStream profile)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Download the profile file according to profileMetadataId
     *
     * @param vitamContext the vitam context
     * @param profileMetadataId
     * @return Response
     */
    Response downloadProfileFile(VitamContext vitamContext, String profileMetadataId)
        throws AccessExternalClientException, AccessExternalNotFoundException;

    /**
     * create a set of contexts
     *
     * @param vitamContext the vitam context
     * @param contexts
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     */
    RequestResponse createContexts(VitamContext vitamContext, InputStream contexts)
        throws InvalidParseOperationException, AccessExternalClientServerException;

    /**
     * Update the context by query dsl
     *
     * @param vitamContext the vitam context
     * @param contextId the context id
     * @param queryDsl
     * @return Vitam response
     * @throws AccessExternalClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse updateContext(VitamContext vitamContext, String contextId, JsonNode queryDsl)
        throws AccessExternalClientException, InvalidParseOperationException;

    /**
     * @param vitamContext the vitam context
     * @param query
     * @throws AccessExternalClientServerException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     * @Deprecated use checkTraceabilityOperations instead.
     */
    @Deprecated
    RequestResponse checkTraceabilityOperation(VitamContext vitamContext, JsonNode query)
        throws AccessExternalClientServerException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * @param vitamContext the vitam context
     * @param query DSL query to select traceability operations to verify
     * @throws AccessExternalClientServerException
     * @throws InvalidParseOperationException
     * @Deprecated use checkTraceabilityOperations instead.
     */
    RequestResponse<JsonNode> checkTraceabilityOperations(VitamContext vitamContext, JsonNode query)
        throws AccessExternalClientServerException, InvalidParseOperationException;

    /**
     * Download the traceability operation file according to operationId
     *
     * @param vitamContext the vitam context
     * @param operationId
     * @throws AccessExternalClientServerException
     * @throws AccessUnauthorizedException
     */
    Response downloadTraceabilityOperationFile(VitamContext vitamContext, String operationId)
        throws AccessExternalClientServerException, AccessUnauthorizedException;

    /**
     * Check the existence of objects in the context of an audit
     *
     * @param vitamContext the vitam context
     * @param auditOption
     * @return Status
     * @throws AccessExternalClientServerException
     */
    RequestResponse launchAudit(VitamContext vitamContext, JsonNode auditOption)
        throws AccessExternalClientServerException;

    /**
     * Find a format by its id.
     *
     * @param vitamContext the vitam context
     * @param formatId the formatId
     * @return a format
     * @throws VitamClientException
     */
    RequestResponse<FileFormatModel> findFormatById(VitamContext vitamContext, String formatId)
        throws VitamClientException;

    /**
     * Find a rule by its id.
     *
     * @param vitamContext the vitam context
     * @param ruleId the rule id
     * @return a rule
     * @throws VitamClientException
     */
    RequestResponse<FileRulesModel> findRuleById(VitamContext vitamContext, String ruleId) throws VitamClientException;

    /**
     * Find an entry contract by its id.
     *
     * @param vitamContext the vitam context
     * @param contractId the contract id
     * @return an ingest contract
     * @throws VitamClientException
     */
    RequestResponse<IngestContractModel> findIngestContractById(VitamContext vitamContext, String contractId)
        throws VitamClientException;

    /**
     * Find an access contracts by its id.
     *
     * @param vitamContext the vitam context
     * @param contractId the contract id
     * @return an access contract
     * @throws VitamClientException
     */
    RequestResponse<AccessContractModel> findAccessContractById(VitamContext vitamContext, String contractId)
        throws VitamClientException;

    /**
     * Find an management contracts by its id.
     *
     * @param vitamContext the vitam context
     * @param contractId the contract id
     * @return an access contract
     * @throws VitamClientException
     */
    RequestResponse<ManagementContractModel> findManagementContractById(VitamContext vitamContext, String contractId)
        throws VitamClientException;

    /**
     * Find a context by its id
     *
     * @param vitamContext the vitam context
     * @param contextId the context id
     * @return a context
     * @throws VitamClientException
     */
    RequestResponse<ContextModel> findContextById(VitamContext vitamContext, String contextId)
        throws VitamClientException;

    /**
     * Find a profile by its id.
     *
     * @param vitamContext the vitam context
     * @param profileId the profile tId
     * @return a profile
     * @throws VitamClientException
     */
    RequestResponse<ProfileModel> findProfileById(VitamContext vitamContext, String profileId)
        throws VitamClientException;

    /**
     * Find an accession register by its id.
     *
     * @param vitamContext the vitam context
     * @param accessionRegisterId the accession register id
     * @return an accession register
     * @throws VitamClientException
     */
    RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(
        VitamContext vitamContext,
        String accessionRegisterId
    ) throws VitamClientException;

    /**
     * Find agencies
     *
     * @param vitamContext the vitam context
     * @param query select query
     * @return list of agencies
     * @throws VitamClientException
     */
    RequestResponse<AgenciesModel> findAgencies(VitamContext vitamContext, JsonNode query) throws VitamClientException;

    /**
     * Find an agency by its id.
     *
     * @param vitamContext the vitam context
     * @param agencyById the agency id
     * @return an agency
     * @throws VitamClientException
     */
    RequestResponse<AgenciesModel> findAgencyByID(VitamContext vitamContext, String agencyById)
        throws VitamClientException;

    /**
     * Find an archive unit profile by its id.
     *
     * @param vitamContext the vitam context
     * @param id the archive unit profile Id
     * @return an archive unit profile
     * @throws VitamClientException
     */
    RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfileById(VitamContext vitamContext, String id)
        throws VitamClientException;

    /**
     * Updates the given security profile by query dsl
     *
     * @param vitamContext the vitam context
     * @param securityProfileId the identifier of the security profile to update
     * @param queryDsl the given dsl query
     * @return Response status ok or vitam error
     * @throws VitamClientException
     */
    RequestResponse updateSecurityProfile(VitamContext vitamContext, String securityProfileId, JsonNode queryDsl)
        throws VitamClientException;

    /**
     * Find security profiles by query dsl.
     *
     * @param vitamContext the vitam context
     * @param select select query
     * @return list of security profiles
     * @throws VitamClientException
     */
    RequestResponse<SecurityProfileModel> findSecurityProfiles(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    /**
     * Find a security profile by its identifier.
     *
     * @param vitamContext the vitam context
     * @param securityProfileId the identifier of the security profile
     * @return a security profile
     * @throws VitamClientException
     */
    RequestResponse<SecurityProfileModel> findSecurityProfileById(VitamContext vitamContext, String securityProfileId)
        throws VitamClientException;

    /**
     * Update the detail of the profile
     *
     * @param vitamContext
     * @param profileMetadataId
     * @param queryDsl
     * @return a profile
     * @throws AccessExternalClientException
     */
    RequestResponse updateProfile(VitamContext vitamContext, String profileMetadataId, JsonNode queryDsl)
        throws AccessExternalClientException;

    /**
     * Get the list of operations details
     *
     * @param vitamContext the vitam context
     * @param query filter query
     * @return list of operations details
     * @throws VitamClientException
     */
    RequestResponse<ProcessDetail> listOperationsDetails(VitamContext vitamContext, ProcessQuery query)
        throws VitamClientException;

    /**
     * Update the operation according to the requested action
     *
     * @param vitamContext the vitam context
     * @param action an action as a string among "RESUME" (resume workflow till the end), "NEXT" (launch next step),
     * "REPLAY" (replay the step) and PAUSE" (pause the workflow)
     * @param operationId
     * @return the status
     * @throws VitamClientException
     */
    RequestResponse<ItemStatus> updateOperationActionProcess(
        VitamContext vitamContext,
        String action,
        String operationId
    ) throws VitamClientException;

    /**
     * @param vitamContext the vitam context
     * @param operationId
     * @return the details of the operation
     * @throws VitamClientException
     */
    RequestResponse<ItemStatus> getOperationProcessExecutionDetails(VitamContext vitamContext, String operationId)
        throws VitamClientException;

    /**
     * Cancel the operation
     *
     * @param vitamContext the vitam context
     * @param operationId
     * @return the status
     * @throws VitamClientException
     * @throws IllegalArgumentException
     */
    RequestResponse<ItemStatus> cancelOperationProcessExecution(VitamContext vitamContext, String operationId)
        throws VitamClientException, IllegalArgumentException;

    /**
     * @param vitamContext the vitam context@return the Workflow definitions
     * @return the pool of workflows
     * @throws VitamClientException
     */
    RequestResponse<WorkFlow> getWorkflowDefinitions(VitamContext vitamContext) throws VitamClientException;

    /**
     * create a set of agencies
     *
     * @param vitamContext the vitam context
     * @param agencies agencies to be created
     * @param filename
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createAgencies(VitamContext vitamContext, InputStream agencies, String filename)
        throws AccessExternalClientException, InvalidParseOperationException;

    /**
     * create a set of formats
     *
     * @param vitamContext the vitam context
     * @param formats formats to be created
     * @param filename
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createFormats(VitamContext vitamContext, InputStream formats, String filename)
        throws AccessExternalClientException, InvalidParseOperationException;

    /**
     * create a set of rules
     *
     * @param vitamContext the vitam context
     * @param rules rules to be created
     * @param filename
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createRules(VitamContext vitamContext, InputStream rules, String filename)
        throws AccessExternalClientException, InvalidParseOperationException;

    /**
     * create a set of security profiles
     *
     * @param vitamContext the vitam context
     * @param securityProfiles security profiles to be created
     * @param filename
     * @return Vitam response
     * @throws AccessExternalClientException
     * @throws InvalidParseOperationException
     * @throws VitamClientException
     */
    RequestResponse createSecurityProfiles(VitamContext vitamContext, InputStream securityProfiles, String filename)
        throws AccessExternalClientException, InvalidParseOperationException, VitamClientException;

    /**
     * Download distribution reports
     *
     * @param vitamContext the vitam context
     * @param opId the op (logbook) ID
     * @return the rulesReport
     * @throws VitamClientException
     */
    Response downloadDistributionReport(VitamContext vitamContext, String opId) throws VitamClientException;

    /**
     * Download batch reports
     *
     * @param vitamContext the vitam context
     * @param opId the op (logbook) ID
     * @return batch report file
     * @throws VitamClientException
     */
    Response downloadBatchReport(VitamContext vitamContext, String opId) throws VitamClientException;

    /**
     * Download rules report
     *
     * @param vitamContext the vitam context
     * @param opId the op (logbook) ID
     * @return the rulesReport
     * @throws VitamClientException
     */
    Response downloadRulesReport(VitamContext vitamContext, String opId) throws VitamClientException;

    /**
     * Download Csv referential Agencies
     *
     * @param vitamContext the vitam context
     * @param opId the op (logbook) ID
     * @return Agecsv referential
     * @throws VitamClientException vitamClientException
     */
    Response downloadAgenciesCsvAsStream(VitamContext vitamContext, String opId) throws VitamClientException;

    /**
     * Download Csv referential Rules
     *
     * @param vitamContext the vitam context
     * @param opId the op (logbook) ID
     * @return Rules csv referential
     * @throws VitamClientException vitamClientException
     */
    Response downloadRulesCsvAsStream(VitamContext vitamContext, String opId) throws VitamClientException;

    /**
     * checkAgencies<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param agencies the input stream to be checked
     * @return response including InputStream
     * @throws VitamClientException
     */
    Response checkAgencies(VitamContext vitamContext, InputStream agencies) throws VitamClientException;

    /**
     * launch a traceability audit for the request
     *
     * @param vitamContext the vitamContext
     * @param queryDsl the queryDsl
     * @return RequestResponse
     * @throws VitamClientException The Exception
     */
    RequestResponse<JsonNode> evidenceAudit(VitamContext vitamContext, JsonNode queryDsl) throws VitamClientException;

    /**
     * launch a rectification audit for the operation id
     *
     * @param vitamContext the operation Id
     * @param operationId the operation Id
     * @return RequestResponse
     * @throws VitamClientException The Exception
     */
    RequestResponse rectificationAudit(VitamContext vitamContext, String operationId) throws VitamClientException;

    /**
     * launch probative value process
     *
     * @param vitamContext the vitam context
     * @param probativeValueRequest the request
     * @return RequestResponse
     * @throws VitamClientException {@link VitamClientException}
     */
    RequestResponse exportProbativeValue(VitamContext vitamContext, ProbativeValueRequest probativeValueRequest)
        throws VitamClientException;

    /**
     * Create a ArchiveUnitProfile after passing the validation steps. If profiles are json and valid, they are stored
     * in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json of file is unparsable or invalid</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>profiles already exist in the database</li>
     * </ul>
     *
     * @param vitamContext the vitam context
     * @param profiles archive unit profiles as Json InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse createArchiveUnitProfile(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Find ontologies
     *
     * @param vitamContext the vitam context
     * @param query select query
     * @return list of ontologies
     * @throws VitamClientException
     */
    RequestResponse<OntologyModel> findOntologies(VitamContext vitamContext, JsonNode query)
        throws VitamClientException;

    /**
     * Find an ontology by its id.
     *
     * @param vitamContext the vitam context
     * @param id the ontology Id
     * @return an ontology
     * @throws VitamClientException
     */
    RequestResponse<OntologyModel> findOntologyById(VitamContext vitamContext, String id) throws VitamClientException;

    /**
     * Import a set of ontologies metadata. </BR>
     * If all the ontologies are valid, they will be stored in the ontology collection and indexed. The input is invalid
     * in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains an already used identifier</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * </ul>
     *
     * @param forceUpdate
     * @param vitamContext the vitam context
     * @param ontologies as Json InputStream
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse importOntologies(boolean forceUpdate, VitamContext vitamContext, InputStream ontologies)
        throws InvalidParseOperationException, AccessExternalClientException;

    RequestResponse importGriffin(VitamContext vitamContext, InputStream griffinStream, String filName)
        throws VitamClientException, AccessExternalClientException;

    RequestResponse importPreservationScenario(VitamContext vitamContext, InputStream scenarioStream, String filName)
        throws VitamClientException, AccessExternalClientException;

    RequestResponse<GriffinModel> findGriffinById(VitamContext vitamContext, String id) throws VitamClientException;

    RequestResponse<PreservationScenarioModel> findPreservationScenarioById(VitamContext vitamContext, String id)
        throws VitamClientException;

    RequestResponse<PreservationScenarioModel> findPreservationScenario(VitamContext vitamContext, JsonNode select)
        throws VitamClientException;

    RequestResponse<GriffinModel> findGriffin(VitamContext vitamContext, JsonNode select) throws VitamClientException;

    /**
     * Create external logbook operation entry <br>
     * <br>
     *
     * @param logbookOperationparams the logbook parameters to be created
     * @param vitamContext the vitam context
     * @return RequestResponse status of the insertion
     * @throws LogbookExternalClientException
     */
    RequestResponse createExternalOperation(
        VitamContext vitamContext,
        LogbookOperationParameters logbookOperationparams
    ) throws LogbookExternalClientException;
}
