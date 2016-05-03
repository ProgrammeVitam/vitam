/**
 * This file is part of Vitam Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common.exception;

/**
 * Invalid UUID
 *
 * 
 *
 */
public class InvalidUuidOperationException extends Exception {

    private static final long serialVersionUID = 1191923223282003583L;

    /**
     *
     */
    public InvalidUuidOperationException() {
    }

    /**
     * @param arg0
     */
    public InvalidUuidOperationException(final String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public InvalidUuidOperationException(final Throwable arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public InvalidUuidOperationException(final String arg0, final Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @param arg3
     */
    public InvalidUuidOperationException(final String arg0, final Throwable arg1,
            final boolean arg2, final boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

}
