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

import { Action } from '@ngrx/store';

export const SERVER_ERROR = '[Errors] Server error';
export const SERVER_ERROR_COMPLETE = '[Errors] Server error complete';
export const FORBIDDEN_ERROR = '[Errors] Forbidden error';
export const CHANGE_ROUTE = '[Errors] Change route';
export const HTTP_ERROR = '[Errors] Http error';
export const SAVED_DATA_NOTIFICATION = '[Error] Saved data notification';
export const HIDE_SAVED_DATA_NOTIFICATION = '[Error] Hide saved data notification';

export class ServerErrorAction implements Action {
  readonly type = SERVER_ERROR;
  constructor(public payload: any) { }
}

export class ServerErrorCompleteAction implements Action {
  readonly type = SERVER_ERROR_COMPLETE;
  constructor(public payload: any) { }
}


export class ForbiddenErrorAction implements Action {
  readonly type = FORBIDDEN_ERROR;
  constructor(public payload: any) { }
}

export class ChangeRouteAction implements Action {
  readonly type = CHANGE_ROUTE;
}

export class HttpErrorAction implements Action {
  readonly type = HTTP_ERROR;
  constructor(public payload: any) { }
}

export class SavedDataNotificationAction implements Action {
  readonly type = SAVED_DATA_NOTIFICATION;
}

export class HideSavedDataNotificationAction implements Action {
  readonly type = HIDE_SAVED_DATA_NOTIFICATION;
}

export type Actions = ServerErrorAction |
  ForbiddenErrorAction |
  ChangeRouteAction |
  HttpErrorAction |
  SavedDataNotificationAction |
  HideSavedDataNotificationAction |
  ServerErrorCompleteAction;

