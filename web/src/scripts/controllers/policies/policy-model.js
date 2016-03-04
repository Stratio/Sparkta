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

  /*POLICY MODEL CONTROLLER*/
  angular
    .module('webApp')
    .controller('PolicyModelCtrl', PolicyModelCtrl);

  PolicyModelCtrl.$inject = ['ModelFactory', 'PolicyModelFactory', 'ModelService'];

  function PolicyModelCtrl(ModelFactory, PolicyModelFactory, ModelService) {
    var vm = this;

    vm.init = init;
    vm.addModel = addModel;
    vm.removeModel = removeModel;
    vm.resetOutputFields = resetOutputFields;
    vm.onChangeType= onChangeType;
    vm.isLastModel = ModelService.isLastModel;
    vm.isNewModel = ModelService.isNewModel;
    vm.modelInputs = ModelFactory.getModelInputs();

    vm.init();

    function init() {
      vm.template = PolicyModelFactory.getTemplate();
      vm.policy = PolicyModelFactory.getCurrentPolicy();
      vm.model = ModelFactory.getModel();
      vm.modelError = '';
      vm.lastType = vm.template.model.types[0].name;
      if (vm.model) {
        vm.modelError = ModelFactory.getError();
        vm.modelContext = ModelFactory.getContext();
        vm.modelTypes = vm.template.model.types;
        vm.configPlaceholder = vm.template.configPlaceholder;
        vm.outputPattern = vm.template.outputPattern;
        vm.outputInputPlaceholder = vm.template.outputInputPlaceholder;
        vm.outputFieldTypes = vm.template.model.outputFieldTypes;
      }
    }

    function onChangeType(){
      switch (vm.model.type) {
        case "Morphlines":{
          vm.model.configuration = vm.template.model.morphlines.defaultConfiguration;
        }
      }
    }

    function addModel() {
      vm.form.$submitted = true;
      if (vm.form.$valid && vm.model.outputFields.length != 0) {
        vm.form.$submitted = false;
        ModelService.addModel();
        ModelService.changeModelCreationPanelVisibility(false);
      } else {
        ModelFactory.setError("_GENERIC_FORM_ERROR_");
      }
    }

    function removeModel() {
      return ModelService.removeModel().then(function () {
        var order = 0;
        var modelNumber = vm.policy.transformations.length;
        if (modelNumber > 0) {
          order = vm.policy.transformations[modelNumber - 1].order + 1
        }
        vm.model = ModelFactory.resetModel(vm.template.model, order, modelNumber);
        ModelFactory.updateModelInputs(vm.policy.transformations);
      });
    }

    function resetOutputFields() {
      if (vm.model.type !== vm.lastType) {
        vm.model.outputFields = [];
        vm.lastType = vm.model.type;
      }
    }
  }
})
();
