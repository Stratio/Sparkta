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

  /*DELETE POLICIES MODAL CONTROLLER */
  angular
    .module('webApp')
    .controller('DeletePolicyModalCtrl', DeletePolicyModalCtrl);

  DeletePolicyModalCtrl.$inject = ['$modalInstance', 'item', 'PolicyFactory'];

  function DeletePolicyModalCtrl($modalInstance, item, PolicyFactory) {
    /*jshint validthis: true*/
    var vm = this;

    vm.ok = ok;
    vm.cancel = cancel;
    vm.error = false;

    init();

    ///////////////////////////////////////

    function init() {
      vm.policyData = item;
    }

    function ok() {
      return PolicyFactory.deletePolicy(vm.policyData.id).then(function () {
        $modalInstance.close(vm.policyData);

      }, function (error) {
        vm.error = true;
        vm.errorText = "_INPUT_ERROR_" + error.data.i18nCode + "_";
      });
    }

    function cancel() {
      $modalInstance.dismiss('cancel');
    }
  }

})();
