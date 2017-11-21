package fr.gouv.vitam.common.storage.cas.container.api;

/**
 * Enum with all possible Storage Type
 */
public enum StorageType {

    /**
     * A container
     */
    CONTAINER,
    /**
     * An object in the object store
     */
    BLOB,
    /**
     * Represents "special" blobs that have content-type set to
     * application/directory.
     */
    FOLDER,
    /**
     * A partial path; used when the delimiter is set and represents all objects
     * that start with the same name up to the delimiter character (e.g. foo-bar
     * and foo-baz, with delimiter set to "-" will be returned as "foo-").
     */
    RELATIVE_PATH;

}
