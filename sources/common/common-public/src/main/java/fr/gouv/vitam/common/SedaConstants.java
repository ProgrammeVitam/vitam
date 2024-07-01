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
package fr.gouv.vitam.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Constants for the Seda used in Json files
 */
public class SedaConstants {

    /**
     * versions element
     */
    public static final String TAG_VERSIONS = "versions";

    /**
     * Uri element
     */
    public static final String TAG_URI = "Uri";

    /**
     * File Info element
     */
    public static final String TAG_FILE_INFO = "FileInfo";

    /**
     * Tag of nb
     */
    public static final String TAG_NB = "_nbc";

    /**
     * Tag of size
     */
    public static final String TAG_SIZE = "Size";

    /**
     * Tag of messageDigest
     */
    public static final String TAG_DIGEST = "MessageDigest";

    /**
     * Tag of algorithm
     */
    public static final String ALGORITHM = "Algorithm";

    /**
     * Tag for storage in objectGroup
     */
    public static final String STORAGE = "_storage";

    /**
     * Tag in storage
     */
    public static final String OFFER_IDS = "offerIds";

    /**
     * Strategy Id
     */
    public static final String STRATEGY_ID = "strategyId";

    /**
     * Tag of DataObjectVersion
     */
    public static final String TAG_DO_VERSION = "DataObjectVersion";

    /**
     * Tag of MessageIdentifier
     */
    public static final String TAG_MESSAGE_IDENTIFIER = "MessageIdentifier";

    /**
     * Tag of BinaryDataObject
     */
    public static final String TAG_BINARY_DATA_OBJECT = "BinaryDataObject";

    /**
     * Tag of PhysicalDataObject
     */
    public static final String TAG_PHYSICAL_DATA_OBJECT = "PhysicalDataObject";

    /**
     * Tag of FormatIdentification
     */
    public static final String TAG_FORMAT_IDENTIFICATION = "FormatIdentification";

    /**
     * Tag of FormatId
     */
    public static final String TAG_FORMAT_ID = "FormatId";

    /**
     * Tag of FormatLitteral
     */
    public static final String TAG_FORMAT_LITTERAL = "FormatLitteral";

    /**
     * Tag of Mimetype
     */
    public static final String TAG_MIME_TYPE = "MimeType";

    /**
     * Tag of DataObjectGroupReferenceId
     */
    public static final String TAG_DATA_OBJECT_GROUP_REFERENCEID = "DataObjectGroupReferenceId";

    /**
     * Tag of DataObjectGroupExistingReferenceId
     */
    public static final String TAG_DATA_OBJECT_GROUP_EXISTING_REFERENCEID = "DataObjectGroupExistingReferenceId";

    /**
     * Tag of RelatedObjectReference
     */
    public static final String TAG_RELATED_OBJECT_REFERENCE = "RelatedObjectReference";

    /**
     * Tag of RelatedTransferReference
     */
    public static final String TAG_RELATED_TRANSFER_REFERENCE = "RelatedTransferReference";

    /**
     * Tag of TransferRequestReplyIdentifier
     */
    public static final String TAG_TRANSFER_REQUEST_REPLY_IDENTIFIER = "TransferRequestReplyIdentifier";

    /**
     * Tag of Requester
     */
    public static final String TAG_REQUESTER = "Requester";

    /**
     * Tag of AuthorizationRequestReplyIdentifier
     */
    public static final String TAG_AUTHORIZATION_REQUEST_REPLY_IDENTIFIER = "AuthorizationRequestReplyIdentifier";

    /**
     * Tag of DataObjectReferenceId
     */
    public static final String TAG_DATA_OBJECT_REFERENCEID = "DataObjectReferenceId";
    /**
     * Tag of PhysicalId
     */
    public static final String TAG_PHYSICAL_ID = "PhysicalId";

    /**
     * Tag of dValue : used for data with an attibute
     */
    public static final String TAG_D_VALUE = "dValue";

    /**
     * Prefix of id element
     */
    public static final String PREFIX_ID = "_id";

    /**
     * Prefix of object group
     */
    public static final String PREFIX_OG = "_og";

