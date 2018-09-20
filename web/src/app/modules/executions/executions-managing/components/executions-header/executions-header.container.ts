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
   Output,
   OnInit
} from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';

import { State } from './../../reducers';
import * as executionActions from '../../actions/executions';
import * as fromRoot from './../../reducers';

@Component({
   selector: 'executions-managing-header-container',
   template: `
        <executions-managing-header [selectedExecutions]="selectedExecutions"
            [showDetails]="showDetails"
            [statusFilter]="statusFilter$ | async"
            [typeFilter]="typeFilter$ | async"
            [timeIntervalFilter]="timeIntervalFilter$ | async"
            (downloadExecutions)="downloadExecutions()"
            (showExecutionInfo)="showExecutionInfo.emit()"
            (onRunExecutions)="onRunExecutions($event)"
            (onStopExecution)="stopExecution()"
            (onChangeStatusFilter)="changeStatusFilter($event)"
            (onChangeTypeFilter)="changeTypeFilter($event)"
            (onChangeTimeIntervalFilter)="changeTimeIntervalFilter($event)"
            (onSearch)="searchExecutions($event)"></executions-managing-header>
    `,
   changeDetection: ChangeDetectionStrategy.OnPush
})

export class ExecutionsHeaderContainer implements OnInit {

   @Input() selectedExecutions: Array<any>;
   @Input() showDetails: boolean;

   @Output() showExecutionInfo = new EventEmitter<void>();

   public statusFilter$: Observable<string>;
   public typeFilter$: Observable<string>;
   public timeIntervalFilter$: Observable<number>;

   constructor(private _store: Store<State>) { }

   ngOnInit(): void {
      this.statusFilter$ = this._store.select(fromRoot.getStatusFilter);
      this.typeFilter$ = this._store.select(fromRoot.getTypeFilter);
      this.timeIntervalFilter$ = this._store.select(fromRoot.getTimeIntervalFilter);
   }

   downloadExecutions(): void { }

   searchExecutions(text: string) {
      this._store.dispatch(new executionActions.SearchExecutionAction(text));
   }

   onRunExecutions(execution: any) { }

   changeStatusFilter(status: string) {
      this._store.dispatch(new executionActions.SelectStatusFilterAction(status));
   }

   changeTypeFilter(workflowType: string) {
      this._store.dispatch(new executionActions.SelectTypeFilterAction(workflowType));
   }

   changeTimeIntervalFilter(time: number) {
      this._store.dispatch(new executionActions.SelectTimeIntervalFilterAction(time));
   }

   stopExecution() {
      this._store.dispatch(new executionActions.StopExecutionAction());
   }
}
