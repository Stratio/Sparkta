import { SpHighlightTextareaComponent } from '../shared/components/highlight-textarea/highlight-textarea.component';
/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
    StModalModule, StProgressBarModule, StTagInputModule, StFullscreenLayoutModule,
    StHorizontalTabsModule, StModalService, StDropdownMenuModule, EgeoResolveService, StForegroundNotificationsModule
} from '@stratio/egeo';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';
import { PERFECT_SCROLLBAR_CONFIG } from 'ngx-perfect-scrollbar';
import { PerfectScrollbarConfigInterface } from 'ngx-perfect-scrollbar';

import {
    WizardComponent, WizardHeaderComponent, WizardConfigEditorComponent,
    WizardEditorComponent, WizardEditorService, WizardNodeComponent,
    DraggableSvgDirective, WizardEdgeComponent, SelectedEntityComponent,
    WizardSettingsComponent
} from '.';
import { WizardRoutingModule } from './wizard.router';
import { SharedModule } from '@app/shared';
import { WizardModalComponent } from './components/wizard-modal/wizard-modal.component';
import { WizardDetailsComponent } from './components/wizard-details/wizard-details.component';
import { WizardService } from './services/wizard.service';
import { ValidateSchemaService } from './services/validate-schema.service';
import { WizardEffect } from './effects/wizard';
import { DebugEffect } from './effects/debug';

import { reducers } from './reducers/';
import { WizardEditorContainer } from './containers/wizard-editor-container/wizard-editor-container.component';
import { CrossdataModule } from '@app/crossdata/crossdata.module';
import { EdgeOptionsComponent } from '@app/wizard/components/edge-options/edge-options.component';
import { MocksConfigComponent } from '@app/wizard/components/wizard-config-editor/mocks-config/mocks-config.component';
import { HighlightTextareaModule } from '@app/shared/components/highlight-textarea/hightlight-textarea.module';
import { SidebarConfigComponent } from '@app/wizard/components/wizard-config-editor/sidebar-config/sidebar-config.component';

const DEFAULT_PERFECT_SCROLLBAR_CONFIG: PerfectScrollbarConfigInterface = {
    suppressScrollX: true
};

@NgModule({
    declarations: [
        WizardComponent,
        WizardHeaderComponent,
        WizardEditorContainer,
        WizardEditorComponent,
        WizardSettingsComponent,
        WizardNodeComponent,
        WizardDetailsComponent,
        DraggableSvgDirective,
        WizardEdgeComponent,
        WizardConfigEditorComponent,
        SelectedEntityComponent,
        EdgeOptionsComponent,
        WizardModalComponent,
        MocksConfigComponent,
        SidebarConfigComponent
    ],
    imports: [
        StProgressBarModule,
        StTagInputModule,
        StFullscreenLayoutModule,
        StDropdownMenuModule,
        StHorizontalTabsModule,
        StForegroundNotificationsModule,
        StModalModule.withComponents([WizardModalComponent]),
        StoreModule.forFeature('wizard', reducers),
        EffectsModule.forFeature([DebugEffect, WizardEffect]),
        HighlightTextareaModule,
        WizardRoutingModule,
        FormsModule,
        SharedModule,
        FormsModule,
        ReactiveFormsModule,
        CrossdataModule,
        PerfectScrollbarModule
    ],
    providers: [
        WizardEditorService,
        WizardService,
        ValidateSchemaService,
        StModalService,
        EgeoResolveService,
        {
            provide: PERFECT_SCROLLBAR_CONFIG,
            useValue: DEFAULT_PERFECT_SCROLLBAR_CONFIG
        }]
})

export class WizardModule { }
