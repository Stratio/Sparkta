(function () {
  'use strict';

  angular
    .module('webApp')
    .service('AccordionStatusService', AccordionStatusService);


  function AccordionStatusService() {
    var vm = this;
    vm.accordionStatus = {};
    vm.accordionStatus.items = [];
    vm.accordionStatus.newItem = true;

    vm.ResetAccordionStatus = ResetAccordionStatus;
    vm.GetAccordionStatus = GetAccordionStatus;

    function ResetAccordionStatus(length, truePosition) {
      for (var i = 0; i < length; ++i) {
        if (i == truePosition)
          vm.accordionStatus.items[i] = true;
        else
          vm.accordionStatus.items[i] = false;
      }
      if (truePosition == undefined)
        vm.accordionStatus.newItem = true;
    }

    function GetAccordionStatus() {
      return vm.accordionStatus;
    }
  }
})();
