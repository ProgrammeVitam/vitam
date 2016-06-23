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
