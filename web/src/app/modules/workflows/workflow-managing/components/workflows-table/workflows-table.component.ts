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
    ChangeDetectorRef
} from '@angular/core';
import { StTableHeader, Order } from '@stratio/egeo';
import { Router } from '@angular/router';

import { Group } from '../../models/workflows';

@Component({
    selector: 'workflows-manage-table',
    styleUrls: ['workflows-table.component.scss'],
    templateUrl: 'workflows-table.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class WorkflowsManagingTableComponent {

    @Input() workflowList: Array<any> = [];
    @Input() groupList: Array<any> = [];
    @Input() selectedWorkflows: Array<string> = [];
    @Input() selectedGroupsList: Array<string> = [];
    @Input() selectedVersions: Array<string> = [];
    @Input() workflowVersions: Array<any> = [];

    @Output() onChangeOrder = new EventEmitter<Order>();
    @Output() onChangeOrderVersions = new EventEmitter<Order>();
    @Output() selectWorkflow = new EventEmitter<string>();
    @Output() selectGroup = new EventEmitter<string>();
    @Output() selectVersion = new EventEmitter<string>();
    @Output() openWorkflow = new EventEmitter<any>();
    @Output() changeFolder = new EventEmitter<any>();

    public fields: StTableHeader[];
    public versionFields: StTableHeader[];

    public openWorkflows: Array<string> = [];

    changeOrder(event: Order): void {
        this.onChangeOrder.emit(event);
    }

    changeOrderVersions(event: Order): void {
        this.onChangeOrderVersions.emit(event);
    }

    checkVersion(id: string) {
        this.selectVersion.emit(id);
    }

    checkWorkflow(workflow: any) {
        this.selectWorkflow.emit(workflow.name);
    }

    checkGroup(group: Group) {
        this.selectGroup.emit(group.name);
    }

    openWorkflowClick(event: Event, workflow: any) {
        event.stopPropagation();
        this.openWorkflow.emit(workflow);
    }

    openGroup(event: Event, group: Group) {
       event.stopPropagation();
       this.changeFolder.emit(group);
    }

    showSparkUI(url: string) {
        window.open(url, '_blank');
    }

    editVersion(event: Event, versionId: string) {
        event.stopPropagation();
        this.route.navigate(['wizard/edit', versionId]);
    }

    constructor(private route: Router, private _cd: ChangeDetectorRef) {

        this.fields = [
            { id: 'name', label: 'Name' },
            { id: 'executionEngine', label: 'type' },
            { id: 'lastUpdateAux', label: 'Last update' }
        ];

        this.versionFields = [
            { id: 'version', label: 'Version' },
            { id: 'tagsAux', label: 'Tag' },
            { id: 'lastUpdateAux', label: 'Last update' }
        ];
    }
}
