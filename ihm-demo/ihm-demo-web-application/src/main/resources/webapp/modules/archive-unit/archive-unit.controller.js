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
    'LIST_ITEM_LABEL': 'Valeur ',
    'MGT_WITH_CSHARP_KEY': '#mgt',
  }).filter('filterSize', function() {
  	return function(bytes, precision) {
		if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
		if (typeof precision === 'undefined') precision = 1;
		var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
			number = Math.floor(Math.log(bytes) / Math.log(1024));
		return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
	}
  })
  .controller('ArchiveUnitController', function($scope, $http, $routeParams, ihmDemoFactory, $window, ARCHIVE_UNIT_MODULE_CONST,
                archiveDetailsService, $mdToast, $mdDialog){

    var self = this;

    //******************************* Alert diplayed ******************************* //
    self.showAlert = function($event, dialogTitle, message) {
      $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
          .clickOutsideToClose(true)
          .title(dialogTitle)
          .textContent(message)
          .ariaLabel('Alert Dialog Demo')
          .ok('OK')
          .targetEvent($event)
      );
    };
    // **************************************************************************** //

    // *************** Set Edit mode ********************** //
    self.isEditMode = false;
    self.switchToEditMode = function switchToEditMode(){
      self.isEditMode = !self.isEditMode;
    };
    // **************************************************** //

    // *************** Cancel changes ********************** //
    self.cancelChanges = function cancelChanges(){
      self.isEditMode = false;
    };
    // **************************************************** //

    // 2- Details diplaying process
    self.archiveId = $routeParams.archiveId;
    self.archiveTitle = '';
    self.archiveArray=[];
    self.archiveTree = [];

    // Get required data
    self.archiveFields = $window.data;
    self.archiveDetailsConfig = $window.dataConfig;

    // Function buildSingleField: build single field structure
    var buildSingleField = function buildSingleField (value, key, parent, parents) {
      // 1- Check if the current field is configured to be diplayed
      var isFieldWithLabel = self.archiveDetailsConfig != null && self.archiveDetailsConfig[key] !== null && self.archiveDetailsConfig[key] !== undefined;
      var fieldNameLabel = key;
      if(isFieldWithLabel){
        fieldNameLabel = self.archiveDetailsConfig[key];
      }

      var fieldSet = {};
      var isMgtChild = false;

      fieldSet.fieldName = fieldNameLabel;
      fieldSet.fieldId = key;
      fieldSet.fieldValue= value;
      fieldSet.currentFieldValue = value;
      fieldSet.isChild = false;

      // parents list
      fieldSet.parents = [];
      if(parent !== key){
        for(i=0; i<parents.length;i++){
            fieldSet.parents.push(parents[i]);
            if(parents[i] == ARCHIVE_UNIT_MODULE_CONST.MGT_KEY || parents[i] == ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY){
              isMgtChild = true;
            }
        }

        fieldSet.parents.push(parent);
        fieldSet.isChild = true;
      }

      if(!angular.isObject(value)) {
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
        if(!isMgtChild){
          fieldSet.isModificationAllowed = true;
        }
      } else {
        // Composite value
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
        var contentField = value;
        fieldSet.content = [];
        // fieldSet.isChild = true;

        var keyArrayIndex = 1;
        angular.forEach(contentField, function(value, key) {
          if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
            key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY){
            var fieldSetSecond = buildSingleField(value, key, fieldSet.fieldId, fieldSet.parents);
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

    //************* Intercept user changes *********** //
    self.modifiedFields = [];
    self.interceptUserChanges = function interceptUserChanges(fieldSet){
      if (fieldSet.fieldValue !== fieldSet.currentFieldValue) {
        fieldSet.isFieldModified = true;
      } else {
        fieldSet.isFieldModified = false;
      }
    };

    //************* Build modified fields *********** //
    var getModifiedFields = function getModifiedFields(fieldSet){
      if (fieldSet.isFieldModified == true) {
        // Add modified field to updateQueryFields
        var modifiedFieldSet = {};
        modifiedFieldSet.fieldId = fieldSet.fieldId;
        modifiedFieldSet.newFieldValue = fieldSet.currentFieldValue;

        // build field reference
        var fieldPath = "";
        for(i=0; i < fieldSet.parents.length; i++){
          // TODO : correct this test
          if(angular.isNumber(fieldSet.parents[i])){
            fieldPath = fieldPath.substring(0, fieldPath.length - 1) + "[" + fieldSet.parents[i] + "].";
          }else{
            fieldPath = fieldPath + fieldSet.parents[i] + ".";
          }
        }

        // TODO : correct this test
        if(angular.isNumber(fieldSet.fieldId)){
          modifiedFieldSet.fieldId = fieldPath.substring(0, fieldPath.length - 1) + "[" + fieldSet.fieldId + "]";
        }else{
          modifiedFieldSet.fieldId = fieldPath + modifiedFieldSet.fieldId;
        }
        self.modifiedFields.push(modifiedFieldSet);
      } else if (fieldSet.content!== null && fieldSet.content !== undefined && fieldSet.content.length !== 0) {
        angular.forEach(fieldSet.content, function(nestedField) {
          getModifiedFields(nestedField);
        });
      }
    };


    //************* Save modifications *********** //
    self.saveModifications = function saveModifications($event) {
      // Prepare modified fields map
      self.modifiedFields = [];
      angular.forEach(self.archiveArray, function(value) {
        getModifiedFields(value);
      });

      // Call REST service
      ihmDemoFactory.saveArchiveUnit(self.archiveId, self.modifiedFields)
      .then(function (response) {
          // SUCCESS
          // Archive unit updated: send new select query to back office
          // Find archive unit details
          var displayUpdatedArchiveCallBack = function (data) {
            if(data.$result == null || data.$result == undefined ||
              data.$hint == null || data.$hint == undefined) {
                console.log("errorMsg");
                self.showAlert($event, "Erreur", "Erreur survenue à la mise à jour de l'archive unit");
            } else {
              // Archive unit found
              self.archiveFields = data.$result[0];
              //get archive object groups informations to be displayed in the table
              ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._og)
    	      .then(function (response) {
    	    	  var dataOG = response.data;	    	  ;
    	    	  if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
    	    			  dataOG.versions == null || dataOG.versions == undefined){
    	    		  // ObjectGroups Not Found
    	    		  console.log("errorMsg");
    	    	  } else {
    	    		  $scope.archiveObjectGroups = dataOG;
    	    		  $scope.archiveObjectGroupsOgId = self.archiveFields._og;
    	    	  }
    	      },function (error) {
    	    	  console.log("errorMsg");
    	      });
              self.archiveArray=[];
              self.displayArchiveDetails();

              // Refresh archive Details
              // Cancel EditMode
              self.isEditMode = false;
              self.showAlert($event, "Info", "Mise à jour réussie de l'archive unit");
            }
          };

          var failureUpdateDisplayCallback = function(errorMsg){
            // Display error message
            console.log(errorMsg);
            self.showAlert($event, "Erreur", "Erreur survenue à la mise à jour de l'archive unit");
          }
          archiveDetailsService.findArchiveUnitDetails(self.archiveId, displayUpdatedArchiveCallBack, failureUpdateDisplayCallback);

      }, function (error) {
        console.log('Update Archive unit failed : ' + error.message);
        self.showAlert($event, "Erreur", "Erreur survenue à la mise à jour de l'archive unit");
      });
    };

    // ************ Diplay Archive Unit Form dynamically ************* /
    self.refreshArchiveDetails = function () {
      var displayUpdatedArchiveCallBack = function (data) {
        if(data.$result == null || data.$result == undefined ||
          data.$hint == null || data.$hint == undefined) {
            console.log("errorMsg");
        } else {
          // Archive unit found
          self.archiveFields = data.$result[0];

          //get archive object groups informations to be displayed in the table
          ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._og)
  	      .then(function (response) {
  	    	  var dataOG = response.data;	    	  ;
  	    	  if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
  	    			  dataOG.versions == null || dataOG.versions == undefined){
  	    		  // ObjectGroups Not Found
  	    		  console.log("errorMsg");
  	    	  } else {
  	    		  $scope.archiveObjectGroups = dataOG;
  	    		  $scope.archiveObjectGroupsOgId = self.archiveFields._og;
  	    	  }
  	      },function (error) {
  	    	  console.log("errorMsg");
  	      });

          // Get Archive Tree
          ihmDemoFactory.getArchiveTree(self.archiveFields._id, self.archiveFields._us)
  	      .then(function (response) {
  	    	  self.archiveTree = response.data;
            console.log("Archive tree: " + self.archiveTree);
  	      },function (error) {
  	    	  console.log("Archive tree search failed");
  	      });


          self.archiveArray=[];
          self.isEditMode = false;
          self.displayArchiveDetails();
        }
      };

      var failureUpdateDisplayCallback = function(errorMsg){
        // Display error message
        console.log(errorMsg);
      }
      archiveDetailsService.findArchiveUnitDetails(self.archiveId, displayUpdatedArchiveCallBack, failureUpdateDisplayCallback);
    };

    self.displayArchiveDetails = function(){
      if(self.archiveFields == null || self.archiveFields == undefined){
        // Refresh screen
        self.refreshArchiveDetails();
      } else {
        // ID Field
        var idField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.ID_KEY];
        if(!angular.isObject(idField)){
            var fieldSet = {};
            fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.ID_LABEL;
            fieldSet.fieldValue= idField;
            fieldSet.isChild = false;
            fieldSet.typeF= ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
            fieldSet.fieldId = ARCHIVE_UNIT_MODULE_CONST.ID_KEY;
            fieldSet.parents = [];
            fieldSet.isModificationAllowed = false;
            self.archiveArray.push(fieldSet);
        }
        //get archive object groups informations to be displayed in the table
        ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._og)
	      .then(function (response) {
	    	  var dataOG = response.data;	    	  ;
	    	  if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
	    			  dataOG.versions == null || dataOG.versions == undefined){
	    		  // ObjectGroups Not Found
	    		  console.log("errorMsg");
	    	  } else {
	    		  $scope.archiveObjectGroups = dataOG;
	    		  $scope.archiveObjectGroupsOgId = self.archiveFields._og;
	    	  }
	      },function (error) {
	    	  console.log("errorMsg");
	      });

        // Get Archive Tree
        ihmDemoFactory.getArchiveTree(self.archiveFields._id, self.archiveFields._us)
        .then(function (response) {
          self.archiveTree = response.data;
          console.log("Archive tree: " + self.archiveTree);
        },function (error) {
          console.log("Archive tree search failed");
        });

        // Other fields
        angular.forEach(self.archiveFields, function(value, key) {
            if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
              key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) {
                  // Get Title archive
                  if(key == ARCHIVE_UNIT_MODULE_CONST.TITLE_FIELD){
                    self.archiveTitle = value;
                    $window.document.title = ARCHIVE_UNIT_MODULE_CONST.ARCHIVE_UNIT_FORM_PREFIX +
                            $routeParams.archiveId + ARCHIVE_UNIT_MODULE_CONST.ARCHIVE_UNIT_FORM_TITLE_SEPARATOR + self.archiveTitle;
                  }
                  var parents = [];
                  self.fieldSet = buildSingleField(value, key, key, parents);
                  self.fieldSet.isModificationAllowed = true;
                  self.archiveArray.push(self.fieldSet);
            }
        });

        // _mgt field
        var mgtField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
        if(angular.isObject(mgtField)){
            var fieldSet = {};
            fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.MGT_LABEL;
            fieldSet.fieldValue= mgtField;
            fieldSet.isChild = false;
            fieldSet.typeF= ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
            fieldSet.fieldId = ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY;
            fieldSet.parents = [];

            var contentField = mgtField;
            fieldSet.content = [];

            angular.forEach(contentField, function(value, key) {
                if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
                  key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY){
                  var fieldSetSecond = buildSingleField(value, key, ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY, fieldSet.parents);
                  fieldSetSecond.isModificationAllowed = false;
                  fieldSet.content.push(fieldSetSecond);
                }
            });
            self.archiveArray.push(fieldSet);
          }
      }
    };

    // Display Details
    self.displayArchiveDetails();

    //******** Toast diplayed only if the archive unit is already opened ********* //
    var last = {
        bottom: false,
        top: true,
        left: false,
        right: true
      };
    self.toastPosition = angular.extend({},last);
    self.getToastPosition = function() {
      sanitizePosition();
      return Object.keys(self.toastPosition)
        .filter(function(pos) { return self.toastPosition[pos]; })
        .join(' ');
    };
    function sanitizePosition() {
      var current = self.toastPosition;
      if ( current.bottom && last.top ) current.top = false;
      if ( current.top && last.bottom ) current.bottom = false;
      if ( current.right && last.left ) current.left = false;
      if ( current.left && last.right ) current.right = false;
      last = angular.extend({},current);
    }
    self.showMessageToast = function(message) {
      var pinTo = self.getToastPosition();
      $mdToast.show(
        $mdToast.simple()
          .textContent(message)
          .position(pinTo )
          .hideDelay(3000)
      );
    };
    // **************************************************************************** //

    // ************************* Download object file ********************************** //
    $scope.download = function($event, objGId, usage, version, fileName) {
        var options = {};
        options.usage = usage;
        options.version = version;
        ihmDemoFactory.getObjectAsInputStream(objGId, options)
	      .then(function (response) {
	    	  var a = document.createElement("a");
	    	  document.body.appendChild(a);
	    	  var url = URL.createObjectURL(new Blob([response.data], { type: 'application/octet-stream' }));
	    	  a.href = url;
	          a.download = fileName;
	          a.click();
	          window.URL.revokeObjectURL(url);
	      },function (error) {
	    	  console.log('ERROR : '+error);
	      });
      }

    // ********************************************************************************* //

  });
