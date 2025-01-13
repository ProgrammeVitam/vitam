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

package fr.gouv.vitam.collect.internal.core.csv;

import java.util.Set;
import java.util.regex.Pattern;

public class CsvMetadataUtils {

    public static final int MAX_HEADER_NAME_LENGTH = 255;
    public static final char SEPARATOR_CHAR = '.';
    public static final String SEPARATOR = ".";
    public static final String HASH_PREFIX = "#";
    public static final String CONTENT = "Content";
    public static final String CONTENT_SEPARATOR = CONTENT + SEPARATOR;
    public static final String MANAGEMENT = "Management";
    public static final String MANAGEMENT_SEPARATOR = MANAGEMENT + SEPARATOR;
    public static final String MANAGEMENT_FIELD = "#management";
    public static final Pattern STARTS_WITH_DIGIT_PATTERN = Pattern.compile("^[0-9].*$");
    public static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("^(0|[1-9][0-9]*)$");
    public static final String CONTENT_TITLE = "Content.Title";
    public static final Pattern CONTENT_TITLE_VALID_HEADER_NAME_PATTERN = Pattern.compile(
        "^Content\\.Title(\\.(0|[1-9][0-9]*))?(\\.attr)?$"
    );
    public static final String CONTENT_DESCRIPTION = "Content.Description";
    public static final Pattern CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN = Pattern.compile(
        "^Content\\.Description(\\.(0|[1-9][0-9]*))?(\\.attr)?$"
    );

    public static final Set<String> SEDA_EXTENSION_POINTS = Set.of(
        "Content",
        "Content.SigningInformation.Extended",
        "Content.OriginatingAgency.OrganizationDescriptiveMetadata",
        "Content.SubmissionAgency.OrganizationDescriptiveMetadata"
    );

    public static final String CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST =
        "Content.Signature.ReferencedObject.SignedObjectDigest";
    public static final String CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST =
        "Content.Signature.ReferencedObject.SignedObjectDigest.MessageDigest";
    public static final String CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR =
        "Content.Signature.ReferencedObject.SignedObjectDigest.attr";
    public static final String CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM =
        "Content.Signature.ReferencedObject.SignedObjectDigest.Algorithm";
    public static final Pattern CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN = Pattern.compile(
        "Content.Signature(\\.(0|[1-9][0-9]*))?.ReferencedObject.SignedObjectDigest.attr"
    );

    public static final String END_DATE_FIELD = "EndDate";

    public static final Set<String> SEDA_MANAGEMENT_SPECIAL_ARRAY_FIELDS = Set.of(
        "Management.AccessRule.Rule",
        "Management.AccessRule.StartDate",
        "Management.AppraisalRule.Rule",
        "Management.AppraisalRule.StartDate",
        "Management.ClassificationRule.Rule",
        "Management.ClassificationRule.StartDate",
        "Management.DisseminationRule.Rule",
        "Management.DisseminationRule.StartDate",
        "Management.HoldRule.Rule",
        "Management.HoldRule.StartDate",
        "Management.HoldRule.HoldEndDate",
        "Management.HoldRule.HoldOwner",
        "Management.HoldRule.HoldReassessingDate",
        "Management.HoldRule.HoldReason",
        "Management.HoldRule.PreventRearrangement",
        "Management.ReuseRule.Rule",
        "Management.ReuseRule.StartDate",
        "Management.StorageRule.Rule",
        "Management.StorageRule.StartDate"
    );

    public static final Set<String> SEDA_MANAGEMENT_SPECIAL_RULE_PROPERTY_ARRAY_FIELDS = Set.of(
        "Management.AccessRule.StartDate",
        "Management.AppraisalRule.StartDate",
        "Management.ClassificationRule.StartDate",
        "Management.DisseminationRule.StartDate",
        "Management.HoldRule.StartDate",
        "Management.HoldRule.HoldEndDate",
        "Management.HoldRule.HoldOwner",
        "Management.HoldRule.HoldReassessingDate",
        "Management.HoldRule.HoldReason",
        "Management.HoldRule.PreventRearrangement",
        "Management.ReuseRule.StartDate",
        "Management.StorageRule.StartDate"
    );

    public static final Pattern LANG_ATTR_VALUE_PATTERN = Pattern.compile("^xml:lang=\"(.+)\"$");
    public static final Pattern ALGORITHM_ATTR_VALUE_PATTERN = Pattern.compile("^algorithm=\"(.+)\"$");

    public static final String ATTR_HEADER_NAME = "attr";
    public static final String ATTR_HEADER_NAME_SUFFIX = SEPARATOR + ATTR_HEADER_NAME;
    public static final String FILE_HEADER = "File";

    public static final String API_FIELD_TITLE = "Title";
    public static final String API_FIELD_TITLE_ = "Title_";

    public static final String API_FIELD_DESCRIPTION = "Description";
    public static final String API_FIELD_DESCRIPTION_ = "Description_";

    public static final String RULE_FIELD_NAME = "Rule";
    public static final String RULES_PREFIX = "Rules";
    public static final String RULES_SEPARATOR_PREFIX = RULES_PREFIX + SEPARATOR;

    public static final String IMPLICIT_0_ARRAY_INDEX = "0";

    public static String buildPath(String basePath, String subPath) {
        if (basePath == null && subPath == null) {
            return null;
        }
        if (basePath == null) {
            return subPath;
        }
        if (subPath == null) {
            return basePath;
        }
        return basePath + SEPARATOR + subPath;
    }

    public static boolean isFileField(String headerName) {
        return headerName.equals(FILE_HEADER);
    }

    public static boolean isContentField(String headerName) {
        return headerName.startsWith(CONTENT_SEPARATOR) && headerName.length() > CONTENT_SEPARATOR.length();
    }

    public static boolean isManagementField(String headerName) {
        return headerName.startsWith(MANAGEMENT_SEPARATOR) && headerName.length() > MANAGEMENT_SEPARATOR.length();
    }

    public static boolean isContentTitleField(String headerName) {
        return equalsOrStartsWith(headerName, CONTENT_TITLE);
    }

    public static boolean isContentDescriptionField(String headerName) {
        return equalsOrStartsWith(headerName, CONTENT_DESCRIPTION);
    }

    public static boolean matchesPattern(String value, Pattern pattern) {
        return pattern.matcher(value).matches();
    }

    public static boolean equalsOrStartsWith(String str, String prefix) {
        return str.equals(prefix) || str.startsWith(prefix + SEPARATOR);
    }
}
