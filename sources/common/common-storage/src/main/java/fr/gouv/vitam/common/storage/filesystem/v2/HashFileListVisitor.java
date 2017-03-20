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
package fr.gouv.vitam.common.storage.filesystem.v2;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.jclouds.blobstore.domain.PageSet;

import fr.gouv.vitam.common.storage.filesystem.v2.metadata.object.HashStorageMetadata;

/**
 * Visitor that will manage the List operation that will begin after a marker 
 *
 */
public final class HashFileListVisitor extends SimpleFileVisitor<Path>{

    private HashPageSet hashPageSet = new HashPageSet();
    private static final int MAX_RESULTS_PER_ITERABLE=100;
    private static final int MAX_DEPTH=10;
    private long currentFiles = 0;
    List<String> beginMarkerSplitted;
    String beginMarker;
    private boolean rootDirectory=true;
    int level = 0;
    ArrayList<Integer> fatherCompare = new ArrayList<>(MAX_DEPTH);
    
    /**
     * Default constructor 
     */
    public HashFileListVisitor(){
        // Nothing to do in the default constructor
    }
    
    /**
     * Constructor
     * @param beginMarkerSplitted 
     * @param beginMarker
     */
    public HashFileListVisitor(List<String> beginMarkerSplitted, String beginMarker){
        this.beginMarkerSplitted=beginMarkerSplitted;
        this.beginMarker = beginMarker;
    }
    
    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
       if (rootDirectory){
           rootDirectory=false;
           return FileVisitResult.CONTINUE;
       }
       
       if (beginMarkerSplitted != null){
           if (level >= fatherCompare.size()){
               fatherCompare.add(directory.getFileName().toString().compareTo(beginMarkerSplitted.get(level)));
           }else{
               fatherCompare.set(level,directory.getFileName().toString().compareTo(beginMarkerSplitted.get(level)));
           }
           for(Integer compValue: fatherCompare){
               if (compValue > 0){
                   level++;
                   return FileVisitResult.CONTINUE;
               }
           }
           if (level < beginMarkerSplitted.size() && fatherCompare.get(level) < 0){
                   return FileVisitResult.SKIP_SUBTREE;
           }
           level++;
           
       }
       return super.preVisitDirectory(directory, attrs);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (beginMarkerSplitted != null){
            level--;
        }
        return super.postVisitDirectory(dir, exc);
    }
        
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (beginMarker != null && fatherCompare.get(level-1) == 0 && file.getFileName().toString().compareTo(beginMarker) < 0){
            return FileVisitResult.CONTINUE;
        }
        if (currentFiles >= MAX_RESULTS_PER_ITERABLE){
            hashPageSet.setNextMarker(file.getFileName().toString());
            return FileVisitResult.TERMINATE;
        }
        hashPageSet.add(new HashStorageMetadata(file));
        currentFiles++;
        return super.visitFile(file, attrs);
    }
    
    /**
     * Return the constructed PageSet
     * @return PageSet
     */
    public PageSet<HashStorageMetadata> getPageSet(){
        return hashPageSet;
    }

}
