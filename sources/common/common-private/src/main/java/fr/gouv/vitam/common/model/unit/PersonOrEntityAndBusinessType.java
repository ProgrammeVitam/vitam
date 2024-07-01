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
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

import static fr.gouv.vitam.common.utils.SupportedSedaVersions.UNIFIED_NAMESPACE;

/**
 * Model for PersonOrEntityAndBusiness defined in seda.xsd
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlTransient
public class PersonOrEntityAndBusinessType {

    private static final String FIRST_NAME = "FirstName";
    private static final String BIRTH_NAME = "BirthName";
    private static final String FULLNAME = "FullName";
    private static final String GIVEN_NAME = "GivenName";
    private static final String TAG_GENDER = "Gender";
    private static final String BIRTH_DATE = "BirthDate";
    private static final String BIRTH_PLACE = "BirthPlace";
    private static final String DEATH_DATE = "DeathDate";
    private static final String DEATH_PLACE = "DeathPlace";
    private static final String NATIONALITY = "Nationality";
    private static final String TAG_CORPNAME = "Corpname";
    private static final String IDENTIFIER = "Identifier";
    private static final String FUNCTION = "Function";
    private static final String ACTIVITY = "Activity";
    private static final String POSITION = "Position";
    private static final String ROLE = "Role";
    private static final String MANDATE = "Mandate";

    @JsonProperty(FIRST_NAME)
    @XmlElement(name = FIRST_NAME, namespace = UNIFIED_NAMESPACE)
    private String firstName;

    @JsonProperty(BIRTH_NAME)
    @XmlElement(name = BIRTH_NAME, namespace = UNIFIED_NAMESPACE)
    private String birthName;

    @JsonProperty(FULLNAME)
    @XmlElement(name = FULLNAME, namespace = UNIFIED_NAMESPACE)
    private String fullName;

    @JsonProperty(GIVEN_NAME)
    @XmlElement(name = GIVEN_NAME, namespace = UNIFIED_NAMESPACE)
    private String givenName;

    @JsonProperty(TAG_GENDER)
    @XmlElement(name = TAG_GENDER, namespace = UNIFIED_NAMESPACE)
    private String gender;

    @JsonProperty(BIRTH_DATE)
    @XmlElement(name = BIRTH_DATE, namespace = UNIFIED_NAMESPACE)
    private String birthDate;

    @JsonProperty(BIRTH_PLACE)
    @XmlElement(name = BIRTH_PLACE, namespace = UNIFIED_NAMESPACE)
    private LocationGroupModel birthPlace;

    @JsonProperty(DEATH_DATE)
    @XmlElement(name = DEATH_DATE, namespace = UNIFIED_NAMESPACE)
    private String deathDate;

    @JsonProperty(DEATH_PLACE)
    @XmlElement(name = DEATH_PLACE, namespace = UNIFIED_NAMESPACE)
    private LocationGroupModel deathPlace;

    @JsonProperty(NATIONALITY)
    @XmlElement(name = NATIONALITY, namespace = UNIFIED_NAMESPACE)
    private List<String> nationalities;

    @JsonProperty(TAG_CORPNAME)
    @XmlElement(name = TAG_CORPNAME, namespace = UNIFIED_NAMESPACE)
    private String corpname;

    @JsonProperty(IDENTIFIER)
    @XmlElement(name = IDENTIFIER, namespace = UNIFIED_NAMESPACE)
    private List<String> identifiers;

    //BusinessGroup element's properties
    @JsonProperty(FUNCTION)
    @XmlElement(name = FUNCTION, namespace = UNIFIED_NAMESPACE)
    private List<String> function;

    @JsonProperty(ACTIVITY)
    @XmlElement(name = ACTIVITY, namespace = UNIFIED_NAMESPACE)
    private List<String> activity;

    @JsonProperty(POSITION)
    @XmlElement(name = POSITION, namespace = UNIFIED_NAMESPACE)
    private List<String> position;

    @JsonProperty(ROLE)
    @XmlElement(name = ROLE, namespace = UNIFIED_NAMESPACE)
    private List<String> role;

    @JsonProperty(MANDATE)
    @XmlElement(name = MANDATE, namespace = UNIFIED_NAMESPACE)
    private List<String> mandate;

    public PersonOrEntityAndBusinessType() {}

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

    /**
     * getter for fullName
     *
     * @return fullName value
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * set fullName
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCorpname() {
        return corpname;
    }

    public void setCorpname(String corpname) {
        this.corpname = corpname;
    }

    public List<String> getNationalities() {
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
        return identifiers;
    }

    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * getter for function
     *
     * @return function value
     */
    public List<String> getFunction() {
        return function;
    }

    /**
     * set function
     */
    public void setFunction(List<String> function) {
        this.function = function;
    }

    /**
     * getter for activity
     *
     * @return activity value
     */
    public List<String> getActivity() {
        return activity;
    }

    /**
     * set activity
     */
    public void setActivity(List<String> activity) {
        this.activity = activity;
    }

    /**
     * getter for position
     *
     * @return position value
     */
    public List<String> getPosition() {
        return position;
    }

    /**
     * set position
     */
    public void setPosition(List<String> position) {
        this.position = position;
    }

    /**
     * getter for role
     *
     * @return role value
     */
    public List<String> getRole() {
        return role;
    }

    /**
     * set role
     */
    public void setRole(List<String> role) {
        this.role = role;
    }

    /**
     * getter for mandate
     *
     * @return mandate value
     */
    public List<String> getMandate() {
        return mandate;
    }

    /**
     * set mandate
     */
    public void setMandate(List<String> mandate) {
        this.mandate = mandate;
    }
}
