
/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { StCheckboxModule, StSwitchModule, StInputModule, StTextareaModule } from '@stratio/egeo';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

import { FormFieldComponent } from './form-field.component';
import { SpSelectModule } from '../sp-select/sp-select.module';
import { HighlightTextareaModule } from '../highlight-textarea/hightlight-textarea.module';
import { FormListModule } from '../form-list/form-list.module';
import { SpTextareaModule } from '@app/shared/components/sp-textarea/sp-textarea.module';
import { SpInputModule } from '@app/shared/components/sp-input/sp-input.module';
import { SpColumnInputModule } from '../sp-column-input/sp-column-input.module';
import { MlTagInputModule } from '@app/shared/components/ml-tag-input/ml-tag-input.module';
import {MlChipsModule} from '@app/shared/components/ml-chips/ml-chips.module';
import { SpCrossdataInputModule } from '../sp-crossdata-input/sp-crossdata-input.module';

@NgModule({
  exports: [
    FormFieldComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    FormListModule,
    ReactiveFormsModule,
    HighlightTextareaModule,
    SpSelectModule,
    StTextareaModule,
    SpTextareaModule,
    StCheckboxModule,
    StSwitchModule,
    StInputModule,
    SpInputModule,
    MlTagInputModule,
    TranslateModule,
    SpColumnInputModule,
    SpCrossdataInputModule,
    MlChipsModule
  ],
  declarations: [
    FormFieldComponent
  ]
})

export class FormFieldModule { }