    /**
     * Prefix of work element in the OG
     */
    public static final String PREFIX_WORK = "_work";

    /**
     * Prefix to be added in _work, it can be boolean or object. Used for linked AU to AU and/or GOT
     */
    public static final String PREFIX_EXISTING = "_existing";

    /**
     * Prefix of qualifiers element
     */
    public static final String PREFIX_QUALIFIERS = "_qualifiers";

    /**
     * Prefix of qualifiers element
     */
    public static final String PREFIX_QUALIFIER = "qualifier";

    /**
     * Prefix of up element
     */
    public static final String PREFIX_UP = "_up";

    /**
     * Prefix of tenantID
     */
    public static final String PREFIX_TENANT_ID = "_tenant";

    /**
     * Prefix of nb
     */
    public static final String PREFIX_NB = "_nbc";
    /**
     * Prefix of ops
     */
    public static final String PREFIX_OPS = "_ops";
    /**
     * Prefix of opi
     */
    public static final String PREFIX_OPI = "_opi";
    /**
     * Prefix of type
     */
    public static final String PREFIX_TYPE = "_profil";

    /**
     * Prefix of management
     */
    public static final String PREFIX_MGT = "_mgt";

    /**
     * Prefix of OriginatingAgency
     */
    public static final String PREFIX_ORIGINATING_AGENCY = "_sp";

    /**
     * Prefix of OriginatingAgency
     */
    public static final String PREFIX_ORIGINATING_AGENCIES = "_sps";

    /**
     * Date format patern
     */
    public static final String DATE_FORMAT_PATERN = "yyyy-MM-dd";

    /**
     * Date time format patern
     */
    public static final String DATE_TIME_FORMAT_PATERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Rule separator in work
     */
    public static final String RULE_SEPARATOR = ";";

    // XML Tags used in SEDA
    /**
     * tag of ArchiveDeliveryRequestReply
     */
    public static final String TAG_ARCHIVE_DELIVERY_REQUEST_REPLY = "ArchiveDeliveryRequestReply";
    public static final String TAG_COMMENT = "Comment";
    /**
     * tag of Rules
     */
    public static final String TAG_RULES = "Rules";
    /**
     * tag of Date
     */
    public static final String TAG_DATE = "Date";
    /**
     * tag of ArchivalAgreement
     */
    public static final String TAG_ARCHIVAL_AGREEMENT = "ArchivalAgreement";

    /**
     * tag of ArchiveProfile
     */
    public static final String TAG_ARCHIVE_PROFILE = "ArchivalProfile";

    /**
     * tag of ServiceLevel
     */

    public static final String TAG_SERVICE_LEVEL = "ServiceLevel";
    /**
     * tag of DataObjectPackage
     */
    public static final String TAG_DATA_OBJECT_PACKAGE = "DataObjectPackage";
    /**
     * tag of DescriptiveMetadata
     */
    public static final String TAG_DESCRIPTIVE_METADATA = "DescriptiveMetadata";
    /**
     * tag of ManagementMetadata
     */
    public static final String TAG_MANAGEMENT_METADATA = "ManagementMetadata";
    /**
     * tag of ReplyOutcome
     */
    public static final String TAG_REPLY_OUTCOME = "ReplyOutcome";
    /**
     * tag of ArchiveUnitList
     */
    public static final String TAG_ARCHIVE_UNIT_LIST = "ArchiveUnitList";
    /**
     * tag of DataObjectList
     */
    public static final String TAG_DATA_OBJECT_LIST = "DataObjectList";
    /**
     * tag of ReplyCode
     */
    public static final String TAG_REPLY_CODE = "ReplyCode";
    /**
     * tag of ArchivalAgency
     */
    public static final String TAG_ARCHIVAL_AGENCY = "ArchivalAgency";

