/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
  'use strict';

  /*NEW OPERATOR MODAL CONTROLLER */
  angular
    .module('webApp')
    .controller('NewOperatorModalCtrl', NewOperatorModalCtrl);

  NewOperatorModalCtrl.$inject = ['$modalInstance', 'operatorName', 'operatorType', 'operators', 'UtilsService', 'template'];

  function NewOperatorModalCtrl($modalInstance, operatorName, operatorType, operators, UtilsService, template) {
    /*jshint validthis: true*/
    var vm = this;

    vm.ok = ok;
    vm.cancel = cancel;

    init();

    function init() {
      vm.operator = {};
      vm.operator.name = operatorName;
      vm.operator.configuration = "";
      vm.operator.type = operatorType;
      vm.configHelpLink = template.configurationHelpLink;
      vm.error = false;
      vm.errorText = "";
      setDefaultConfiguration();
    }

    ///////////////////////////////////////

    function isRepeated() {
      var position = UtilsService.findElementInJSONArray(operators, vm.operator, "name");
      var repeated = position != -1;
      if (repeated) {
        vm.errorText = "_POLICY_._CUBE_._OPERATOR_NAME_EXISTS_";
      }
      return repeated;
    }

    function setDefaultConfiguration() {
      var defaultConfiguration = {};
      var countType = template.functionNames[2];
      if (vm.operator.type !== countType) {
        defaultConfiguration = template.defaultOperatorConfiguration;
      }
      vm.operator.configuration = defaultConfiguration;
    }

    function ok() {
      if (vm.form.$valid) {
        if (!isRepeated()) {
          $modalInstance.close(vm.operator);
        }
      }
    };

    function cancel() {
      $modalInstance.dismiss('cancel');
    };
  };

})();
