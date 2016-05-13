package fr.gouv.vitam.workspace.common;

public enum ErrorMessage {

    CONTAINER_ALREADY_EXIST("Container already exist "), 
    CONTAINER_NOT_FOUND("Container not found "),

    FOLDER_ALREADY_EXIST("Folder already exist "),
    FOLDER_NOT_FOUND("Folder not found "),

    OBJECT_ALREADY_EXIST("Object already exist "),
    OBJECT_NOT_FOUND("Object not found "),

    CONTAINER_NAME_IS_A_MANDATORY_PARAMETER("Container name is a mandatory parameter"), 
    CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER("Container name and Folder name are a mandatory parameter"), 
    CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER("Container name and Object name are a mandatory parameter"),
    
   INTERNAL_SERVER_ERROR("Internal Server Error");

    private final String message;

    private ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}