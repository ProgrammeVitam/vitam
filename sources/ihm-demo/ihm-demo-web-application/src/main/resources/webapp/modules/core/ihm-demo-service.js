angular.module('core')
.service('archiveDetailsService', ['ihmDemoFactory', function(ihmDemoFactory){
  var self = this;

  // Call REST Service to find archive details
  self.findArchiveUnitDetails = function (archiveUnitId, displayFormCallBack, failureCallback) {
    ihmDemoFactory.getArchiveUnitDetails(archiveUnitId)
    .then(function (response) {
      displayFormCallBack(response.data);
    },function (error) {
      failureCallback(error.message)
    });
  };
}
])
.service('responseValidator', function(){
  var self = this;
  self.validateReceivedResponse = function (responseToValidate) {
    if (responseToValidate.data === undefined || responseToValidate.data.$hits === undefined ||
        responseToValidate.data.$hits === null || responseToValidate.data.$results === undefined || responseToValidate.data.$results === null) {
      // Invalid response
      // Display error message
      return false;
    }

    return true;
  };
})
.service('dateValidator', function(){
    // FIXME Use an external component or a js native function to do exactly the good stuff ?
    var self = this;
    self.validateDate = function (dateToValidate) {
      var dateParts = dateToValidate.split('/');
      if(dateParts.length !== 3){
        return false;
      }

      var date = Number.parseInt(dateParts[0]);
      var month = Number.parseInt(dateParts[1]);
      var year = Number.parseInt(dateParts[2]);

      var isDateValid = date !== 'NaN' && date >= 1 && date <= 31;
      var isMonthValid = month !== 'NaN' && month >= 1 && month <= 12;
      var isYearValid = year !== 'NaN' && year >= 1;

      if(!isDateValid || !isMonthValid || !isYearValid){
        return false;
      }

      // February Case
      if(month == 2 && date > 29){
        return false;
      }

      return true;
    };
  }
)
.service('loadStaticValues', function($http) {
  var self = this;

  var filePath = 'static/values.json';
  var promise = null;

  self.loadFromFile = function() {
    // File loaded only on the first call (change page or refresh page (F5) will fire another HTTP call
    if (!promise) {
      promise = $http.get(filePath);
    }
    return promise;
  }

});

