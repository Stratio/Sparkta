/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SpSelectModule } from '../sp-select/sp-select.module';
import { ReactiveFormsModule } from '@angular/forms';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

 import { SpCrossdataInputComponent } from './sp-crossdata-input.component';
import { CrossdataInputEffect } from './effects/crossdata';
import { reducer } from './reducers';


 @NgModule({
  declarations: [SpCrossdataInputComponent],
  imports: [
    CommonModule,
    StoreModule.forFeature('crossdataInput', reducer),
    EffectsModule.forFeature([CrossdataInputEffect]),
    ReactiveFormsModule,
    SpSelectModule
  ],
  exports: [SpCrossdataInputComponent],
  providers: [],
})
export class SpCrossdataInputModule { }