    /**
     * tag of TransferringAgency
     */
    public static final String TAG_TRANSFERRING_AGENCY = "TransferringAgency";
    /**
     * tag of Identifier
     */
    public static final String TAG_IDENTIFIER = "Identifier";
    /**
     * tag of MessageRequestIdentifier
     */
    public static final String TAG_MESSAGE_REQUEST_IDENTIFIER = "MessageRequestIdentifier";
    /**
     * tag of UnitIdentifier
     */
    public static final String TAG_UNIT_IDENTIFIER = "UnitIdentifier";
    /**
     * tag of CodeListVersions
     */
    public static final String TAG_CODE_LIST_VERSIONS = "CodeListVersions";
    /**
     * tag of ReplyCodeListVersion
     */
    public static final String TAG_REPLY_CODE_LIST_VERSION = "ReplyCodeListVersion";
    /**
     * tag of MimeTypeCodeListVersion
     */
    public static final String TAG_MIME_TYPE_CODE_LIST_VERSION = "MimeTypeCodeListVersion";
    /**
     * tag of EncodingCodeListVersion
     */
    public static final String TAG_ENCODING_CODE_LIST_VERSION = "EncodingCodeListVersion";
    /**
     * tag of MessageDigestAlgorithmCodeListVersion
     */
    public static final String TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION = "MessageDigestAlgorithmCodeListVersion";
    /**
     * tag of CompressionAlgorithmCodeListVersion
     */
    public static final String TAG_COMPRESSION_ALGORITHM_CODE_LIST_VERSION = "CompressionAlgorithmCodeListVersion";
    /**
     * tag of FileFormatCodeListVersion
     */
    public static final String TAG_FILE_FORMAT_CODE_LIST_VERSION = "FileFormatCodeListVersion";
    /**
     * tag of DataObjectVersionCodeListVersion
     */
    public static final String TAG_DATA_OBJECT_VERSION_CODE_LIST_VERSION = "DataObjectVersionCodeListVersion";
    /**
     * tag of StorageRuleCodeListVersion
     */
    public static final String TAG_STORAGE_RULE_CODE_LIST_VERSION = "StorageRuleCodeListVersion";
    /**
     * tag of AppraisalRuleCodeListVersion
     */
    public static final String TAG_APPRAISAL_RULE_CODE_LIST_VERSION = "AppraisalRuleCodeListVersion";
    /**
     * tag of AccessRuleCodeListVersion
     */
    public static final String TAG_ACCESS_RULE_CODE_LIST_VERSION = "AccessRuleCodeListVersion";
    /**
     * tag of DisseminationRuleCodeListVersion
     */
    public static final String TAG_DISSEMINATION_RULE_CODE_LIST_VERSION = "DisseminationRuleCodeListVersion";
    /**
     * tag of ReuseRuleCodeListVersion
     */
    public static final String TAG_REUSE_RULE_CODE_LIST_VERSION = "ReuseRuleCodeListVersion";
    /**
     * tag of ClassificationRuleCodeListVersion
     */
    public static final String TAG_CLASSIFICATION_RULE_CODE_LIST_VERSION = "ClassificationRuleCodeListVersion";
    /**
     * tag of AuthorizationReasonCodeListVersion
     */
    public static final String TAG_AUTHORIZATION_REASON_CODE_LIST_VERSION = "AuthorizationReasonCodeListVersion";
    /**
     * tag of RelationshipCodeListVersion
     */
    public static final String TAG_RELATIONSHIP_CODE_LIST_VERSION = "RelationshipCodeListVersion";
    /**
     * tag of ArchiveTransfer
     */
    public static final String TAG_ARCHIVE_TRANSFER = "ArchiveTransfer";
    /**
     * tag of ArchiveUnit
     */
    public static final String TAG_ARCHIVE_UNIT_PROFILE = "ArchiveUnitProfile";
    /**
     * tag of GrantDate
     */
    public static final String TAG_GRANT_DATE = "GrantDate";
    /**
     * tag of OrganizationDescriptiveMetadata
     */
    public static final String TAG_ORGANIZATIONDESCRIPTIVEMETADATA = "OrganizationDescriptiveMetadata";
    /**
     * tag of OriginatingAgency
     */
    public static final String TAG_ORIGINATINGAGENCY = "OriginatingAgency";

    /**
     * tag of AcquisitionInformation
     */
    public static final String TAG_ACQUISITIONINFORMATION = "AcquisitionInformation";

    /**
     * tag of LegalStatus
     */
    public static final String TAG_LEGALSTATUS = "LegalStatus";

