/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { Component, Input, ChangeDetectorRef } from '@angular/core';
import { Store } from '@ngrx/store';
import * as fromRoot from 'reducers';

@Component({
   selector: 'user-profile',
   styleUrls: ['user-profile.component.scss'],
   templateUrl: 'user-profile.component.html'
})

export class UserProfileComponent {

   @Input() userName = '';
   public showUserProfileMenu = false;
   public userMenuOffset = { x: 0, y: 7 };

   constructor(private store: Store<fromRoot.State>, private _cd: ChangeDetectorRef) {
      this._clickListener = this._clickListener.bind(this);
   }

   logout() {
      window.location.href = 'logout';
   }

   toggleUserProfile(event): void {
      this.showUserProfileMenu = !this.showUserProfileMenu;
      event.stopPropagation();

      if (this.showUserProfileMenu) {
         window.addEventListener('click', this._clickListener);
      } else {
         window.removeEventListener('click', this._clickListener);
      }
   }

   private _clickListener() {
      this.showUserProfileMenu = false;
      window.removeEventListener('click', this._clickListener);
      this._cd.markForCheck();
   }
}
