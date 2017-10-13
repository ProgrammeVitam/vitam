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
package fr.gouv.vitam.common.mapping.dip;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import fr.gouv.culture.archivesdefrance.seda.v2.AgentType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
import fr.gouv.vitam.common.model.unit.AgentTypeModel;

/**
 * Convert AgentTypeModel to Jaxb AgentType
 */
public class AgentTypeMapper {

    /**
     * Convert AgentTypeModel to jaxb type AgentType
     *
     * @param agentTypeModel
     * @return
     * @throws DatatypeConfigurationException
     */
    public AgentType convert(AgentTypeModel agentTypeModel) throws DatatypeConfigurationException {

        if (agentTypeModel == null) {
            return null;
        }

        ObjectFactory objectFactory = new ObjectFactory();
        LocationGroupMapper locationGroupConverter = new LocationGroupMapper();
        final AgentType agentType = new AgentType();
        List<JAXBElement<?>> content = agentType.getContent();

        if (null != agentTypeModel.getFirstName()) {
            content.add(objectFactory.createAgentTypeFirstName(agentTypeModel.getFirstName()));
        }

        if (null != agentTypeModel.getBirthName()) {
            content.add(objectFactory.createAgentTypeBirthName(agentTypeModel.getBirthName()));
        }

        if (null != agentTypeModel.getCorpname()) {
            content.add(objectFactory.createAgentTypeCorpname(agentTypeModel.getCorpname()));
        }

        if (null != agentTypeModel.getGivenName()) {
            content.add(objectFactory.createAgentTypeGivenName(agentTypeModel.getGivenName()));
        }

        if (null != agentTypeModel.getGender()) {
            content.add(objectFactory.createAgentTypeGender(agentTypeModel.getGender()));
        }

        agentTypeModel.getNationalities()
            .forEach(item -> content.add(objectFactory.createAgentTypeNationality(item)));

        agentTypeModel.getIdentifiers()
            .forEach(item -> content.add(objectFactory.createAgentTypeIdentifier(item)));

        if (null != agentTypeModel.getBirthDate()) {
            content
                .add(objectFactory
                    .createAgentTypeBirthDate(XMLGregorianCalendarImpl.parse(agentTypeModel.getBirthDate())));
        }

        if (null != agentTypeModel.getDeathDate()) {
            content
                .add(objectFactory
                    .createAgentTypeDeathDate(XMLGregorianCalendarImpl.parse(agentTypeModel.getDeathDate())));
        }

        if (null != agentTypeModel.getBrithPlace()) {
            content.add(objectFactory
                .createAgentTypeBirthPlace(locationGroupConverter.mapToBirthPlace(agentTypeModel.getBrithPlace())));
        }

        if (null != agentTypeModel.getDeathPlace()) {
            content.add(objectFactory
                .createAgentTypeDeathPlace(locationGroupConverter.mapToDeathPlace(agentTypeModel.getDeathPlace())));
        }

        return agentType;

    }
}
