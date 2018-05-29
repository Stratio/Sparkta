/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ApiService} from './api.service';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class CrossdataService extends ApiService {

    constructor(private _http: HttpClient) {
        super(_http);
    }

    getCrossdataDatabases(): Observable<any> {
        const options: any = {};
        return this.request('crossdata/databases', 'get', options);
    }

    getCrossdataTables(): Observable<any> {
        const options: any = {};
        return this.request('crossdata/tables', 'get', options);
    }


    getDatabaseTables(query: any): Observable<any> {
        const options: any = {
            body: query
        };
        return this.request('crossdata/tables', 'post', options);
    }

    getCrossdataTableInfo(tableName: string): Observable<any> {

        const options: any = {
            body: {
                tableName: tableName
            }
        };
        return this.request('crossdata/tables/info', 'post', options);
    }

    executeCrossdataQuery(query: string): Observable<any> {

        const options: any = {
            body: {
                query: query
            }
        };
        return this.request('crossdata/queries', 'post', options);
    }

    getCrossdataTablesInfo(tableName: string): Observable<any> {
        const options: any = {
            body: {
                tableName: tableName
            }
        };
        return this.request('crossdata/tables/info', 'post', options);
    }

}
