/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import {
  Component, OnInit, OnDestroy, ChangeDetectionStrategy, Input, ViewChild
} from '@angular/core';
import { Store } from '@ngrx/store';
import { NgForm } from '@angular/forms';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { cloneDeep as _cloneDeep } from 'lodash';

import * as fromWizard from './../../reducers';
import * as wizardActions from './../../actions/wizard';

import { settingsTemplate } from 'data-templates/index';
import { WizardService } from '@app/wizard/services/wizard.service';
import { InitializeSchemaService } from 'app/services/initialize-schema.service';
import { HelpOptions } from '@app/shared/components/sp-help/sp-help.component';


@Component({
  selector: 'wizard-settings',
  styleUrls: ['wizard-settings.styles.scss'],
  templateUrl: 'wizard-settings.template.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class WizardSettingsComponent implements OnInit, OnDestroy {

  @Input() edition: boolean;
  @Input() parameters;
  @Input() workflowType: string;
  @ViewChild('entityForm') public entityForm: NgForm;

  public basicSettings: any = [];
  public advancedSettings: any = {};
  public submitted = true;
  public currentName = '';
  public tags: Array<string> = [];
  public fadeActive = false;
  public isShowedHelp = false;
  public parametersLists = [];

  public basicFormModel: any = {};    // inputs, outputs, transformation basic settings (name, description)
  public entityFormModel: any = {};   // common config
  public settingsFormModel: any = {}; // advanced settings
  public valueDictionary: any = {};
  public helpOptions: Array<HelpOptions> = [];
  private errorManagementOutputs: Array<string> = [];
  private settingsSubscription: Subscription;

  ngOnInit(): void {
    setTimeout(() => {
      this.fadeActive = true;
    });
    // get all error management outputs
    this.store.select(fromWizard.getErrorsManagementOutputs).pipe(take(1)).subscribe((errorManagementOutputs: Array<string>) => {
      this.errorManagementOutputs = errorManagementOutputs;
    });
    this.getFormTemplate();
  }

  cancelEdition() {
    this.store.dispatch(new wizardActions.HideSettingsAction());
  }

  getFormTemplate() {
    this.settingsSubscription = this.store.select(fromWizard.getWorkflowSettings).subscribe((currentSettings: any) => {
      const settings = _cloneDeep(currentSettings);
      this.tags = settings.basic.tags;
      this.basicFormModel = settings.basic;
      this.settingsFormModel = settings.advancedSettings;
      this.parametersLists = this.settingsFormModel.global.parametersLists || [];

    });
    const settings = _cloneDeep(<any>settingsTemplate);

    // hides workflow name edition when its in edit mode
    this.basicSettings = this.edition ? settings.basicSettings.map((field: any) => {
      return field.propertyId === 'name' ? Object.assign({}, field, {
        hidden: true
      }) : field;
    }) : settings.basicSettings;
    this.valueDictionary['outputErrors'] = this.errorManagementOutputs.map((outputName: string) => {
      return {
        label: outputName,
        value: outputName
      };
    });
    this.valueDictionary.parameters = this.parameters;
    this.advancedSettings = this.workflowType === 'Streaming' ? settings.advancedSettings :
      settings.advancedSettings.filter((section: any) => {
        return section.name !== 'streamingSettings';
      });
    this.helpOptions = this._initializeSchemaService.getHelpOptions(this.advancedSettings[0].properties);

  }

  saveForm() {
    if (this.entityForm.valid) {
      this.basicFormModel.tags = this.tags;
      this.settingsFormModel.global.parametersLists = this.parametersLists;
      this.store.dispatch(new wizardActions.SaveSettingsAction({
        basic: this.basicFormModel,
        advancedSettings: this.settingsFormModel
      }));
      this.store.dispatch(new wizardActions.ValidateWorkflowAction());
      this.store.dispatch(new wizardActions.HideSettingsAction());
    }
  }

  onChangeGroup(event) {
    this.helpOptions = this._initializeSchemaService.getHelpOptions(this.advancedSettings[event].properties);
  }

  closeHelp() {
    this.isShowedHelp = false;
  }

  constructor(private store: Store<fromWizard.State>,
    private _initializeSchemaService: InitializeSchemaService,
    private _wizardService: WizardService) {
  }

  ngOnDestroy(): void {
    this.settingsSubscription && this.settingsSubscription.unsubscribe();
  }

}
