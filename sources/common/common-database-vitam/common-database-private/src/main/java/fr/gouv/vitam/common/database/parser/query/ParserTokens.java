/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.parser.query;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.ontologies.client.AdminManagementOntologiesClient;
import fr.gouv.vitam.functional.administration.ontologies.client.AdminManagementOntologiesClientFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main language definition
 */
public class ParserTokens extends BuilderToken {

    /**
     * Default prefix for internal variable
     */
    public static final String DEFAULT_HASH_PREFIX = "#";
    /**
     * Default prefix for internal variable
     */
    public static final char DEFAULT_HASH_PREFIX_CHAR = '#';
    /**
     * Default underscore prefix for command
     */
    public static final char DEFAULT_UNDERSCORE_PREFIX_CHAR = '_';
    /**
     * Given field that are not array in mongo
     */
    private static final List<String> SUB_FIELDS_NOT_ARRAY = Arrays.asList("evDetData",
        "FirstName", "Corpname", "Gender", "BirthPlace", "GivenName", "DeathPlace", "BirthDate", "Identifier",
        "BirthName", "DeathDate", "Nationality", "UpdateDate",
        "Function", "Activity", "Position", "Role",
        "Geogname", "Address", "PostalCode", "City", "Region", "Country",
        "PhysicalId", "Filename", "MessageDigest", "Size", "FormatIdentification", "FileInfo", "Metadata",
        "OtherMetadata", "_nbc", "_opi",
        "Algorithm", "DataObjectGroupId", "DataObjectVersion", "strategyId",
        "EndDate", "Rule", "PreventInheritance", "StartDate", "FinalAction",
        "ClassificationLevel", "ClassificationOwner", "ClassificationAudience", "ClassificationReassessingDate",
        "NeedReassessingAuthorization", "_validComputedInheritedRules"
    );

