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
  .constant('ARCHIVE_UNIT_MODULE_CONST', {
    'CONFIG_FILE_NOT_FOUND_MSG' : 'Fichier de configuration des détails à afficher est introuvable ou invalide.',
    'ARCHIVE_UNIT_FORM_PREFIX' : 'Archive Unit : ',
    'ARCHIVE_UNIT_FORM_TITLE_SEPARATOR' : '/',
    'SIMPLE_FIELD_TYPE': 'S',
    'COMPLEX_FIELD_TYPE': 'P',
    'TITLE_FIELD': 'Title',
    'ID_KEY': '_id',
    'MGT_KEY': '_mgt',
    'TECH_KEY': '_',
    'ID_LABEL': 'ID',
    'MGT_LABEL': 'Management',
    'LIST_ITEM_LABEL': 'Valeur '
  })
  .controller('ArchiveUnitController', function($http, $routeParams, ihmDemoFactory, $window, ARCHIVE_UNIT_MODULE_CONST,
                archiveDetailsService){

    var self = this;

    // *************** Set Edit mode ********************** //
    self.isEditMode = false;
    self.switchToEditMode = function switchToEditMode(){
      console.log(self.isEditMode);
      self.isEditMode = !self.isEditMode;
    };
    // **************************************************** //

    // 2- Details diplaying process
    self.archiveId = $routeParams.archiveId;
    self.archiveTitle = '';
    self.archiveArray=[];

    // Function buildSingleField: build single field structure
    var buildSingleField = function buildSingleField (value, key) {
      // 1- Check if the current field is configured to be diplayed
      var isFieldWithLabel = self.archiveDetailsConfig != null && self.archiveDetailsConfig[key] !== null && self.archiveDetailsConfig[key] !== undefined;
      var fieldNameLabel = key;
      if(isFieldWithLabel){
        fieldNameLabel = self.archiveDetailsConfig[key];
      }

      var fieldSet = {};
      fieldSet.fieldName = fieldNameLabel;
      fieldSet.fieldValue= value;
      fieldSet.isChild = false;

      if(!angular.isObject(value)) {
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
      } else {
        // Composite value
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
        var contentField = value;
        fieldSet.content = [];
        fieldSet.isChild = true;

        var keyArrayIndex = 1;
        angular.forEach(contentField, function(value, key) {
          if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
            key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY){
            var fieldSetSecond = buildSingleField(value, key);
            fieldSetSecond.isChild = true;

            if(angular.isArray(contentField)){
              fieldSetSecond.fieldName = ARCHIVE_UNIT_MODULE_CONST.LIST_ITEM_LABEL + keyArrayIndex;
              keyArrayIndex = keyArrayIndex + 1;
            }

            fieldSet.content.push(fieldSetSecond);
          }
        });
      }
      return fieldSet;
    };


    // ************ Diplay Archive Unit Form dynamically ************* /

    // Get required data
    self.archiveFields = $window.data;
    self.archiveDetailsConfig = $window.dataConfig;

    var idField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.ID_KEY];
    if(!angular.isObject(idField)){
        var fieldSet = {};
        fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.ID_LABEL;
        fieldSet.fieldValue= idField;
        fieldSet.isChild = false;
        fieldSet.typeF= ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
        self.archiveArray.push(fieldSet);
    }

    angular.forEach(self.archiveFields, function(value, key) {
        if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
          key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) {
              // Get Title archive
              if(key == ARCHIVE_UNIT_MODULE_CONST.TITLE_FIELD){
                self.archiveTitle = value;
                $window.document.title = ARCHIVE_UNIT_MODULE_CONST.ARCHIVE_UNIT_FORM_PREFIX +
                        $routeParams.archiveId + ARCHIVE_UNIT_MODULE_CONST.ARCHIVE_UNIT_FORM_TITLE_SEPARATOR + self.archiveTitle;
              }
              self.fieldSet = buildSingleField(value, key);
              self.archiveArray.push(self.fieldSet);
        }
    });

    var mgtField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
    if(angular.isObject(mgtField)){
        var fieldSet = {};
        fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.MGT_LABEL;
        fieldSet.fieldValue= mgtField;
        fieldSet.isChild = false;
        fieldSet.typeF= ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;

        var contentField = mgtField;
        fieldSet.content = [];

        angular.forEach(contentField, function(value, key) {
            if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
              key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY){
                var fieldSetSecond = buildSingleField(value, key);
                fieldSet.content.push(fieldSetSecond);
            }
        });
        self.archiveArray.push(fieldSet);
    }


    //************* The following array is used only for mocking response *********** //
    // self.archiveFiles={
    //   1:"archiveUnit",
    //   2:"miniArchiveUnit",
    //   3:"archiveUnit3"
    // };
    // ****************************************************************************** //

  });
