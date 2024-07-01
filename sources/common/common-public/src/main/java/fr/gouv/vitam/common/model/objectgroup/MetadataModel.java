/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Object mapping metadataResponse
 */
public class MetadataModel {

    @JsonProperty("Document")
    private Map<String, Object> document;

    @JsonProperty("Text")
    private Map<String, Object> text;

    @JsonProperty("Image")
    private Map<String, Object> image;

    @JsonProperty("Audio")
    private Map<String, Object> audio;

    @JsonProperty("Video")
    private Map<String, Object> video;

    @JsonIgnore
    private Map<String, Object> any = new HashMap<>();

    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> document) {
        this.document = document;
    }

    public Map<String, Object> getText() {
        return text;
    }

    public void setText(Map<String, Object> text) {
        this.text = text;
    }

    public Map<String, Object> getAudio() {
        return audio;
    }

    public void setAudio(Map<String, Object> audio) {
        this.audio = audio;
    }

    public Map<String, Object> getVideo() {
        return video;
    }

    public void setVideo(Map<String, Object> video) {
        this.video = video;
    }

    public Map<String, Object> getImage() {
        return image;
    }

    public void setImage(Map<String, Object> image) {
        this.image = image;
    }

    @JsonAnyGetter
    public Map<String, Object> getAny() {
        return any;
    }

    @JsonAnySetter
    public void setAny(String key, Object value) {
        if (key != null && key.startsWith("#")) {
            return;
        }
        this.any.put(key, value);
    }
}