    /**
     * @deprecated Quick & dirty fix of non analyzed fields.
     */
    private static final Set<String> NON_ANALYZED_FIELDS = new HashSet<>(Arrays.asList(
        /* Units */
        "AcquiredDate",
        "Addressee.BirthDate",
        "Addressee.BirthPlace.PostalCode",
        "Addressee.DeathDate",
        "Addressee.DeathPlace.PostalCode",
        "Addressee.Identifier",
        "ArchivalAgencyArchiveUnitIdentifier",
        "ArchiveUnitProfile",
        "AuthorizedAgent.BirthDate",
        "AuthorizedAgent.BirthPlace.PostalCode",
        "AuthorizedAgent.DeathDate",
        "AuthorizedAgent.DeathPlace.PostalCode",
        "AuthorizedAgent.Identifier",
        "CreatedDate",
        "CustodialHistory.CustodialHistoryFile.DataObjectGroupReferenceId",
        "DescriptionLevel",
        "Descriptions",
        "DescriptionLanguage",
        "EndDate",
        "Event.EventDateTime",
        "Event.EventIdentifier",
        "FilePlanPosition",
        "Gps.GpsAltitude",
        "Gps.GpsAltitudeRef",
        "Gps.GpsDateStamp",
        "Gps.GpsLatitude",
        "Gps.GpsLatitudeRef",
        "Gps.GpsLongitude",
        "Gps.GpsLongitudeRef",
        "Gps.GpsVersionID",
        "Keyword.KeywordContent",
        "Keyword.KeywordReference",
        "Keyword.KeywordType",
        "Language",
        "OriginatingAgency.Identifier",
        "OriginatingAgencyArchiveUnitIdentifier",
        "OriginatingSystemId",
        "ReceivedDate",
        "Recipient.BirthDate",
        "Recipient.BirthPlace.PostalCode",
        "Recipient.DeathDate",
        "Recipient.DeathPlace.PostalCode",
        "Recipient.Identifier",
        "RegisteredDate",
        "RelatedObjectReference.IsPartOf.ArchiveUnitRefId",
        "RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectGroupReferenceId",
        "RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectReferenceId",
        "RelatedObjectReference.IsPartOf.RepositoryArchiveUnitPID",
        "RelatedObjectReference.IsPartOf.RepositoryObjectPID",
        "RelatedObjectReference.IsVersionOf.ArchiveUnitRefId",
        "RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectGroupReferenceId",
        "RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectReferenceId",
        "RelatedObjectReference.IsVersionOf.RepositoryArchiveUnitPID",
        "RelatedObjectReference.IsVersionOf.RepositoryObjectPID",
        "RelatedObjectReference.References.ArchiveUnitRefId",
        "RelatedObjectReference.References.DataObjectReference.DataObjectGroupReferenceId",
        "RelatedObjectReference.References.DataObjectReference.DataObjectReferenceId",
        "RelatedObjectReference.References.RepositoryArchiveUnitPID",
        "RelatedObjectReference.References.RepositoryObjectPID",
        "RelatedObjectReference.Replaces.ArchiveUnitRefId",
        "RelatedObjectReference.Replaces.DataObjectReference.DataObjectGroupReferenceId",
        "RelatedObjectReference.Replaces.DataObjectReference.DataObjectReferenceId",
        "RelatedObjectReference.Replaces.RepositoryArchiveUnitPID",
        "RelatedObjectReference.Replaces.RepositoryObjectPID",
        "RelatedObjectReference.Requires.ArchiveUnitRefId",
        "RelatedObjectReference.Requires.DataObjectReference.DataObjectGroupReferenceId",
        "RelatedObjectReference.Requires.DataObjectReference.DataObjectReferenceId",
        "RelatedObjectReference.Requires.RepositoryArchiveUnitPID",
        "RelatedObjectReference.Requires.RepositoryObjectPID",
        "SentDate",
        "Signature.DateSignature",
        "Signature.Masterdata.Value",
        "Signature.ReferencedObject.SignedObjectDigest.Algorithm",
        "Signature.ReferencedObject.SignedObjectDigest.Value",
        "Signature.ReferencedObject.SignedObjectId",
        "Signature.Signer.Activity",
        "Signature.Signer.BirthDate",
        "Signature.Signer.BirthPlace.PostalCode",
        "Signature.Signer.DeathDate",
        "Signature.Signer.DeathPlace.PostalCode",
        "Signature.Signer.Function",
        "Signature.Signer.Identifier",
        "Signature.Signer.SigningTime",
        "Signature.Validator.Activity",
        "Signature.Validator.BirthDate",
        "Signature.Validator.BirthPlace.PostalCode",
        "Signature.Validator.DeathDate",
        "Signature.Validator.DeathPlace.PostalCode",
        "Signature.Validator.Function",
        "Signature.Validator.Identifier",
        "Signature.Validator.ValidationTime",
        "StartDate",
        "Status",
        "SubmissionAgency.Identifier",
        "SystemId",
        "Tag",
        "Titles",
        "TransactedDate",
        "TransferringAgencyArchiveUnitIdentifier",
        "Type",
        "Version",
        "Sender.Activity",
        "Sender.BirthDate",
        "Sender.BirthPlace.PostalCode",
        "Sender.DeathDate",
        "Sender.DeathPlace.PostalCode",
        "Sender.Function",
        "Sender.Identifier",
        "Transmitter.Activity",
        "Transmitter.BirthDate",
        "Transmitter.BirthPlace.PostalCode",
        "Transmitter.DeathDate",
        "Transmitter.DeathPlace.PostalCode",
        "Transmitter.Function",
        "Transmitter.Identifier",
        "Writer.Activity",
        "Writer.BirthDate",
        "Writer.BirthPlace.PostalCode",
        "Writer.DeathDate",
        "Writer.DeathPlace.PostalCode",
        "Writer.Function",
        "Writer.Identifier",
        "_elimination.OperationId",
        "_elimination.GlobalStatus",
        "_elimination.DestroyableOriginatingAgencies",
        "_elimination.NonDestroyableOriginatingAgencies",
        "_elimination.ExtendedInfo",
        "_elimination.ExtendedInfo.ExtendedInfoType",
        "_elimination.ExtendedInfo.ExtendedInfoDetails",
        "_elimination.ExtendedInfo.ExtendedInfoDetails.ParentUnitId",
        "_elimination.ExtendedInfo.ExtendedInfoDetails.DestroyableOriginatingAgencies",
        "_elimination.ExtendedInfo.ExtendedInfoDetails.NonDestroyableOriginatingAgencies",
        "_computedInheritedRules.StorageRule",
        "_computedInheritedRules.StorageRule.MaxEndDate",
        "_computedInheritedRules.AppraisalRule",
        "_computedInheritedRules.AppraisalRule.MaxEndDate",
        "_computedInheritedRules.DisseminationRule",
        "_computedInheritedRules.DisseminationRule.MaxEndDate",
        "_computedInheritedRules.AccessRule",
        "_computedInheritedRules.AccessRule.MaxEndDate",
        "_computedInheritedRules.ReuseRule",
        "_computedInheritedRules.ReuseRule.MaxEndDate",
        "_computedInheritedRules.ClassificationRule",
        "_computedInheritedRules.ClassificationRule.MaxEndDate",
        "_computedInheritedRules.indexationDate",
        "_computedInheritedRules.NeedAuthorization",
        "_validComputedInheritedRules",
        "_glpd",
        "_graph",
        "_max",
        "_mgt.AccessRule.Inheritance.PreventInheritance",
        "_mgt.AccessRule.Inheritance.PreventRulesId",
        "_mgt.AccessRule.Rules.EndDate",
        "_mgt.AccessRule.Rules.Rule",
        "_mgt.AccessRule.Rules.StartDate",
        "_mgt.AppraisalRule.FinalAction",
        "_mgt.AppraisalRule.Inheritance.PreventInheritance",
        "_mgt.AppraisalRule.Inheritance.PreventRulesId",
        "_mgt.AppraisalRule.Rules.EndDate",
        "_mgt.AppraisalRule.Rules.Rule",
        "_mgt.AppraisalRule.Rules.StartDate",
        "_mgt.ClassificationRule.Inheritance.PreventInheritance",
        "_mgt.ClassificationRule.Inheritance.PreventRulesId",
        "_mgt.ClassificationRule.Rules.EndDate",
        "_mgt.ClassificationRule.Rules.Rule",
        "_mgt.ClassificationRule.Rules.StartDate",
        "_mgt.DisseminationRule.Inheritance.PreventInheritance",
        "_mgt.DisseminationRule.Inheritance.PreventRulesId",
        "_mgt.ClassificationRule.ClassificationLevel",
        "_mgt.ClassificationRule.ClassificationAudience",
        "_mgt.ClassificationRule.ClassificationReassessingDate",
        "_mgt.ClassificationRule.NeedReassessingAuthorization",
        "_mgt.DisseminationRule.Rules.EndDate",
        "_mgt.DisseminationRule.Rules.Rule",
        "_mgt.DisseminationRule.Rules.StartDate",
        "_mgt.ReuseRule.Inheritance.PreventInheritance",
        "_mgt.ReuseRule.Inheritance.PreventRulesId",
        "_mgt.ReuseRule.Rules.EndDate",
        "_mgt.ReuseRule.Rules.Rule",
        "_mgt.ReuseRule.Rules.StartDate",
        "_mgt.StorageRule.FinalAction",
        "_mgt.StorageRule.Inheritance.PreventInheritance",
        "_mgt.StorageRule.Inheritance.PreventRulesId",
        "_mgt.StorageRule.Rules.EndDate",
        "_mgt.StorageRule.Rules.Rule",
        "_mgt.StorageRule.Rules.StartDate",
        "_mgt.NeedAuthorization",
        "_min",
        "_nbc",
        "_og",
        "_ops",
        "_opi",
        "_score",
        "_sp",
        "_sps",
        "_storage._nbc",
        "_storage.offerIds",
        "_storage.strategyId",
        "_tenant",
        "_uds",
        "_unitType",
        "_unused",
        "_up",
        "_us",
        "_us_sp",
        "_v",
        "_id",
        /* Object Groups */
        "FileInfo.DateCreatedByApplication",
        "FileInfo.LastModified",
        "Metadata",
        "OtherMetadata",
        "_nbc",
        "_ops",
        "_opi",
        "_profil",
        "_qualifiers._nbc",
        "_qualifiers.qualifier",
        "_qualifiers.versions.Algorithm",
        "_qualifiers.versions.DataObjectGroupId",
        "_qualifiers.versions.DataObjectVersion",
        "_qualifiers.versions._opi",
        "_qualifiers.versions.FileInfo.DateCreatedByApplication",
        "_qualifiers.versions.FileInfo.LastModified",
        "_qualifiers.versions.FormatIdentification.FormatId",
        "_qualifiers.versions.FormatIdentification.Encoding",
        "_qualifiers.versions.FormatIdentification.MimeType",
        "_qualifiers.versions.FormatIdentification.FormatLitteral",
        "_qualifiers.versions.MessageDigest",
        "_qualifiers.versions.PhysicalDimensions.Depth.unit",
        "_qualifiers.versions.PhysicalDimensions.Depth.dValue",
        "_qualifiers.versions.PhysicalDimensions.Diameter.unit",
        "_qualifiers.versions.PhysicalDimensions.Diameter.dValue",
        "_qualifiers.versions.PhysicalDimensions.Height.unit",
        "_qualifiers.versions.PhysicalDimensions.Height.dValue",
        "_qualifiers.versions.PhysicalDimensions.Length.unit",
        "_qualifiers.versions.PhysicalDimensions.Length.dValue",
        "_qualifiers.versions.PhysicalDimensions.NumberOfPage",
        "_qualifiers.versions.PhysicalDimensions.Shape",
        "_qualifiers.versions.PhysicalDimensions.Thickness.unit",
        "_qualifiers.versions.PhysicalDimensions.Thickness.dValue",
        "_qualifiers.versions.PhysicalDimensions.Weight.unit",
        "_qualifiers.versions.PhysicalDimensions.Weight.dValue",
        "_qualifiers.versions.PhysicalDimensions.Width.unit",
        "_qualifiers.versions.PhysicalDimensions.Width.dValue",
        "_qualifiers.versions.PhysicalId",
        "_qualifiers.versions.Size",
        "_qualifiers.versions.Uri",
        "_qualifiers.versions._id",
        "_qualifiers.versions._storage._nbc",
        "_qualifiers.versions._storage.offerIds",
        "_qualifiers.versions._storage.strategyId",
        "_score",
        "_sp",
        "_sps",
        "_storage._nbc",
        "_storage.offerIds",
        "_storage.strategyId",
        "_tenant",
        "_uds",
        "_unused",
        "_up",
        "_us",
        "_v",
        "_id",
        /* Access Contracts */
        "Identifier",
        "Status",
        "OriginatingAgencies",
        "CreationDate",
        "LastUpdate",
        "ActivationDate",
        "DeactivationDate",
        "DataObjectVersion",
        "WritingPermission",
        "WritingRestrictedDesc",
        "EveryOriginatingAgency",
        "EveryDataObjectVersion",
        "RootUnits",
        "ExcludedRootUnits",
        "AccessLog",
        "_tenant",
        "_v",
        "_score",
        "_id",
        /* Agencies */
        "Identifier",
        "_tenant",
        "_v",
        "_score",
        "_id",
        /* Context */
        "Identifier",
        "Status",
        "LastUpdate",
        "CreationDate",
        "ActivationDate",
        "DeactivationDate",
        "Permissions.AccessContracts",
        "Permissions.IngestContracts",
        "Permissions.tenant",
        "EnableControl",
        "SecurityProfile",
        "_v",
        "_score",
        "_id",
        /* Format */
        "PUID",
        "Extension",
        "VersionPronom",
        "CreatedDate",
        "Version",
        "Alert",
        "HasPriorityOverFileFormatID",
        "_v",
        "_score",
        "_id",
        /* Ingest Contract */
        "Identifier",
        "Status",
        "ArchiveProfiles",
        "CreationDate",
        "LastUpdate",
        "ActivationDate",
        "DeactivationDate",
        "LinkParentId",
        "CheckParentLink",
        "CheckParentId",
        "MasterMandatory",
        "FormatUnidentifiedAuthorized",
        "EveryFormatType",
        "FormatType",
        "_tenant",
        "_v",
        "_score",
        "_id",
        /* Profiles */
        "Identifier",
        "Status",
        "Format",
        "Path",
        "CreationDate",
        "LastUpdate",
        "ActivationDate",
        "DeactivationDate",
        "_tenant",
        "_v",
        "_score",
        "_id",
        /* ArchiveUnitProfile */
        "Identifier",
        "Status",
        "ControlSchema",
        "CreationDate",
        "LastUpdate",
        "ActivationDate",
        "DeactivationDate",
        "_tenant",
        "_v",
        "_score",
        "_id",
        /* Rule */
        "RuleType",
        "RuleDuration",
        "_tenant",
        "RuleId",
        "RuleMeasurement",
        "CreationDate",
        "UpdateDate",
        "_v",
        "_score",
        "_id",
        /* Security profiles */
        "Identifier",
        "FullAccess",
        "Permissions",
        "_v",
        "_score",
        "_id",
        /* Accession register summary */
        "OriginatingAgency",
        "creationDate",
        "TotalObjects.ingested",
        "TotalObjects.deleted",
        "TotalObjects.remained",
        "TotalObjectGroups.ingested",
        "TotalObjectGroups.deleted",
        "TotalObjectGroups.remained",
        "TotalUnits.ingested",
        "TotalUnits.deleted",
        "TotalUnits.remained",
        "ObjectSize.ingested",
        "ObjectSize.deleted",
        "ObjectSize.remained",
        "_id",
        "_tenant",
        "_v",
        "_score",
        /* Accession register detail */
        "OriginatingAgency",
        "SubmissionAgency",
        "ArchivalAgreement",
        "AcquisitionInformation",
        "Opc",
        "Opi",
        "Events.Opc",
        "Events.OpType",
        "Events.Gots",
        "Events.Units",
        "Events.Objects",
        "Events.ObjSize",
        "Events.CreationDate",
        "EndDate",
        "StartDate",
        "LastUpdate",
        "Status",
        "TotalObjects.ingested",
        "TotalObjects.deleted",
        "TotalObjects.remained",
        "TotalObjectGroups.ingested",
        "TotalObjectGroups.deleted",
        "TotalObjectGroups.remained",
        "TotalUnits.ingested",
        "TotalUnits.deleted",
        "TotalUnits.remained",
        "ObjectSize.ingested",
        "ObjectSize.deleted",
        "ObjectSize.remained",
        "OperationIds",
        "LegalStatus",
        "OpType",
        "_id",
        "_tenant",
        "_v",
        "_score",
        "_history.ud",
        "_history.data._mgt.ClassificationRule.Inheritance.PreventInheritance",
        "_history.data._mgt.ClassificationRule.Inheritance.PreventRulesId",
        "_history.data._mgt.ClassificationRule.Rules.EndDate",
        "_history.data._mgt.ClassificationRule.Rules.Rule",
        "_history.data._mgt.ClassificationRule.Rules.StartDate",
        "_history.data._mgt.DisseminationRule.Inheritance.PreventInheritance",
        "_history.data._mgt.DisseminationRule.Inheritance.PreventRulesId",
        "_history.data._mgt.ClassificationRule.ClassificationLevel",
        "_history.data._mgt.ClassificationRule.ClassificationAudience",
        "_history.data._mgt.ClassificationRule.ClassificationReassessingDate",
        "_history.data._mgt.ClassificationRule.NeedReassessingAuthorization",
        "_history.data._v",
        "_implementationVersion",
        "_sedaVersion"));

