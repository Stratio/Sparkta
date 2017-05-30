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
    .service('ApiEntitiesService', ApiEntitiesService);

  ApiEntitiesService.$inject = ['$resource', 'apiConfigSettings', '$browser'];

  function ApiEntitiesService($resource, apiConfigSettings, $browser) {
    var vm = this;

    vm.getAllPlugins = getAllPlugins;
    vm.deletePlugin = deletePlugin;
    vm.createPlugin = createPlugin;
    vm.getAllDrivers = getAllDrivers;
    vm.deleteDriver = deleteDriver;
    vm.createDriver = createDriver;
    vm.getAllBackups = getAllBackups;
    vm.buildBackup = buildBackup;
    vm.deleteBackup = deleteBackup;
    vm.downloadBackup = downloadBackup;
    vm.uploadBackup = uploadBackup;
    vm.deleteAllBackups = deleteAllBackups;
    vm.executeBackup = executeBackup;
    vm.deleteMetadata = deleteMetadata;

    /////////////////////////////////

    function getAllPlugins() {
      return $resource('plugins', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function createPlugin() {
      return $resource('plugins', {}, {
        'put': {
          method: 'PUT',
          transformRequest: angular.identity,
          isArray: true,
          headers: {
            'Content-Type': undefined
          },
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function deletePlugin() {
      return $resource('plugins/:fileName', {
        fileName: '@fileName'
      }, {
        'delete': {
          method: 'DELETE',
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function getAllDrivers() {
      return $resource('driver', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function createDriver() {
      return $resource('driver', {}, {
        'put': {
          method: 'PUT',
          transformRequest: angular.identity,
          isArray: true,
          headers: {
            'Content-Type': undefined
          },
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function deleteDriver() {
      return $resource('driver/:fileName', {
        fileName: '@fileName'
      }, {
        'delete': {
          method: 'DELETE',
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function getAllBackups() {
      return $resource('metadata/backup', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function buildBackup() {
      return $resource('metadata/backup/build', {}, {
        'get': {
          method: 'GET',
          isArray: true,
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function deleteBackup() {
      return $resource('/metadata/backup/:fileName', {
        fileName: '@fileName'
      }, {
        'delete': {
          method: 'DELETE',
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function downloadBackup() {
      return $resource('/metadata/backup/:fileName', {
        fileName: '@fileName'
      }, {
        'get': {
          method: 'GET',
          isArray: true
        },
        timeout: apiConfigSettings.timeout
      });
    }

    function uploadBackup() {
      return $resource('metadata/backup', {}, {
        'put': {
          method: 'PUT',
          transformRequest: angular.identity,
          isArray: true,
          headers: {
            'Content-Type': undefined
          },
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function deleteAllBackups(){
      return $resource('/metadata/backup', {}, {
        'delete': {
          method: 'DELETE',
          timeout: apiConfigSettings.timeout
        }
      });
    }

    function executeBackup(){
      return $resource('/metadata/backup', {}, {
        'post': {
          method: 'POST',
          timeout: apiConfigSettings.timeout
        }
      }); 
    }

    function deleteMetadata(){
      return $resource('/metadata', {}, {
        'delete': {
          method: 'DELETE',
          timeout: apiConfigSettings.timeout
        }
      });
    }
  }
})();
