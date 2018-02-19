///
/// Copyright (C) 2015 Stratio (http://stratio.com)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { Action, Store } from '@ngrx/store';
import { Actions, Effect } from '@ngrx/effects';
import { Observable } from 'rxjs/Observable';

import * as resourcesActions from 'actions/resources';
import { ResourcesService } from 'app/services';
import * as fromRoot from 'reducers';
import * as errorActions from 'actions/errors';

@Injectable()
export class ResourcesEffect {

    @Effect()
    getPluginsList$: Observable<Action> = this.actions$
        .ofType(resourcesActions.LIST_PLUGINS).switchMap((response: any) => {
            return this.resourcesService.getPluginsList()
                .map((pluginsList: any) => {
                    return new resourcesActions.ListPluginsCompleteAction(pluginsList);
                }).catch(function (error) {
                    return Observable.from([
                        new resourcesActions.ListPluginsErrorAction(''),
                        new errorActions.ServerErrorAction(error)
                    ]);
                });
        });

    @Effect()
    getDriversList$: Observable<Action> = this.actions$
        .ofType(resourcesActions.LIST_DRIVERS).switchMap((response: any) => {
            return this.resourcesService.getDriversList()
                .map((driversList: any) => {
                    return new resourcesActions.ListDriversCompleteAction(driversList);
                }).catch(function (error) {
                    return Observable.from([
                        new resourcesActions.ListDriversErrorAction(''),
                        new errorActions.ServerErrorAction(error)
                    ]);
                });
        });

    @Effect()
    uploadDriver$: Observable<Action> = this.actions$
        .ofType(resourcesActions.UPLOAD_DRIVER).switchMap((data: any) => {
            return this.resourcesService.uploadDriver(data.payload)
                .mergeMap(() => {
                    return [new resourcesActions.UploadDriverCompleteAction(''), new resourcesActions.ListDriversAction()];
                }).catch(function (error) {
                    return Observable.from([
                        new resourcesActions.UploadDriverErrorAction(''),
                        new errorActions.ServerErrorAction(error)
                    ]);
                });
        });

    @Effect()
    uploadPlugin$: Observable<Action> = this.actions$
        .ofType(resourcesActions.UPLOAD_PLUGIN).switchMap((data: any) => {
            return this.resourcesService.uploadPlugin(data.payload)
                .mergeMap(() => {
                    return [new resourcesActions.UploadPluginCompleteAction(''), new resourcesActions.ListPluginsAction()];
                }).catch(function (error) {
                    return Observable.from([
                        new resourcesActions.UploadPluginErrorAction(''),
                        new errorActions.ServerErrorAction(error)
                    ]);
                });
        });

    @Effect()
    deletePlugin$: Observable<Action> = this.actions$
        .ofType(resourcesActions.DELETE_PLUGIN)
        .withLatestFrom(this.store.select(state => state.resources))
        .switchMap(([payload, resources]: [any, any]) => {
            const joinObservables: Observable<any>[] = [];
            resources.selectedPlugins.forEach((fileName: string) => {
                joinObservables.push(this.resourcesService.deletePlugin(fileName));
            });
            return Observable.forkJoin(joinObservables).mergeMap(results => {
                return [new resourcesActions.DeletePluginCompleteAction(''), new resourcesActions.ListPluginsAction()];
            }).catch(function (error: any) {
                return Observable.from([
                    new resourcesActions.DeletePluginErrorAction(''),
                    new errorActions.ServerErrorAction(error)
                ]);
            });
        });


    @Effect()
    deleteDriver$: Observable<Action> = this.actions$
        .ofType(resourcesActions.DELETE_DRIVER).switchMap((data: any) => {
            return this.resourcesService.deleteDriver(data.payload)
                .mergeMap(() => {
                    return [new resourcesActions.DeleteDriverCompleteAction(''), new resourcesActions.ListDriversAction()];
                }).catch(function (error) {
                    return Observable.from([
                        new resourcesActions.DeleteDriverErrorAction(''),
                        new errorActions.ServerErrorAction(error)
                    ]);
                });
        });

    constructor(
        private actions$: Actions,
        private resourcesService: ResourcesService,
        private store: Store<fromRoot.State>
    ) { }
}
