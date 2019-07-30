package fr.gouv.vitam.worker.common.utils;

public class SedaUtilsException extends  IllegalArgumentException {

    public SedaUtilsException(Exception e) {
        super(e.getMessage(), e);
    }
}
