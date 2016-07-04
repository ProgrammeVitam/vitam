angular.module('core')
.service('archiveDetailsService', ['$http', 'ihmDemoFactory', function($http, ihmDemoFactory){
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
]);
