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
package fr.gouv.vitam.common.model.unit;

import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class representing a Signer or a Validator
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = NAMESPACE_URI)
public class ValidatorOrSignerModel {

    @JsonProperty("BirthPlace")
    @XmlElement(name = "BirthPlace", namespace = NAMESPACE_URI)
    private LocationGroupModel birthPlace;

    @JsonProperty("BirthDate")
    @XmlElement(name = "BirthDate", namespace = NAMESPACE_URI)
    private String birthDate;

    @JsonProperty("DeathPlace")
    @XmlElement(name = "DeathPlace", namespace = NAMESPACE_URI)
    private LocationGroupModel deathPlace;

    @JsonProperty("GivenName")
    @XmlElement(name = "GivenName", namespace = NAMESPACE_URI)
    private String givenName;

    @JsonProperty("BirthName")
    @XmlElement(name = "BirthName", namespace = NAMESPACE_URI)
    private String birthName;

    @JsonProperty("FirstName")
    @XmlElement(name = "FirstName", namespace = NAMESPACE_URI)
    private String firstName;

    @JsonProperty("Gender")
    @XmlElement(name = "Gender", namespace = NAMESPACE_URI)
    private String gender;

    @JsonProperty("Corpname")
    @XmlElement(name = "Corpname", namespace = NAMESPACE_URI)
    private String corpname;

    @JsonProperty("Nationality")
    @XmlElement(name = "Nationality", namespace = NAMESPACE_URI)
    private List<String> nationalities;

    @JsonProperty("DeathDate")
    @XmlElement(name = "DeathDate", namespace = NAMESPACE_URI)
    private String deathDate;

    @JsonProperty("Identifier")
    @XmlElement(name = "Identifier", namespace = NAMESPACE_URI)
    private List<String> identifiers;

    @JsonProperty("Function")
    @XmlElement(name = "Function", namespace = NAMESPACE_URI)
    private String function;

    @JsonProperty("Activity")
    @XmlElement(name = "Activity", namespace = NAMESPACE_URI)
    private String activity;

    @JsonProperty("Position")
    @XmlElement(name = "Position", namespace = NAMESPACE_URI)
    private String position;

    @JsonProperty("Role")
    @XmlElement(name = "Role", namespace = NAMESPACE_URI)
    private String role;

    @JsonProperty("SigningTime")
    @XmlElement(name = "SigningTime", namespace = NAMESPACE_URI)
    private String signingTime;

    @JsonProperty("ValidationTime")
    @XmlElement(name = "ValidationTime", namespace = NAMESPACE_URI)
    private String validationTime;

    public ValidatorOrSignerModel() {
    }

    public LocationGroupModel getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(LocationGroupModel birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public LocationGroupModel getDeathPlace() {
        return deathPlace;
    }

    public void setDeathPlace(LocationGroupModel deathPlace) {
        this.deathPlace = deathPlace;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getBirthName() {
        return birthName;
    }

    public void setBirthName(String birthName) {
        this.birthName = birthName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCorpname() {
        return corpname;
    }

    public void setCorpname(String corpname) {
        this.corpname = corpname;
    }

    public List<String> getNationalities() {
        if (nationalities == null) {
            nationalities = new ArrayList<>();
        }
        return nationalities;
    }

    public void setNationalities(List<String> nationalities) {
        this.nationalities = nationalities;
    }

    public String getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(String deathDate) {
        this.deathDate = deathDate;
    }

    public List<String> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new ArrayList<>();
        }
        return identifiers;
    }

    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

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
