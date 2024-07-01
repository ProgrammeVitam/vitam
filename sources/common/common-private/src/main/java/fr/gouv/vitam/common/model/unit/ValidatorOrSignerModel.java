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
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static fr.gouv.vitam.common.utils.SupportedSedaVersions.UNIFIED_NAMESPACE;

/**
 * Class representing a Signer or a Validator
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    namespace = UNIFIED_NAMESPACE,
    propOrder = {
        "firstName",
        "birthName",
        "fullName",
        "givenName",
        "gender",
        "birthDate",
        "birthPlace",
        "deathDate",
        "deathPlace",
        "nationalities",
        "corpname",
        "identifiers",
        "signingTime",
        "validationTime",
        "function",
        "activity",
        "position",
        "role",
        "mandate",
    }
)
public class ValidatorOrSignerModel extends PersonOrEntityAndBusinessType {

    @JsonProperty("SigningTime")
    @XmlElement(name = "SigningTime", namespace = UNIFIED_NAMESPACE)
    private String signingTime;

    @JsonProperty("ValidationTime")
    @XmlElement(name = "ValidationTime", namespace = UNIFIED_NAMESPACE)
    private String validationTime;

    /**
     * When updated : not forget to update XmlType.propOrder annotation to fix properties order
     * This avoid duplicating properties declaration between this class and his superclass AgentTypeModel
     */
    public ValidatorOrSignerModel() {}

    public String getSigningTime() {
        return signingTime;
    }

    public void setSigningTime(String signingTime) {
        this.signingTime = signingTime;
    }

    public String getValidationTime() {
        return validationTime;
    }

    public void setValidationTime(String validationTime) {
        this.validationTime = validationTime;
    }
}