    private static AdminManagementOntologiesClientFactory ONTOLOGY_MGT_FACTORY = AdminManagementOntologiesClientFactory.getInstance();
    private static ConcurrentMap<String, Boolean> analyzedOntologyCache = new ConcurrentHashMap<>();

    static {
        new OntologiesLoader();
    }

    private ParserTokens() {
        // Empty
    }

    /**
     * reload cached ontologies
     */
    private static void loadOntologies() {
        Map<String, Boolean> reloadedOntologies = new HashMap<>();

        try (AdminManagementOntologiesClient client = ONTOLOGY_MGT_FACTORY.getClient()) {
            RequestResponse<OntologyModel> ontologyResponse =
                client.findOntologiesForCache(new Select().getFinalSelect());
            if (ontologyResponse.isOk()) {
                ((RequestResponseOK<OntologyModel>) ontologyResponse).getResults().stream().forEach(ontology -> {
                    reloadedOntologies.put(ontology.getIdentifier(), ontology.getType().isAnalyzed());
                });
            }
        } catch (final Exception e) {
            // unable to load ontologies
            // TODO : handle exception properly
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);

        }

        // refresh cache
        analyzedOntologyCache.clear();
        analyzedOntologyCache.putAll(reloadedOntologies);
    }

    /**
     * get isAnalyzed flag from cached ontology (reload cache if necessary)
     *
     * @param key
     * @return
     */
    private static Boolean getAnalyzedFlagFromCachedOntologies(String key) {
        if (analyzedOntologyCache.isEmpty()) {
            // force cache reload
            loadOntologies();
        }

        return analyzedOntologyCache.get(key);
    }

    /**
     * extract key from a path name
     *
     * @param name the path
     * @return the key
     */
    private static String getKeyFromPathName(String name) {
        int dotPos = name.lastIndexOf(".");
        if (dotPos > 0) {
            return name.substring(dotPos + 1);
        }

        return name;
    }


    /**
     * specific fields: nbunits, dua, ... <br>
     * $fields : [ #nbunits:1, #dua:1, #all:1... ]
     * <p>
     * #all:1 means all, while #all:0 means none
     */
    public static enum PROJECTIONARGS {
        /**
         * Id of the item
         */
        ID("id"),
        /**
         * Number of units immediate children from this Unit
         */
        NBUNITS("nbunits"),
        /**
         * Number of objects within ObjectGroup
         */
        NBOBJECTS("nbobjects"),
        /**
         * All Dua for the result
         */
        DUA("dua"),
        /**
         * All fields for the result or None except Id
         */
        ALL("all"),
        /**
         * Qualifiers field
         */
        QUALIFIERS("qualifiers"),
        /**
         * Object size
         */
        SIZE("size"),
        /**
         * Object format
         */
        FORMAT("format"),
        /**
         * Unit/ObjectGroup type
         */
        TYPE("type"),
        /**
         * Unit/ObjectGroup Tenant
         */
        TENANT("tenant"),
        /**
         * Unit's ObjectGroup
         */
        OBJECT("object"),
        /**
         * Unit's immediate parents
         */
        UNITUPS("unitups"),
        /**
         * Unit's MIN distance from root
         */
        MIN("min"),
        /**
         * Unit's MAX distance from root
         */
        MAX("max"),
        /**
         * All Unit's parents
         */
        ALLUNITUPS("allunitups"),
        /**
         * Management bloc
         */
        MANAGEMENT("management"),
        /**
         * unit type bloc
         */
        UNITTYPE("unitType"),
        /**
         * parents arrays
         */
        UDS("uds"),
        /**
         * Unit or GOT's list of participating operations
         */
        OPERATIONS("operations"),
        /**
         * Unit or GOT's initial operation
         */
        OPI("opi"),
        /**
         * originating agency
         */
        ORIGINATING_AGENCY("originating_agency"),
        /**
         * originating agencies
         */
        ORIGINATING_AGENCIES("originating_agencies"),
        /**
         * Storage in OG
         */
        STORAGE("storage"),
        /**
         * Document's version (number of update on document)
         */
        VERSION("version"),
        /**
         * Document's version (number of update on document)
         */
        ATOMIC_VERSION("atomic_version"),
        /**
         * Document's usage (BINARY_MASTER, PHYSICAL_MASTER, DISSEMINATION, ...)
         */
        USAGE("usage"),
        /**
         * Document scoring according to research
         */
        SCORE("score"),
        /**
         * Last persisted date (logbook operation & lifecycle documents)
         */
        LAST_PERSISTED_DATE("lastPersistedDate"),
        /**
         * Parent unit graph
         */
        GRAPH("graph"),
        /**
         * elimination
         */
        ELIMINATION("elimination"),
        /**
         * Graph last peristed date
         */
        GRAPH_LAST_PERISTED_DATE("graph_last_persisted_date"),
        /**
         * Originating agency
         */
        PARENT_ORIGINATING_AGENCIES("parent_originating_agencies"),
        /**
         * Parent unit history
         */
        HISTORY("history"),
        /**
         * Seda Version
         */
        SEDAVERSION("sedaVersion"),
        /**
         * Vitam Implementation Version
         */
        IMPLEMENTATIONVERSION("implementationVersion"),
        /**
         * Vitam computedInheritedRules field
         */
        COMPUTEDINHERITEDRULES("computedInheritedRules"),
        VALIDCOMPUTEDINHERITEDRULES("validComputedInheritedRules");


        private static final String NOT_FOUND = "Not found";
        private final String exactToken;

        /**
         * Constructor Add DEFAULT_HASH_PREFIX before the exactToken (#+exactToken)
         */
        private PROJECTIONARGS(String realName) {
            exactToken = DEFAULT_HASH_PREFIX + realName;
        }

        /**
         * @return the exact token
         */
        public final String exactToken() {
            return exactToken;
        }

        /**
         * Used in projection for getObject
         *
         * @param token the token to valid
         * @return True if this token is valid, even starting with a "_"
         */
        public static final boolean isValid(String token) {
            // Exception for getObject sliced projection
            return token.startsWith("_qualifiers.") || token.equals("mgt") || token.startsWith("_mgt.") ||
                token.startsWith("_storage.");
        }

        /**
         * Parse the given name
         *
         * @param name name of the field to parse
         * @return the corresponding PROJECTIONARGS
         * @throws IllegalArgumentException if not found
         */
        public static final PROJECTIONARGS parse(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            try {
                return PROJECTIONARGS.valueOf(name.toUpperCase());
            } catch (final Exception e) {
                throw new IllegalArgumentException(NOT_FOUND, e);
            }
        }

        /**
         * This function check the field name for know if it's allowed to inserting or updating the given field from
         * external. This check is done by the API-internal by the VarNameAdapter.
         *
         * @param name field name
         * @return True if the field is not allowed, false
         */
        public static boolean notAllowedOnSetExternal(String name) {
            if (name == null || name.isEmpty()) {
                return true;
            }
            if (name.charAt(0) == ParserTokens.DEFAULT_UNDERSCORE_PREFIX_CHAR) {
                return true;
            }
            return false;
        }

        /**
         * Translate intern field to external field. This functionality is used by isNotAnalyzed() and isAnArray()
         *
         * @param name field name
         * @return the PROJECTIONARGS associated
         * @throws IllegalArgumentException if not found
         */
        public static PROJECTIONARGS getPROJECTIONARGS(String name) {
            // TODO Try to find a better way since this is already defined in Metadata
            if (name.charAt(0) == DEFAULT_UNDERSCORE_PREFIX_CHAR) {
                switch (name) {
                    case "_qualifiers.qualifier":
                        return USAGE;
                    case "_qualifiers.versions.FormatIdentification.FormatId":
                        return FORMAT;
                    case "_id":
                        return ID;
                    case "_og":
                        return OBJECT;
                    case "_ops":
                        return OPERATIONS;
                    case "_opi":
                        return OPI;
                    case "_up":
                        return UNITUPS;
                    case "_us":
                        return ALLUNITUPS;
                    case "_tenant":
                        return TENANT;
                    case "_unitType":
                        return UNITTYPE;
                    case "_uds":
                        return UDS;
                    case "_sp":
                        return ORIGINATING_AGENCY;
                    case "_sps":
                        return ORIGINATING_AGENCIES;
                    case "_storage":
                        return STORAGE;
                    case "_qualifiers":
                        return QUALIFIERS;
                    case "_type":
                        return TYPE;
                    case "_nbunits":
                        return NBUNITS;
                    case "_nbobjects":
                        return NBOBJECTS;
                    case "_min":
                        return MIN;
                    case "_max":
                        return MAX;
                    case "_version":
                        return VERSION;
                    case "_score":
                        return SCORE;
                    case "_lastPersistedDate":
                        return LAST_PERSISTED_DATE;
                    case "_us_sp":
                        return PARENT_ORIGINATING_AGENCIES;
                    case "_graph":
                        return GRAPH;
                    case "_elimination":
                        return ELIMINATION;
                    case "_computedInheritedRules":
                        return COMPUTEDINHERITEDRULES;
                    case "_validComputedInheritedRules":
                        return VALIDCOMPUTEDINHERITEDRULES;
                    case "_glpd":
                        return GRAPH_LAST_PERISTED_DATE;
                    case "_history":
                        return HISTORY;
                    case "_sedaVersion":
                        return SEDAVERSION;
                    case "_implementationVersion":
                        return IMPLEMENTATIONVERSION;
                    default:
                }
            } else if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                // Check on prefix (preceding '.')
                final int pos = name.indexOf('.');
                final String realname;
                if (pos > 1) {
                    realname = name.substring(1, pos);
                } else {
                    realname = name.substring(1);
                }
                try {
                    return PROJECTIONARGS.valueOf(realname.toUpperCase());
                } catch (final Exception e) {
                    // Ignore
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            throw new IllegalArgumentException("Not found");
        }

        /**
         * Return True if attribute is not analyzed
         *
         * @param name field name
         * @return True if attribute is not analyzed
         */
        public static boolean isNotAnalyzed(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }

            try {
                Boolean isFieldAnalyzed = getAnalyzedFlagFromCachedOntologies(getKeyFromPathName(name));
                if (isFieldAnalyzed != null) {
                    return Boolean.FALSE.equals(isFieldAnalyzed);
                } else {
                    SysErrLogger.FAKE_LOGGER.syserr("Unable to use ontology to check if field is not analyzed " + name);
                    return NON_ANALYZED_FIELDS.contains(name);
                }
            } catch (Exception e) {
                // TODO : handle exception properly
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                return false;
            }
        }

        /**
         * Used in Set for Update
         *
         * @param name field name
         * @return True if attribute is an array
         */
        public static boolean isAnArray(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            try {
                final PROJECTIONARGS proj = getPROJECTIONARGS(name);
                switch (proj) {
                    case ID:
                    case NBUNITS:
                    case NBOBJECTS:
                    case SIZE:
                    case FORMAT:
                    case TYPE:
                    case TENANT:
                    case OBJECT:
                    case MIN:
                    case MAX:
                    case MANAGEMENT:
                    case UNITTYPE:
                    case ORIGINATING_AGENCY:
                    case STORAGE:
                    case VERSION:
                    case LAST_PERSISTED_DATE:
                    case UDS:
                    case PARENT_ORIGINATING_AGENCIES:
                    case COMPUTEDINHERITEDRULES:
                        return false;
                    case ELIMINATION:
                        return true;
                    default:
                        break;

                }
            } catch (final IllegalArgumentException e) {
                // Ignore
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            // FIXME Patch for Single collection and Metadata
            return ParserTokens.isAnArrayVariable(name);
        }

        /**
         * This function check the field name for know if it's allowed to inserting or updating the given field from
         * external. This check is done by the API-internal by the VarNameAdapter.
         *
         * @param name field name
         * @return True if this value is not allowed on set (insert, update)
         */
        public static boolean notAllowedOnSet(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                // Check on prefix (preceding '.')
                int pos = name.indexOf('.');
                final String realname;
                if (pos > 1) {
                    realname = name.substring(1, pos);
                } else {
                    realname = name.substring(1);
                }
                try {
                    final PROJECTIONARGS proj = PROJECTIONARGS.valueOf(realname.toUpperCase());
                    switch (proj) {
                        case ALL:
                        case FORMAT:
                        case ID:
                        case NBUNITS:
                        case NBOBJECTS:
                        case QUALIFIERS:
                        case SIZE:
                        case OBJECT:
                        case UNITUPS:
                        case ALLUNITUPS:
                        case TENANT:
                        case MIN:
                        case MAX:
                        case UNITTYPE:
                        case ORIGINATING_AGENCY:
                        case ORIGINATING_AGENCIES:
                        case VERSION:
                        case USAGE:
                        case OPERATIONS:
                        case OPI:
                        case SCORE:
                        case LAST_PERSISTED_DATE:
                        case GRAPH:
                        case GRAPH_LAST_PERISTED_DATE:
                        case HISTORY:
                        case ELIMINATION:
                        case PARENT_ORIGINATING_AGENCIES:
                        case SEDAVERSION:
                        case IMPLEMENTATIONVERSION:
                        case STORAGE:
                        case COMPUTEDINHERITEDRULES:
                            return true;
                        default:
                    }
                } catch (final Exception e) {
                    // Ignore
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            return ParserTokens.isSingleProtectedVariable(name);
        }
    }

    /**
     * Check field name for know if it is protected for single collection
     *
     * @param name name of the field
     * @return True if the name is a protected one
     */
    public static boolean isSingleProtectedVariable(String name) {
        // FIXME Patch for Single collection
        switch (name) {
            case "Identifier":
            case "PUID":
                return true;
            default:
        }
        return false;
    }

    /**
     * Check field name for know if it is an array field for single collection or multiple collection
     *
     * @param name name of the field
     * @return True if this is an Array Variable
     */
    public static boolean isAnArrayVariable(String name) {
        // FIXME Patch for Single collection
        switch (name) {
            // Preservation
            case "ExecutableName":
            case "ExecutableVersion":
            case "DefaultGriffin":

                // context
            case "SecurityProfile":
            case "EnableControl":
                // Formats
            case "Status":
            case "Name":
            case "Description":
            case "PUID":
            case "VersionPronom":
            case "Group":
            case "MimeType":
            case "CreatedDate":
            case "Version":
            case "Alert":
            case "Comment":
                // Profile
            case "Format":
            case "Path":
                // Rule
            case "RuleId":
            case "RuleType":
            case "RuleMeasurement":
            case "RuleValue":
            case "RuleDescription":
            case "RuleDuration":
                // ArchiveUnitProfile
            case "ControlSchema":
                // Ingest contracts
            case "LinkParentId":
            case "CheckParentLink":
            case "CheckParentId":
            case "MasterMandatory":
            case "FormatUnidentifiedAuthorized":
            case "EveryFormatType":
            case "FormatType":
                // Access contracts
            case "Identifier":
            case "CreationDate":
            case "LastUpdate":
            case "ActivationDate":
            case "DeactivationDate":
            case "DataObjectVersion":
            case "WritingPermission":
            case "WritingRestrictedDesc":
            case "EveryOriginatingAgency":
            case "EveryDataObjectVersion":
            case "AccessLog":
                // AccessionRegisterDetails
            case "SubmissionAgency":
            case "ArchivalAgreement":
            case "EndDate":
            case "StartDate":
            case "TotalObjectGroups":
            case "TotalObjectGroups.ingested":
            case "TotalObjectGroups.deleted":
            case "TotalObjectGroups.remained":
            case "TotalUnits":
            case "TotalUnits.ingested":
            case "TotalUnits.deleted":
            case "TotalUnits.remained":
            case "TotalObjects":
            case "TotalObjects.ingested":
            case "TotalObjects.deleted":
            case "TotalObjects.remained":
            case "ObjectSize":
            case "ObjectSize.ingested":
            case "ObjectSize.deleted":
            case "ObjectSize.remained":
            case "OriginatingAgency":
            case "Events.Opc":
            case "Events.OpType":
            case "Events.Gots":
            case "Events.Units":
            case "Events.Objects":
            case "Events.ObjSize":
            case "Events.CreationDate":

                // Sequence
            case "Counter":
                // Logbook
            case "evId":
            case "evParentId":
            case "evType":
            case "evDateTime":
            case "evIdProc":
            case "evTypeProc":
            case "outcome":
            case "outDetail":
            case "outMessg":
            case "agId":
            case "obId":
            case "evDetData":
            case "events.evId":
            case "events.evType":
            case "events.evDateTime":
            case "events.evIdProc":
            case "events.evTypeProc":
            case "events.outcome":
            case "events.outDetail":
            case "events.outMessg":
            case "events.agId":
            case "events.obId":
            case "events.evDetData":
            case "agIdApp":
            case "agIdPers":
            case "evIdAppSession":
            case "evIdReq":
            case "agIdExt":
            case "obIdReq":
            case "obIdIn":
            case "events.agIdApp":
            case "events.evIdAppSession":
            case "events.evIdReq":
            case "events.agIdSubm":
            case "events.obIdReq":
            case "events.obIdIn":
                // Unit
            case "DescriptionLevel":
            case "ArchiveUnitProfile":
            case "LevelType":
            case "Title":
            case "FilePlanPosition":
            case "SystemId":
            case "OriginatingSystemId":
            case "ArchivalAgencyArchiveUnitIdentifier":
            case "originatingAgencyArchiveUnitIdentifier":
            case "TransferringAgencyArchiveUnitIdentifier":
            case "CustodialHistory":
            case "Type":
            case "DocumentType":
            case "Language":
            case "DescriptionLanguage":
            case "Coverage":
            case "AuthorizedAgent":
            case "Source":
            case "RelatedObjectReference":
            case "TransactedDate":
            case "AcquiredDate":
            case "SentDate":
            case "ReceivedDate":
            case "RegisteredDate":
            case "Signature":
            case "Gps":
            case "_nbc":
            case "SedaField":
            case "ApiField":
            case "ShortName":
            case "OperationId":
            case "GlobalStatus":
            case "ExtendedInfoType":
            case "ExtendedInfoDetails":
            case "ParentUnitId":
            case "_mgt.NeedAuthorization":
                // OG
                return false;
            default:
        }
        if (SUB_FIELDS_NOT_ARRAY.parallelStream().anyMatch(name::contains)) {
            return false;
        } else if (name.startsWith("Title_") || name.startsWith("Description_")) {
            return false;
        }
        return true;
    }

    private static class OntologiesLoader implements Runnable {

        // TODO : change period
        private Integer period = VitamConfiguration.getExpireCacheEntriesDelay(); // default 5min

        public OntologiesLoader() {
            Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance()).scheduleAtFixedRate(this, 0, period, TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());
            loadOntologies();
        }
    }
}