    /**
     * tag of OriginatingAgencyIdentifier
     */
    public static final String TAG_ORIGINATINGAGENCYIDENTIFIER = "OriginatingAgencyIdentifier";
    /**
     * tag of OriginatingAgencyIdentifier
     */
    public static final String TAG_ORIGINATINGAGENCYIDENTIFIERS = "OriginatingAgencyIdentifiers";
    /**
     * tag of SubmissionAgencyIdentifier
     */
    public static final String TAG_SUBMISSIONAGENCYIDENTIFIER = "SubmissionAgencyIdentifier";
    /**
     * tag of DataObjectGroupId
     */
    public static final String TAG_DATA_OBJECT_GROUPE_ID = "DataObjectGroupId";
    /**
     * tag of StorageRule
     */
    public static final String TAG_RULE_STORAGE = "StorageRule";
    /**
     * tag of AppraisalRule
     */
    public static final String TAG_RULE_APPRAISAL = "AppraisalRule";
    /**
     * tag of AccessRule
     */
    public static final String TAG_RULE_ACCESS = "AccessRule";
    /**
     * tag of DisseminationRule
     */
    public static final String TAG_RULE_DISSEMINATION = "DisseminationRule";
    /**
     * tag of ReuseRule
     */
    public static final String TAG_RULE_REUSE = "ReuseRule";
    /**
     * tag of ClassificationRule
     */
    public static final String TAG_RULE_CLASSIFICATION = "ClassificationRule";
    /**
     * tag of HoldRule
     */
    public static final String TAG_RULE_HOLD = "HoldRule";
    /**
     * tag of Rule
     */
    public static final String TAG_RULE_RULE = "Rule";
    /**
     * tag of StartDate
     */
    public static final String TAG_RULE_START_DATE = "StartDate";

    /**
     * tag of FinalAction
     */
    public static final String TAG_RULE_FINAL_ACTION = "FinalAction";
    /**
     * tag PreventInheritance
     */
    public static final String TAG_RULE_PREVENT_INHERITANCE = "PreventInheritance";
    /**
     * tag of RefNonRuleId
     */
    public static final String TAG_RULE_REF_NON_RULE_ID = "RefNonRuleId";
    /**
     * tag of ClassificationLevel
     */
    public static final String TAG_RULE_CLASSIFICATION_LEVEL = "ClassificationLevel";
    /**
     * tag of ClassificationOwner
     */
    public static final String TAG_RULE_CLASSIFICATION_OWNER = "ClassificationOwner";
    /**
     * tag of ClassificationAudience
     */
    public static final String TAG_RULE_CLASSIFICATION_AUDIENCE = "ClassificationAudience";
    /**
     * tag of ClassificationReassessingDate
     */
    public static final String TAG_RULE_CLASSIFICATION_REASSESSING_DATE = "ClassificationReassessingDate";
    /**
     * tag of NeedReassessingAuthorization
     */
    public static final String TAG_RULE_CLASSIFICATION_NEED_REASSESSING_AUTHORIZATION = "NeedReassessingAuthorization";
    /**
     * tag of NeedAuthorization
     */
    public static final String TAG_RULE_NEED_AUTHORISATION = "NeedAuthorization";

    public static final String TAG_RULE_HOLD_END_DATE = "HoldEndDate";
    public static final String TAG_RULE_HOLD_OWNER = "HoldOwner";
    public static final String TAG_RULE_HOLD_REASSESSING_DATE = "HoldReassessingDate";
    public static final String TAG_RULE_HOLD_REASON = "HoldReason";
    public static final String TAG_RULE_PREVENT_REARRANGEMENT = "PreventRearrangement";

    // Tag's attributes used in SEDA
    public static final String TAG_ATTRIBUTE_LANG = "lang";
    public static final String TAG_ATTRIBUTE_UNIT = "unit";

    /**
     * Prefix of rules will be applicated to archive unit
     */
    public static final String TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT = "RulesToApply";
    /**
     * Prefix of rules end date
     */
    public static final String TAG_RULE_END_DATE = "EndDate";
    private static List<String> RULES_TYPE;

