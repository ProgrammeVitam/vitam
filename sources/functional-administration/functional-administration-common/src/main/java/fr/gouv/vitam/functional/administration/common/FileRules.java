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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.model.administration.RuleMeasurementEnum;
import fr.gouv.vitam.common.model.administration.RuleType;
import org.bson.Document;

/**
 * FileRules create the template of FileRules from VitamDocument
 */
public class FileRules extends VitamDocument<FileRules> {

    private static final long serialVersionUID = 2471943065920459435L;

    /**
     * the rule id
     */
    public static final String RULEID = "RuleId";
    /**
     * the rule type
     */
    public static final String RULETYPE = "RuleType";
    /**
     * the rule value
     */
    public static final String RULEVALUE = "RuleValue";
    /**
     * the rule description
     */
    public static final String RULEDESCRIPTION = "RuleDescription";
    /**
     * the rule duration
     */
    public static final String RULEDURATION = "RuleDuration";
    /**
     * the rule measurement
     */
    public static final String RULEMEASUREMENT = "RuleMeasurement";
    private static final String CREATIONDATE = "CreationDate";
    private static final String UPDATEDATE = "UpdateDate";
    private static final String TENANT = "_tenant";

    /**
     * Constructor
     */

    public FileRules() {}

    /**
     * Constructor
     *
     * @param document in format Document to create FileRules
     */
    public FileRules(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create FileRules
     */
    public FileRules(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create FileRules
     */
    public FileRules(String content) {
        super(content);
    }

    /**
     * @param tenantId the working tenant
     */
    public FileRules(Integer tenantId) {
        // Empty
        append(TENANT, tenantId);
    }

    @Override
    public VitamDocument<FileRules> newInstance(JsonNode content) {
        return new FileRules(content);
    }

    /**
     * setRuleId
     *
     * @param ruleId to set
     * @return FileRules
     */
    public FileRules setRuleId(String ruleId) {
        append(RULEID, ruleId);
        return this;
    }

    /**
     * setRuleType
     *
     * @param ruleType to set
     * @return FileRules
     */
    public FileRules setRuleType(String ruleType) {
        append(RULETYPE, ruleType);
        return this;
    }

    /**
     * setRuleValue
     *
     * @param ruleValue to set
     * @return FileRules
     */
    public FileRules setRuleValue(String ruleValue) {
        append(RULEVALUE, ruleValue);
        return this;
    }

    /**
     * setRuleDescription
     *
     * @param ruleDescription to set
     * @return FileRules
     */

    public FileRules setRuleDescription(String ruleDescription) {
        append(RULEDESCRIPTION, ruleDescription);
        return this;
    }

    /**
     * setRuleDuration
     *
     * @param ruleDuration to set
     * @return FileRules
     */
    public FileRules setRuleDuration(String ruleDuration) {
        append(RULEDURATION, ruleDuration);
        return this;
    }

    /**
     * setRuleMeasurement
     *
     * @param ruleMeasurement to set
     * @return FileRules
     */
    public FileRules setRuleMeasurement(String ruleMeasurement) {
        append(RULEMEASUREMENT, ruleMeasurement);
        return this;
    }

    /**
     * setCreationDate
     *
     * @param creationDate to set
     * @return this
     */
    public FileRules setCreationDate(String creationDate) {
        append(CREATIONDATE, creationDate);
        return this;
    }

    /**
     * setUpdateDate
     *
     * @param updateDate to set
     * @return this
     */

    public FileRules setUpdateDate(String updateDate) {
        append(UPDATEDATE, updateDate);
        return this;
    }

    public String getRuleid() {
        return getString(RULEID);
    }

    public RuleType getRuletype() {
        return RuleType.getEnumFromName(getString(RULETYPE));
    }

    public String getRulevalue() {
        return getString(RULEVALUE);
    }

    public String getRuledescription() {
        return getString(RULEDESCRIPTION);
    }

    public String getRuleduration() {
        return getString(RULEDURATION);
    }

    public RuleMeasurementEnum getRulemeasurement() {
        return RuleMeasurementEnum.getEnumFromType(getString(RULEMEASUREMENT));
    }

    public String getCreationdate() {
        return getString(CREATIONDATE);
    }
}
