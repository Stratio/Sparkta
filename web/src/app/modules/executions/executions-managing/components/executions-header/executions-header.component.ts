/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import {
   ChangeDetectionStrategy,
   Component,
   EventEmitter,
   Input,
   Output
} from '@angular/core';
import { Router } from '@angular/router';
import { StModalService } from '@stratio/egeo';

import { BreadcrumbMenuService } from 'services';

@Component({
   selector: 'executions-managing-header',
   styleUrls: ['executions-header.component.scss'],
   templateUrl: 'executions-header.component.html',
   changeDetection: ChangeDetectionStrategy.OnPush
})

export class ExecutionsHeaderComponent {

   @Input() selectedExecutions: Array<any> = [];
   @Input() showDetails = false;

   @Output() downloadExecutions = new EventEmitter<void>();
   @Output() onRunExecutions = new EventEmitter<any>();
   @Output() onStopExecution = new EventEmitter<any>();
   @Output() onSearch = new EventEmitter<any>();
   @Output() showExecutionInfo = new EventEmitter<void>();

   public searchQuery= '';

   public breadcrumbOptions: string[] = [];

   constructor(private _modalService: StModalService, public breadcrumbMenuService: BreadcrumbMenuService, private route: Router) {
      this.breadcrumbOptions = ['Home', 'executions'];
   }

}
