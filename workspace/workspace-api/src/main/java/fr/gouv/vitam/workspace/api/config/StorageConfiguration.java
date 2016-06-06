package fr.gouv.vitam.workspace.api.config;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment to describe the use of the class

public class StorageConfiguration {
    // TODO REVIEW Why such a configuration? Should the client be able to change it ? If this is for Filesystem
    // implementation, this is not valid for others, right ? This should be in the rest or core package probably or even
    // a filesystem implementation package.

    private String storagePath;

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

}
