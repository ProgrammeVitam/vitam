/**
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
 */
 
angular.module('archive.unit')
    .controller('ArchiveUnitController', function($http, $routeParams){

        var self = this;
        self.archiveId = $routeParams.archiveId;
        self.archiveArray=[];
        self.archiveFiles={
          1:"archiveUnit",
          2:"miniArchiveUnit",
          3:"archiveUnit3"
        };

        var buildSingleField = function buildSingleField (value, key) {
            var fieldSet = {};
            fieldSet.fieldName = key;
            fieldSet.fieldValue= value;
            fieldSet.isChild = false;

            if(!angular.isObject(value)){
                fieldSet.typeF = "S";
            }else {
                // Composite value
                fieldSet.typeF = "P";
                var contentField = value;
                fieldSet.content = [];
                fieldSet.isChild = true;

                var keyArrayIndex = 1;
                angular.forEach(contentField, function(value, key) {
                    if(key !== "_mgt" && key !== "_id" && key.toString().charAt(0)!=="_"){
                        var fieldSetSecond = buildSingleField(value, key);
                        fieldSetSecond.isChild = true;

                        if(angular.isArray(contentField)){
                            fieldSetSecond.fieldName = "Valeur " + keyArrayIndex;
                            keyArrayIndex = keyArrayIndex + 1;
                        }

                        fieldSet.content.push(fieldSetSecond);
                    }
                });
            }
            return fieldSet;
        };

        var archiveFile ="archives/" + self.archiveFiles[$routeParams.archiveId] + ".json";
        var prom = $http.get(archiveFile);
        prom.error(function() {
            alert("Fichier introuvable ou invalide.")
        })

        prom.success(function (data) {
            self.archiveFields = data;

            var idField = self.archiveFields["_id"];
            if(!angular.isObject(idField)){
                var fieldSet = {};
                fieldSet.fieldName = "ID";
                fieldSet.fieldValue= idField;
                fieldSet.isChild = false;
                fieldSet.typeF= "S";
                self.archiveArray.push(fieldSet);
            }

            angular.forEach(self.archiveFields, function(value, key) {
                if(key!=="_mgt" && key!=="_id" && key.toString().charAt(0)!=="_") {
                    self.fieldSet = buildSingleField(value, key);
                    self.archiveArray.push(self.fieldSet);
                }
            });

            var mgtField = self.archiveFields["_mgt"];
            if(angular.isObject(mgtField)){
                var fieldSet = {};
                fieldSet.fieldName = "Management";
                fieldSet.fieldValue= mgtField;
                fieldSet.isChild = false;
                fieldSet.typeF= "P";

                var contentField = mgtField;
                fieldSet.content = [];

                angular.forEach(contentField, function(value, key) {
                    if(key !== "_mgt" && key !== "_id" && key.toString().charAt(0)!=="_"){
                        var fieldSetSecond = buildSingleField(value, key);
                        fieldSet.content.push(fieldSetSecond);
                    }
                });
                self.archiveArray.push(fieldSet);
            }
        })
    });
