/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartsModule } from 'ng2-charts';
import { TranslateModule } from '@ngx-translate/core';

import { ExecutionsChartComponent } from './executions-chart.component';


@NgModule({
   imports: [
     ChartsModule,
     CommonModule,
     TranslateModule
   ],
   declarations: [ExecutionsChartComponent],
   exports: [ExecutionsChartComponent]
})
export class ExecutionsChartModule { }
