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

  /*POLICY CUBES CONTROLLER*/
  angular
    .module('webApp')
    .controller('PolicyCubeAccordionCtrl', PolicyCubeAccordionCtrl);

  PolicyCubeAccordionCtrl.$inject = ['PolicyModelFactory', 'CubeModelFactory', 'CubeService'];

  function PolicyCubeAccordionCtrl(PolicyModelFactory, CubeModelFactory, CubeService) {
    var vm = this;

    vm.init = init;
    vm.changeOpenedCube = changeOpenedCube;
    vm.isActiveCubeCreationPanel = CubeService.isActiveCubeCreationPanel;
    vm.activateCubeCreationPanel = CubeService.activateCubeCreationPanel;

    vm.error = "";

    vm.init();

    function init() {
      vm.template = PolicyModelFactory.getTemplate();
      vm.policy = PolicyModelFactory.getCurrentPolicy();
      vm.cubeAccordionStatus = [];
      vm.helpLink = vm.template.helpLinks.cubes;
      if (vm.policy.cubes.length > 0) {
        PolicyModelFactory.enableNextStep();
      } else {
        CubeService.changeCubeCreationPanelVisibility(true);
      }
    }

    function changeOpenedCube(selectedCubePosition) {
      if (vm.policy.cubes.length > 0 && selectedCubePosition >= 0 && selectedCubePosition < vm.policy.cubes.length) {
        var selectedCube = vm.policy.cubes[selectedCubePosition];
        CubeModelFactory.setCube(selectedCube, selectedCubePosition);
      } else {
        CubeModelFactory.resetCube(vm.template.cube, CubeService.getCreatedCubes(), vm.policy.cubes.length);
      }
    }
  }
})();
