/**
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
 */
package fr.gouv.vitam.worker.common.utils;

/**
 * Constants for the Seda used in Json files
 */
public class SedaConstants {

    public static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.0";

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
    public static final String TAG_NB = "nb";

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
     * Tag of DataObjectReferenceId
     */
    public static final String TAG_DATA_OBJECT_REFERENCEID = "DataObjectReferenceId";

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
     * Prefix of qualifiers element
     */
    public static final String PREFIX_QUALIFIERS = "_qualifiers";

    /**
     * Prefix of up element
     */
    public static final String PREFIX_UP = "_up";

    /**
     * Prefix of tenantID
     */
    public static final String PREFIX_TENANT_ID = "_tenantId";

    /**
     * Prefix of nb
     */
    public static final String PREFIX_NB = "_nb";
    /**
     * Prefix of ops
     */
    public static final String PREFIX_OPS = "_ops";
    /**
     * Prefix of type
     */
    public static final String PREFIX_TYPE = "_type";

    /**
     * Prefix of management
     */
    public static final String PREFIX_MGT = "_mgt";

    // XML Tags used in SEDA
    public static final String TAG_ARCHIVE_TRANSFER_REPLY = "ArchiveTransferReply";
    public static final String TAG_DATE = "Date";
    public static final String TAG_ARCHIVAL_AGREEMENT = "ArchivalAgreement";
    public static final String TAG_DATA_OBJECT_PACKAGE = "DataObjectPackage";
    public static final String TAG_DESCRIPTIVE_METADATA = "DescriptiveMetadata";
    public static final String TAG_MANAGEMENT_METADATA = "ManagementMetadata";
    public static final String TAG_REPLY_OUTCOME = "ReplyOutcome";
    public static final String TAG_ARCHIVE_UNIT_LIST = "ArchiveUnitList";
    public static final String TAG_DATA_OBJECT_LIST = "DataObjectList";
    public static final String TAG_REPLY_CODE = "ReplyCode";
    public static final String TAG_ARCHIVAL_AGENCY = "ArchivalAgency";
    public static final String TAG_TRANSFERRING_AGENCY = "TransferringAgency";
    public static final String TAG_IDENTIFIER = "Identifier";
    public static final String TAG_MESSAGE_REQUEST_IDENTIFIER = "MessageRequestIdentifier";
    public static final String TAG_CODE_LIST_VERSIONS = "CodeListVersions";
    public static final String TAG_REPLY_CODE_LIST_VERSION = "ReplyCodeListVersion";
    public static final String TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION = "MessageDigestAlgorithmCodeListVersion";
    public static final String TAG_FILE_FORMAT_CODE_LIST_VERSION = "FileFormatCodeListVersion";
    public static final String TAG_ARCHIVE_TRANSFER = "ArchiveTransfer";
    public static final String TAG_GRANT_DATE = "GrantDate";

    public static final String TAG_ORIGINATINGAGENCYIDENTIFIER = "OriginatingAgencyIdentifier";
    public static final String TAG_SUBMISSIONAGENCYIDENTIFIER = "SubmissionAgencyIdentifier";

    
    public static final String TAG_OPERATION = "Operation";    
    public static final String TAG_EVENT = "Event";
    public static final String TAG_EVENT_TYPE = "EventType";
    public static final String TAG_EVENT_TYPE_CODE = "EventTypeCode";
    public static final String TAG_EVENT_DATE_TIME = "EventDateTime";
    public static final String TAG_EVENT_OUTCOME = "Outcome";
    public static final String TAG_EVENT_OUTCOME_DETAIL = "OutcomeDetail";
    public static final String TAG_EVENT_OUTCOME_DETAIL_MESSAGE = "OutcomeDetailMessage";
    public static final String TAG_EVENT_OUTCOME_DETAIL_MESSAGE_CODE = "OutcomeDetailMessageCode";
    public static final String ATTRIBUTE_ID = "id";
    public static final String TAG_ARCHIVE_UNIT = "ArchiveUnit";
    public static final String TAG_DATA_OBJECT_GROUP = "DataObjectGroup";
    public static final String TAG_BINARY_DATA_OBJECT_ID = "BinaryDataObjectID";
    public static final String TAG_BINARY_DATA_OBJECT_SYSTEM_ID = "BinaryDataObjectSystemId";    
    
    public static final String NAMESPACE_XLINK = "xlink";
    public static final String NAMESPACE_PR = "pr";
    public static final String NAMESPACE_XSI = "xsi";
    public static final String ATTRIBUTE_SCHEMA_LOCATION = "schemaLocation";    
    
    private SedaConstants() {
        // Empty constructor
    }

}
