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

  angular
    .module('webApp')
    .service('OutputService', OutputService);

  OutputService.$inject = ['FragmentFactory',  '$q'];

  function OutputService(  FragmentFactory,  $q) {
    var vm = this;
    var outputNameList = null;
    var outputList = null;

    vm.generateOutputNameList = generateOutputNameList;
    vm.getOutputList = getOutputList;

    function generateOutputNameList() {
      var defer = $q.defer();
      if (outputNameList) {
        defer.resolve(outputNameList);
      } else {
        outputNameList = [];
        FragmentFactory.getFragments("output").then(function (result) {
          for (var i = 0; i < result.length; ++i) {
            outputNameList.push({"label": result[i].name, "value": result[i].name});
          }
          defer.resolve(outputNameList);
        });
      }
      return defer.promise;
    }


    function getOutputList(){
      var defer = $q.defer();
      if (outputList) {
        defer.resolve(outputList);
      } else {
        outputList = [];
        FragmentFactory.getFragments("output").then(function (result) {
          defer.resolve(result);
        });
      }
      return defer.promise;
    }
  }
})();
