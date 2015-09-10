(function () {
  'use strict';

  angular
    .module('webApp')
    .service('ModelStaticDataFactory', ModelStaticDataFactory);

  function ModelStaticDataFactory() {

    return {
      types: [{iconName: "shuffle", name: "morphline"}, {iconName: "calendar", name: "datetime"}, {iconName: "alt", name: "type"}],
      defaultInput:["raw"],
      configHelpLink: "http://docs.stratio.com/modules/sparkta/development/transformations.html"
    }
  }
})
();
