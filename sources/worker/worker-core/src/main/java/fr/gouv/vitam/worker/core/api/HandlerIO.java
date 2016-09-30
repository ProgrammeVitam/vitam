/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.worker.core.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler input and output parameter
 */
public class HandlerIO {
    public static final String NOT_ENOUGH_PARAM = "Input/Output io parameter list is not enough";
    public static final String NOT_CONFORM_PARAM = "Input/Output io parameter is not correct";
    private List<Object> input;
    private List<Object> output;
    private String localPathRoot;
    

    /** Constructor with local root path
     * @param localPath
     */
    public HandlerIO(String localPath) {
        input =  new ArrayList<>();
        output =  new ArrayList<>();
        localPathRoot = localPath;
    }
    
    /**
     * @return list of input
     */
    public List<Object> getInput() {
        return input;
    }
    
    /**
     * @param object
     * @return HandlerIO
     */
    public HandlerIO addInput(Object object) {
        this.input.add(object);
        return this;
    }
    
    /**
     * @return list of output
     */
    public List<Object> getOutput() {
        return output;
    }
    
    /**
     * @param object
     * @return HandlerIO
     */
    public HandlerIO addOutput(Object object) {
        this.output.add(object);
        return this;
    }
    
    /**
     * @param source HandlerIO with param
     * @param destination HandlerIO with class element
     * @return
     */
    public static boolean checkHandlerIO(HandlerIO source, HandlerIO destination ) {
        if (source.getInput().size() != destination.getInput().size() || 
            source.getOutput().size() != destination.getOutput().size()) {
            return false;
        }
        for(int i = 0; i < source.getOutput().size(); i++) {
            if (!source.getOutput().get(i).getClass().equals(destination.getOutput().get(i))) {
                return false;
            }
        }
        for(int i = 0; i < source.getInput().size(); i++) {
            if (!source.getInput().get(i).getClass().equals(destination.getInput().get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the localPathRoot
     */
    public String getLocalPathRoot() {
        return localPathRoot;
    }

    /**
     * @param localPathRoot the localPathRoot to set
     *
     * @return this HandlerIO
     */
    public HandlerIO setLocalPathRoot(String localPathRoot) {
        this.localPathRoot = localPathRoot;
        return this;
    }
    
}
