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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.logging.SysErrLogger;

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
    private static final List<String> SUB_FIELDS_NOT_ARRAY = Arrays.asList(new String[] {
        "evDetData",
        "FirstName", "Corpname", "Gender", "BirthPlace", "GivenName", "DeathPlace", "BirthDate", "Identifier",
        "BirthName", "DeathDate", "Nationality", "UpdateDate",
        "Function", "Activity", "Position", "Role",
        "Geogname", "Address", "PostalCode", "City", "Region", "Country",
        "PhysicalId", "Filename", "MessageDigest", "Size", "FormatIdentification", "FileInfo", "Metadata",
        "OtherMetadata", "_nbc",
        "Algorithm", "DataObjectGroupId", "DataObjectVersion", "strategyId",
        "EndDate", "Rule", "PreventInheritance", "StartDate", "FinalAction",
        "ClassificationLevel", "ClassificationOwner", "ClassificationReassessingDate", "NeedReassessingAuthorization"
    });

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
        "Signature.Masterdata",
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
        "Writer.Activity",
        "Writer.BirthDate",
        "Writer.BirthPlace.PostalCode",
        "Writer.DeathDate",
        "Writer.DeathPlace.PostalCode",
        "Writer.Function",
        "Writer.Identifier",
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
        "_mgt.ClassificationRule.Rules.ClassificationLevel",
        "_mgt.ClassificationRule.Rules.ClassificationReassessingDate",
        "_mgt.ClassificationRule.Rules.EndDate",
        "_mgt.ClassificationRule.Rules.NeedReassessingAuthorization",
        "_mgt.ClassificationRule.Rules.Rule",
        "_mgt.ClassificationRule.Rules.StartDate",
        "_mgt.DisseminationRule.Inheritance.PreventInheritance",
        "_mgt.DisseminationRule.Inheritance.PreventRulesId",
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
        "_qualifiers.versions.FileInfo.DateCreatedByApplication",
        "_qualifiers.versions.FileInfo.LastModified",
        "_qualifiers.versions.FormatIdentification.FormatId",
        "_qualifiers.versions.MessageDigest",
        "_qualifiers.versions.PhysicalDimensions.Depth.unit",
        "_qualifiers.versions.PhysicalDimensions.Depth.value",
        "_qualifiers.versions.PhysicalDimensions.Diameter.unit",
        "_qualifiers.versions.PhysicalDimensions.Diameter.value",
        "_qualifiers.versions.PhysicalDimensions.Height.unit",
        "_qualifiers.versions.PhysicalDimensions.Height.value",
        "_qualifiers.versions.PhysicalDimensions.Length.unit",
        "_qualifiers.versions.PhysicalDimensions.Length.value",
        "_qualifiers.versions.PhysicalDimensions.NumberOfPage",
        "_qualifiers.versions.PhysicalDimensions.Shape",
        "_qualifiers.versions.PhysicalDimensions.Thickness.unit",
        "_qualifiers.versions.PhysicalDimensions.Thickness.value",
        "_qualifiers.versions.PhysicalDimensions.Weight.unit",
        "_qualifiers.versions.PhysicalDimensions.Weight.value",
        "_qualifiers.versions.PhysicalDimensions.Width.unit",
        "_qualifiers.versions.PhysicalDimensions.Width.value",
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
        "EveryOriginatingAgency",
        "EveryDataObjectVersion",
        "RootUnits",
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
        "Permissions._tenant",
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
        "TotalObjects.attached",
        "TotalObjects.detached",
        "TotalObjects.symbolicRemained",
        "TotalObjectGroups.ingested",
        "TotalObjectGroups.deleted",
        "TotalObjectGroups.remained",
        "TotalObjectGroups.attached",
        "TotalObjectGroups.detached",
        "TotalObjectGroups.symbolicRemained",
        "TotalUnits.ingested",
        "TotalUnits.deleted",
        "TotalUnits.remained",
        "TotalUnits.attached",
        "TotalUnits.detached",
        "TotalUnits.symbolicRemained",
        "ObjectSize.ingested",
        "ObjectSize.deleted",
        "ObjectSize.remained",
        "ObjectSize.attached",
        "ObjectSize.detached",
        "ObjectSize.symbolicRemained",
        "_id",
        "_tenant",
        "_v",
        "_score",
        /* Accession register detail */
        "OriginatingAgency",
        "SubmissionAgency",
        "ArchivalAgreement",
        "EndDate",
        "StartDate",
        "LastUpdate",
        "Status",
        "TotalObjects.ingested",
        "TotalObjects.deleted",
        "TotalObjects.remained",
        "TotalObjects.attached",
        "TotalObjects.detached",
        "TotalObjects.symbolicRemained",
        "TotalObjectGroups.ingested",
        "TotalObjectGroups.deleted",
        "TotalObjectGroups.remained",
        "TotalObjectGroups.attached",
        "TotalObjectGroups.detached",
        "TotalObjectGroups.symbolicRemained",
        "TotalUnits.ingested",
        "TotalUnits.deleted",
        "TotalUnits.remained",
        "TotalUnits.attached",
        "TotalUnits.detached",
        "TotalUnits.symbolicRemained",
        "ObjectSize.ingested",
        "ObjectSize.deleted",
        "ObjectSize.remained",
        "ObjectSize.attached",
        "ObjectSize.detached",
        "ObjectSize.symbolicRemained",
        "OperationIds",
        "Symbolic",
        "_id",
        "_tenant",
        "_v",
        "_score"));

    private ParserTokens() {
        // Empty
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
        PARENTS("uds"),
        /**
         * Unit or GOT's list of participating operations
         */
        OPERATIONS("operations"),
        /**
         * Unit or GOT's initial operation
         */
        INITIAL_OPERATION("opi"),
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
        LAST_PERSISTED_DATE("lastPersistedDate");



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
        private static PROJECTIONARGS getPROJECTIONARGS(String name) {
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
                        return INITIAL_OPERATION;
                    case "_up":
                        return UNITUPS;
                    case "_us":
                        return ALLUNITUPS;
                    case "_tenant":
                        return TENANT;
                    case "_unitType":
                        return UNITTYPE;
                    case "_uds":
                        return PARENTS;
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

            // Quick & dirty implementation.
            // FIXME : Replace this unmaintainable code using a cleaner / more robust way.

            return NON_ANALYZED_FIELDS.contains(name);
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
                        return false;
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
                        case INITIAL_OPERATION:
                        case SCORE:
                        case LAST_PERSISTED_DATE:
                            return true;
                        case STORAGE:
                            // FIXME should consider more security on this one!
                            return false;
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
            case "MIMEType":
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
                // Ingest contracts
            case "LinkParentId":
                // Access contracts
            case "Identifier":
            case "CreationDate":
            case "LastUpdate":
            case "ActivationDate":
            case "DeactivationDate":
            case "DataObjectVersion":
            case "WritingPermission":
            case "EveryOriginatingAgency":
            case "EveryDataObjectVersion":
                // AccessionRegisterDetails
            case "SubmissionAgency":
            case "ArchivalAgreement":
            case "EndDate":
            case "StartDate":
            case "TotalObjectGroups":
            case "TotalObjectGroups.ingested":
            case "TotalObjectGroups.deleted":
            case "TotalObjectGroups.remained":
            case "TotalObjectGroups.detached":
            case "TotalObjectGroups.attached":
            case "TotalObjectGroups.symbolicRemained":
            case "TotalUnits":
            case "TotalUnits.ingested":
            case "TotalUnits.deleted":
            case "TotalUnits.remained":
            case "TotalUnits.detached":
            case "TotalUnits.attached":
            case "TotalUnits.symbolicRemained":
            case "TotalObjects":
            case "TotalObjects.ingested":
            case "TotalObjects.deleted":
            case "TotalObjects.remained":
            case "TotalObjects.detached":
            case "TotalObjects.attached":
            case "TotalObjects.symbolicRemained":
            case "ObjectSize":
            case "ObjectSize.ingested":
            case "ObjectSize.deleted":
            case "ObjectSize.remained":
            case "ObjectSize.detached":
            case "ObjectSize.attached":
            case "ObjectSize.symbolicRemained":
            case "OriginatingAgency":
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
                // OG
                return false;
            default:
        }
        if (SUB_FIELDS_NOT_ARRAY.parallelStream().anyMatch(name::contains)) {
            return false;
        } else if (name.startsWith("Title_")) {
            return false;
        }
        return true;
    }
}
