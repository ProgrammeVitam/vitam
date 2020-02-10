/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.model;

import fr.gouv.vitam.common.CharsetUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Constants class for Vitam internal features
 */
public class VitamConstants {

    public static final String SIP_FOLDER = "SIP";

    public static final String CONTENT_SIP_FOLDER = SIP_FOLDER + "/Content";

    public static final String SEDA_CURRENT_VERSION = "2.1";

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
     * tag of EveryOriginatingAgency
     */
    public static final String EVERY_ORIGINATING_AGENCY = "EveryOriginatingAgency";

    public static final String MANIFEST_FILE_NAME_REGEX = "^([a-zA-Z0-9_\\-]{0,56}[_-]{1}){0,1}(manifest.xml)\\b";

    private static List<String> ruleTypes = null;

    public final static String URL_ENCODED_SEPARATOR;

    static {
        try {
            URL_ENCODED_SEPARATOR = URLEncoder.encode("/", CharsetUtils.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private VitamConstants() {
        // Nothing
    }

    /**
     * @return supported Rules type
     */
    public static List<String> getSupportedRules() {
        if (ruleTypes == null) {
            ruleTypes = new ArrayList<>();
            ruleTypes.add(TAG_RULE_ACCESS);
            ruleTypes.add(TAG_RULE_REUSE);
            ruleTypes.add(TAG_RULE_STORAGE);
            ruleTypes.add(TAG_RULE_APPRAISAL);
            ruleTypes.add(TAG_RULE_CLASSIFICATION);
            ruleTypes.add(TAG_RULE_DISSEMINATION);
        }
        return ruleTypes;
    }

    public enum AppraisalRuleFinalAction {
        KEEP("Keep"),
        DESTROY("Destroy");

        private final String value;

        AppraisalRuleFinalAction(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static AppraisalRuleFinalAction fromValue(String v) throws IllegalArgumentException {
            for (AppraisalRuleFinalAction c : AppraisalRuleFinalAction.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }


    public enum StorageRuleFinalAction {
        RESTRICT_ACCESS("RestrictAccess"),
        TRANSFER("Transfer"),
        COPY("Copy");

        private final String value;

        StorageRuleFinalAction(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static StorageRuleFinalAction fromValue(String v) throws IllegalArgumentException {
            for (StorageRuleFinalAction c : StorageRuleFinalAction.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }
}
