/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { StModalModule, StProgressBarModule, StTagInputModule, StFullscreenLayoutModule,
    StHorizontalTabsModule, StModalService } from '@stratio/egeo';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

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
import { reducers } from './reducers/';
import { WizardEditorContainer } from './containers/wizard-editor-container/wizard-editor-container.component';
import { CrossdataModule } from "@app/crossdata/crossdata.module";

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
        WizardModalComponent
    ],
    imports: [
        StProgressBarModule,
        StTagInputModule,
        StFullscreenLayoutModule,
        StHorizontalTabsModule,
        StModalModule.withComponents([WizardModalComponent]),
        StoreModule.forFeature('wizard', reducers),
        EffectsModule.forFeature([WizardEffect]),
        WizardRoutingModule,
        FormsModule,
        SharedModule,
        FormsModule,
        ReactiveFormsModule,
        CrossdataModule
    ],
    providers: [WizardEditorService, WizardService, ValidateSchemaService, StModalService]
})

export class WizardModule { }
