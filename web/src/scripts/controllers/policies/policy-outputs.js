(function () {
  'use strict';

  /*POLICY OUTPUTS CONTROLLER*/
  angular
    .module('webApp')
    .controller('PolicyOutputCtrl', PolicyOutputCtrl);

  PolicyOutputCtrl.$inject = ['FragmentFactory', 'PolicyModelFactory', '$q', 'UtilsService'];

  function PolicyOutputCtrl(FragmentFactory, PolicyModelFactory, $q, UtilsService) {
    var vm = this;
    vm.setOutput = setOutput;
    vm.previousStep = previousStep;
    vm.validateForm = validateForm;

    init();

    ////////////////////////////////////

    function init() {
      var defer = $q.defer();

      vm.template = PolicyModelFactory.getTemplate();
      vm.helpLink = vm.template.helpLinks.outputs;
      vm.formSubmmited = false;
      vm.error = false;
      vm.outputList = [];
      vm.policy = PolicyModelFactory.getCurrentPolicy();

      var outputList = FragmentFactory.getFragments("output");
      outputList.then(function (result) {
        vm.outputList = result;
        initOutputs();
        defer.resolve();
      }, function () {
        defer.reject();
      });
      return defer.promise;
    }

    function initOutputs() {
      var outputs = [];
      if (vm.policy.outputs) {
        for (var i = 0; i < vm.policy.outputs.length; ++i) {
          var currentOutput = vm.policy.outputs[i];
          var position = UtilsService.findElementInJSONArray(vm.outputList, currentOutput, "id");
          if (position != -1) {
            outputs[position] = currentOutput;
          }
        }
      }
      vm.policy.outputs = outputs;
    }

    function setOutput(index) {
      if (vm.policy.outputs[index]) {
        vm.policy.outputs[index] = null;
      }
      else {
        vm.policy.outputs[index] = vm.outputList[index];
      }

      var outputsSelected = checkOutputsSelected();
      vm.error = (outputsSelected > 0) ? false : true;
    }

    function previousStep() {
      PolicyModelFactory.previousStep();
    }

    function validateForm() {
      vm.formSubmmited = true;
      var selectedOutputs = checkOutputsSelected();

      if (selectedOutputs > 0) {
        vm.error = false;
        PolicyModelFactory.nextStep();
      }
      else {
        vm.error = true;
      }
    }

    function checkOutputsSelected() {
      var outputsCount = 0;
      var outputsLength = vm.policy.outputs.length;

      for (var i = outputsLength - 1; i >= 0; i--) {
        if (vm.policy.outputs[i]) {
          outputsCount++;
        }
      }
      return outputsCount;
    }
  }
})();
