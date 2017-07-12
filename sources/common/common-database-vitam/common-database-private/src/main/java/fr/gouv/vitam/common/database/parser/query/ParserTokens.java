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
import java.util.List;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Main language definition
 *
 *
 */
public class ParserTokens extends BuilderToken {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ParserTokens.class);
    
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

    private static final List<String>  SUB_FIELDS_NOT_ARRAY  = Arrays.asList( new String[] {
        "evDetData",
        "FirstName", "Corpname", "Gender", "BirthPlace", "GivenName", "DeathPlace", "BirthDate", "Identifier", "BirthName", "DeathDate", "Nationality",
        "Function", "Activity", "Position", "Role",
        "Geogname", "Address", "PostalCode", "City", "Region", "Country",
        "PhysicalId", "Filename", "MessageDigest", "Size", "FormatIdentification", "FileInfo", "Metadata", "OtherMetadata",
        "Algorithm", "DataObjectGroupId", "DataObjectVersion", "strategyId",
        "EndDate", "Rule", "PreventInheritance", "StartDate", "FinalAction",
        "ClassificationLevel", "ClassificationOwner", "ClassificationReassessingDate", "NeedReassessingAuthorization"
    } );

    private static final List<String> FIELD_NOT_ANALYZED = Arrays.asList( new String[] {
        "ArchivalAgencyArchiveUnitIdentifier", "ArchiveUnitProfile", "DescriptionLevel", "DocumentType",
        "Identifier", "OriginatingAgencyArchiveUnitIdentifier", "OriginatingSystemId", "SubmissionAgency",
        "SystemId", "TransferringAgencyArchiveUnitIdentifier", "Rule", "RefNonRuleId", "FinalAction",
        "PreventInheritance", "RefNonRuleId", "ClassificationLevel",
        "PhysicalId", "MessageDigest", "Algorithm", "DataObjectGroupId", "DataObjectVersion", "strategyId",
        "unit", "offerIds", "strategyId"
    } );
    private static final List<String> FIELD_ANALYZED = Arrays.asList( new String[] {"RuleValue", "RuleDescription"} );
    
    private ParserTokens() {
        // Empty
    }


    /**
     * specific fields: nbunits, dua, ... <br>
     * $fields : [ #nbunits:1, #dua:1, #all:1... ]
     *
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
        UNITTYPE("unittype"),
        /**
         * Unit or GOT's list of participating operations
         */
        OPERATIONS("operations"),
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
        SCORE("score");



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
            return token.startsWith("_qualifiers.") || token.equals("mgt") || token.startsWith("_mgt.")
                || token.startsWith("_storage.");
        }

        /**
         *
         * @param name String
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
        *
        * @param name
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
                   case "_up":
                       return UNITUPS;
                   case "_us":
                       return ALLUNITUPS;
                   case "_tenant":
                       return TENANT;
                   case "_unitType":
                       return UNITTYPE;
                   case "_sp":
                       return ORIGINATING_AGENCY;
                   case "_sps":
                       return ORIGINATING_AGENCIES;
                   case "_storage":
                       return STORAGE;
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
       *
       * @param name
       * @return True if attribute is not analyzed
       */
      public static boolean isNotAnalyzed(String name) {
          if (name == null || name.isEmpty()) {
              return false;
          }
          try {
              final PROJECTIONARGS proj = getPROJECTIONARGS(name);
              switch (proj) {
                  case ALLUNITUPS:
                  case FORMAT:
                  case ID:
                  case OBJECT:
                  case OPERATIONS:
                  case ORIGINATING_AGENCIES:
                  case ORIGINATING_AGENCY:
                  case QUALIFIERS:
                  case TENANT:
                  case TYPE:
                  case UNITTYPE:
                  case UNITUPS:
                  case USAGE:
                  case NBUNITS:
                  case NBOBJECTS:
                  case MIN:
                  case MAX:
                  case STORAGE:
                  case VERSION:
                  case SCORE:
                      return true;
                  default:
                      break;

              }
          } catch (final IllegalArgumentException e) {
              // Ignore
              SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          }
          return ParserTokens.isSingleNotAnalyzedVariable(name);
      }
      /**
       * Used in Set for Update
       * 
       * @param name
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
         *
         * @param name String
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
                        case SCORE:
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
     * 
     * @param name
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
     * 
     * @param name
     * @return True if this is an Array Variable
     */
    public static boolean isAnArrayVariable(String name) {
        // FIXME Patch for Single collection
        switch (name) {
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
            case "TotalObjectGroups.total":
            case "TotalObjectGroups.deleted":
            case "TotalObjectGroups.remained":
            case "TotalUnits":
            case "TotalUnits.total":
            case "TotalUnits.deleted":
            case "TotalUnits.remained":
            case "TotalObjects":
            case "TotalObjects.total":
            case "TotalObjects.deleted":
            case "TotalObjects.remained":
            case "ObjectSize":
            case "ObjectSize.total":
            case "ObjectSize.deleted":
            case "ObjectSize.remained":
            case "OriginatingAgency":
            // Sequence
            case "Counter":
            // Logbook
            case "evId":
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
            case "agIdAppSession":
            case "evIdReq":
            case "agIdSubm":
            case "agIdOrig":
            case "obIdReq":
            case "obIdIn":
            case "events.agIdApp":
            case "events.agIdAppSession": 
            case "events.evIdReq":
            case "events.agIdSubm":
            case "events.agIdOrig":
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
            // OG
                return false;
            default:
        }
        if (SUB_FIELDS_NOT_ARRAY.parallelStream().anyMatch(name::contains)) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @param name
     * @return True if the name is a protected one
     */
    public static boolean isSingleNotAnalyzedVariable(String name) {
        // FIXME Patch for Single collection
        switch (name) {
            case "Status":
            case "PUID":
            case "RuleId":
            case "Identifier":
            case "VersionPronom":
            case "Group":
            case "Format":
            case "RuleType":
            case "RuleMeasurement":
            case "Path":
            case "Permissions.AccessContracts":
            case "Permissions.IngestContracts":
            case "ArchiveProfiles":
            case "Extension":
                return true;
            default:
        }
        if (!FIELD_ANALYZED.contains(name) && FIELD_NOT_ANALYZED.parallelStream().anyMatch(name::contains)) {
            return true;
        }
        return false;
    }
}
