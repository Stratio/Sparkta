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

export const GET_USER_PROFILE = '[User] Get user profile';
export const GET_USER_PROFILE_COMPLETE = '[User] Get user profile complete';
export const GET_USER_PROFILE_ERROR = '[User] Get user profile error';
export const SET_EDIT_MONITORING_MODE = '[User] Set edit monitoring mode';

export class GetUserProfileAction implements Action {
  readonly type = GET_USER_PROFILE;

  constructor() { }
}
export class GetUserProfileCompleteAction implements Action {
  readonly type = GET_USER_PROFILE_COMPLETE;

  constructor(public payload: any) { }
}

export class GetUserProfileErrorAction implements Action {
  readonly type = GET_USER_PROFILE_ERROR;

  constructor(public payload: any) { }
}

export class SetEditMonitoringModeAction implements Action {
  readonly type = SET_EDIT_MONITORING_MODE;
    constructor(public payload: any) { }
}

export type Actions =
  GetUserProfileAction |
  GetUserProfileCompleteAction |
  GetUserProfileErrorAction |
  SetEditMonitoringModeAction;

