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
(function() {
  'use strict';

    /*DUPLICATE INPUT MODAL CONTROLLER */
    angular
        .module('webApp')
        .controller('DuplicateFragmentModalCtrl', DuplicateFragmentModalCtrl);

    DuplicateFragmentModalCtrl.$inject = ['$modalInstance', 'item', 'FragmentFactory', '$filter'];

    function DuplicateFragmentModalCtrl($modalInstance, item, FragmentFactory, $filter) {
        /*jshint validthis: true*/
        var vm = this;

        vm.ok = ok;
        vm.cancel = cancel;
        vm.error = false;
        vm.errorText = '';

        init();

        ///////////////////////////////////////

        function init () {
            setTexts(item.texts);
            vm.fragmentData = item.fragmentData;
        };

        function setTexts(texts) {
          vm.modalTexts = {};
          vm.modalTexts.title = texts.title;
        };

        function ok() {
            if (vm.form.$valid){
                checkFragmnetname();
            }
        };

        function checkFragmnetname() {
          var fragmentNamesExisting = [];
          var newFragmentName = vm.fragmentData.name.toLowerCase();
          fragmentNamesExisting = $filter('filter')(item.fragmentNamesList, {'name': newFragmentName}, true);

          if (fragmentNamesExisting.length > 0) {
            vm.error = true;
            vm.errorText = "_INPUT_ERROR_100_";
          }
          else {
            createfragment();
          }
        };

        function createfragment() {
            delete vm.fragmentData['id'];
            var newFragment = FragmentFactory.createFragment(vm.fragmentData);

            newFragment.then(function (result) {
                $modalInstance.close(result);

            },function (error) {
                vm.error = true;
                vm.errorText = "_INPUT_ERROR_" + error.data.i18nCode + "_";
            });
        };

        function cancel() {
            $modalInstance.dismiss('cancel');
        };
    };
})();
