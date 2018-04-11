/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { TemplatesService } from 'services/templates.service';
import { Injectable } from '@angular/core';
import { Action } from '@ngrx/store';

import { Effect, Actions } from '@ngrx/effects';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/withLatestFrom';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/forkJoin';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/from';
import { Observable } from 'rxjs/Observable';

import * as transformationActions from './../actions/transformation';
import * as errorActions from 'actions/errors';

@Injectable()
export class TransformationEffect {

    @Effect()
    getTransformationList$: Observable<Action> = this.actions$
        .ofType(transformationActions.LIST_TRANSFORMATION).switchMap((response: any) => {
            return this.templatesService.getTemplateList('transformation')
                .map((transformationList: any) => {
                    return new transformationActions.ListTransformationCompleteAction(transformationList);
                }).catch(function (error: any) {
                    return error.statusText === 'Unknown Error' ? Observable.of(new transformationActions.ListTransformationFailAction(''))
                     : Observable.of(new errorActions.ServerErrorAction(error));
                });
        });

    @Effect()
    getTransformationTemplate$: Observable<Action> = this.actions$
        .ofType(transformationActions.GET_EDITED_TRANSFORMATION)
        .map((action: any) => action.payload)
        .switchMap((param: any) => {
            return this.templatesService.getTemplateById('transformation', param)
                .map((transformation: any) => {
                    return new transformationActions.GetEditedTransformationCompleteAction(transformation)
                }).catch(function (error: any) {
                    console.log(error)
                    return error.statusText === 'Unknown Error' ? Observable.of(new transformationActions.GetEditedTransformationErrorAction(''))
                        : Observable.of(new errorActions.ServerErrorAction(error));
                });
        });
    @Effect()
    deleteTransformation$: Observable<Action> = this.actions$
        .ofType(transformationActions.DELETE_TRANSFORMATION)
        .map((action: any) => action.payload.selected)
        .switchMap((transformations: any) => {
            const joinObservables: Observable<any>[] = [];
            transformations.map((transformation: any) => {
                joinObservables.push(this.templatesService.deleteTemplate('transformation', transformation.id));
            });
            return Observable.forkJoin(joinObservables).mergeMap(results => {
                return [new transformationActions.DeleteTransformationCompleteAction(transformations), new transformationActions.ListTransformationAction()];
            }).catch(function (error) {
                return Observable.from([
                    new transformationActions.DeleteTransformationErrorAction(''),
                    new errorActions.ServerErrorAction(error)
                ]);
            });
        });

    @Effect()
    duplicateTransformation$: Observable<Action> = this.actions$
        .ofType(transformationActions.DUPLICATE_TRANSFORMATION)
        .switchMap((data: any) => {
            let transformation = Object.assign(data.payload);
            delete transformation.id;
            return this.templatesService.createTemplate(transformation).mergeMap((data: any) => {
                return [new transformationActions.DuplicateTransformationCompleteAction(), new transformationActions.ListTransformationAction];
            }).catch(function (error: any) {
                return Observable.of(new errorActions.ServerErrorAction(error));
            });
        });

    @Effect()
    createTransformation$: Observable<Action> = this.actions$
        .ofType(transformationActions.CREATE_TRANSFORMATION)
        .switchMap((data: any) => {
            return this.templatesService.createTemplate(data.payload).mergeMap((data: any) => {
                return [new transformationActions.CreateTransformationCompleteAction(), new transformationActions.ListTransformationAction];
            }).catch(function (error: any) {
                return Observable.of(new errorActions.ServerErrorAction(error));
            });
        });

    @Effect()
    updateTransformation$: Observable<Action> = this.actions$
        .ofType(transformationActions.UPDATE_TRANSFORMATION)
        .switchMap((data: any) => {
            return this.templatesService.updateFragment(data.payload).mergeMap((data: any) => {
                return [new transformationActions.UpdateTransformationCompleteAction(), new transformationActions.ListTransformationAction];
            }).catch(function (error: any) {
                return Observable.of(new errorActions.ServerErrorAction(error));
            });
        });

    constructor(
        private actions$: Actions,
        private templatesService: TemplatesService
    ) { }

}
