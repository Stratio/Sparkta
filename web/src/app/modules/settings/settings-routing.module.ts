///
/// Copyright (C) 2015 Stratio (http://stratio.com)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { SettingsComponent } from './settings.component';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SpartaBackups } from './backups/backups.component';
import { SpartaDrivers } from './resources/drivers/drivers.component';
import { SpartaPlugins } from './resources/plugins/plugins.component';
import { EnvironmentComponent } from './environment/environment.component';


const settingsRoutes: Routes = [
   {
      path: '',
      component: SettingsComponent,
      children: [
         {
            path: '',
            redirectTo: 'backups'
         },
         {
            path: 'backups',
            component: SpartaBackups
         },
        {
            path: 'resources',
            redirectTo: 'resources/plugins'
         },
         {
            path: 'resources/drivers',
            component: SpartaDrivers
         },
         {
            path: 'plugins',
            component: SpartaPlugins
         },
         {
            path: 'environment',
            component: EnvironmentComponent
         }
      ]
   }
];

@NgModule({
   exports: [
      RouterModule
   ],
   imports: [
      RouterModule.forChild(settingsRoutes)
   ]
})

export class SettingsRoutingModule { }
