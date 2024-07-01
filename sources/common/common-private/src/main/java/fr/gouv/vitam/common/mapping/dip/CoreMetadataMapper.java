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
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.AudioTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.CoreMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.DocumentTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.ImageTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.VideoTechnicalMetadataType;
import fr.gouv.vitam.common.model.objectgroup.MetadataModel;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

/**
 * Map the object MetadataModel generated from ObjectGroup data base model
 * To a jaxb object CoreMetadataType
 * This help convert MetadataModel to xml using jaxb
 */
public class CoreMetadataMapper {

    public CoreMetadataType map(MetadataModel metadataModel) {
        if (metadataModel != null) {
            final CoreMetadataType coreMetadataType = new CoreMetadataType();
            final Map<String, Object> audio = metadataModel.getAudio();
            final Map<String, Object> document = metadataModel.getDocument();
            final Map<String, Object> image = metadataModel.getImage();
            final Map<String, Object> text = metadataModel.getText();
            final Map<String, Object> video = metadataModel.getVideo();
            if (text != null) {
                final TextTechnicalMetadataType textTechnicalMetadataType = new TextTechnicalMetadataType();
                textTechnicalMetadataType.getAny().addAll(mapToOpenType(text));
                coreMetadataType.setText(textTechnicalMetadataType);
            }
            if (audio != null) {
                final AudioTechnicalMetadataType audioTechnicalMetadataType = new AudioTechnicalMetadataType();
                audioTechnicalMetadataType.getAny().addAll(mapToOpenType(audio));
                coreMetadataType.setAudio(audioTechnicalMetadataType);
            }
            if (document != null) {
                final DocumentTechnicalMetadataType documentTechnicalMetadataType = new DocumentTechnicalMetadataType();
                documentTechnicalMetadataType.getAny().addAll(mapToOpenType(document));
                coreMetadataType.setDocument(documentTechnicalMetadataType);
            }
            if (video != null) {
                final VideoTechnicalMetadataType videoTechnicalMetadataType = new VideoTechnicalMetadataType();
                List<Element> e = mapToOpenType(video);
                videoTechnicalMetadataType.getAny().addAll(e);
                coreMetadataType.setVideo(videoTechnicalMetadataType);
            }
            if (image != null) {
                final ImageTechnicalMetadataType imageTechnicalMetadataType = new ImageTechnicalMetadataType();
                imageTechnicalMetadataType.getAny().addAll(mapToOpenType(image));
                coreMetadataType.setImage(imageTechnicalMetadataType);
            }
            return coreMetadataType;
        } else {
            return null;
        }
    }

    private List<Element> mapToOpenType(Map<String, Object> object) {
        return TransformJsonTreeToListOfXmlElement.mapJsonToElement(object);
    }
}
