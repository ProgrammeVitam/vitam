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
package fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * InheritedRule
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InheritedRule {
    @JsonProperty("MaxEndDate")
    private LocalDate maxEndDate;
    @JsonProperty("Rules")
    private Map<String, Rule> rules;

    public InheritedRule() {
        this.rules = new HashMap<>();
    }

    public InheritedRule(LocalDate maxEndDate) {
        this();
        this.maxEndDate = maxEndDate;
    }

    public InheritedRule(Rule initRule) {
        this.maxEndDate = initRule.getMaxEndDate();
        this.rules = new HashMap<>();
        this.rules.put(initRule.getId(), initRule);
    }

    public void addRule(Rule rule) {
        if (this.maxEndDate.isBefore(rule.getMaxEndDate())) {
            this.maxEndDate = rule.getMaxEndDate();
        }

        Rule existingRule = rules.get(rule.getId());
        if (existingRule == null) {
            this.rules.put(rule.getId(), rule);
        } else if (existingRule.getMaxEndDate().isBefore(rule.getMaxEndDate())) {
            existingRule.setMaxEndDate(rule.getMaxEndDate());
        }
    }

    public Map<String, Rule> getRules() {
        return rules;
    }

    public void setRules(Map<String, Rule> rules) {
        this.rules = rules;
    }

    public LocalDate getMaxEndDate() {
        return maxEndDate;
    }

    public void setMaxEndDate(LocalDate maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

}