    /**
     * tag of Operation
     */
    public static final String TAG_OPERATION = "Operation";
    /**
     * tag of Event
     */
    public static final String TAG_EVENT = "Event";
    /**
     * tag of EventTyoe
     */
    public static final String TAG_EVENT_TYPE = "EventType";
    /**
     * tag of EventTypeCode
     */
    public static final String TAG_EVENT_TYPE_CODE = "EventTypeCode";
    /**
     * tag of EventDateTime
     */
    public static final String TAG_EVENT_DATE_TIME = "EventDateTime";
    /**
     * tag of Outcome
     */
    public static final String TAG_EVENT_OUTCOME = "Outcome";
    /**
     * tag of OutcomeDetail
     */
    public static final String TAG_EVENT_OUTCOME_DETAIL = "OutcomeDetail";
    /**
     * tag of OutcomeDetailMessage
     */
    public static final String TAG_EVENT_OUTCOME_DETAIL_MESSAGE = "OutcomeDetailMessage";
    /**
     * tag of EventDetailData
     */
    public static final String TAG_EVENT_DETAIL_DATA = "EventDetailData";
    /**
     * attribute id of archive unit
     */
    public static final String ATTRIBUTE_ID = "id";
    /**
     * tag of ArchiveUnit
     */
    public static final String TAG_ARCHIVE_UNIT = "ArchiveUnit";
    /**
     * tag of SystemId
     */
    public static final String TAG_ARCHIVE_SYSTEM_ID = "SystemId";
    /**
     * update operation
     */
    public static final String UPDATE_OPERATION = "UpdateOperation";
    /**
     * tag of DataObjectGroup
     */
    public static final String TAG_DATA_OBJECT_GROUP = "DataObjectGroup";
    /**
     * tag of BinaryDataObjectID
     */
    public static final String TAG_BINARY_DATA_OBJECT_ID = "BinaryDataObjectID";
    /**
     * tag of DataObjectSystemId
     */
    public static final String TAG_DATA_OBJECT_SYSTEM_ID = "DataObjectSystemId";

    /**
     * tag of DataObjectGroupSystemId
     */
    public static final String TAG_DATA_OBJECT_GROUP_SYSTEM_ID = "DataObjectGroupSystemId";

    /**
     * evDetTechData
     */
    public static final String EV_DET_TECH_DATA = "evDetTechData";

    /**
     * the namespace xlink
     */
    public static final String NAMESPACE_XLINK = "xlink";
    /**
     * the namespace pr
     */
    public static final String NAMESPACE_PR = "pr";
    /**
     * the namespace xsi
     */
    public static final String NAMESPACE_XSI = "xsi";
    /**
     * the attribute schemaLocation
     */
    public static final String ATTRIBUTE_SCHEMA_LOCATION = "schemaLocation";

    /**
     * reparing namespace property
     */
    public static final String STAX_PROPERTY_PREFIX_OUTPUT_SIDE = "javax.xml.stream.isRepairingNamespaces";

    /**
     * LFC OBJECTS FOLDER path
     */
    public static final String LFC_OBJECTS_FOLDER = "LFCObjects";

    /**
     * LFC UNITS FOLDER path
     */
    public static final String LFC_UNITS_FOLDER = "LFCUnits";

    public static final String TAG_LOGBOOK = "LogBook";

    private SedaConstants() {
        // Empty constructor
    }

    /**
     * @return supported Rules type
     */
    public static List<String> getSupportedRules() {
        if (RULES_TYPE == null) {
            RULES_TYPE = new ArrayList<>();
            RULES_TYPE.add(SedaConstants.TAG_RULE_ACCESS);
            RULES_TYPE.add(SedaConstants.TAG_RULE_REUSE);
            RULES_TYPE.add(SedaConstants.TAG_RULE_STORAGE);
            RULES_TYPE.add(SedaConstants.TAG_RULE_APPRAISAL);
            RULES_TYPE.add(SedaConstants.TAG_RULE_CLASSIFICATION);
            RULES_TYPE.add(SedaConstants.TAG_RULE_DISSEMINATION);
            RULES_TYPE.add(SedaConstants.TAG_RULE_HOLD);
        }
        return RULES_TYPE;
    }
}
