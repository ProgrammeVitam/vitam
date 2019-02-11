package fr.gouv.vitam.ihmdemo.core;

import javax.ws.rs.core.MediaType;

public enum AllowedMediaTypeToVisualize {

    PDF(new MediaType("application", "pdf")),
    TIFF(new MediaType("image", "tiff")),
    PLAIN_TEXT(new MediaType("text", "plain")),
    ;

    private final MediaType mediaType;

    AllowedMediaTypeToVisualize(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public static final boolean isAllowedMediaTypeToVsualize(MediaType mediaType) {
        for(AllowedMediaTypeToVisualize amt : AllowedMediaTypeToVisualize.values()) {
            if(amt.mediaType.equals(mediaType)) {
                 return true;
            }
        }
        return false;
    }
}
