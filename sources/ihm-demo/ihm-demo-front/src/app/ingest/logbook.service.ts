import { Injectable } from '@angular/core';
import {ResourcesService} from "../common/resources.service";
import {Headers, Response } from "@angular/http";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/observable/of';
import {VitamResponse} from "../common/utils/response";
import {ArchiveUnitService} from '../archive-unit/archive-unit.service';

@Injectable()
export class LogbookService {
  LOGBOOK_API = 'logbook/operations';
  REPORT_DOWNLOAD_API = 'rules/report/download';
  REPORT_TRACEABILITY_DOWNLOAD_API = 'traceability';


  constructor(private resourceService: ResourcesService, public archiveUnitService: ArchiveUnitService) { }

  getResults(body: any, offset: number = 0): Observable<VitamResponse> {

    const headers = new Headers();
    headers.append('X-Limit', '125');
    headers.append('X-Offset', '' + offset);

    return this.resourceService.post(this.LOGBOOK_API, headers, body)
        .map((res: Response) => res.json());
  }

  downloadReport(objectId) {
    this.resourceService.get(`${this.REPORT_DOWNLOAD_API}/${objectId}`)
        .subscribe(
            (response) => {
              const a = document.createElement('a');
              document.body.appendChild(a);

              a.href = URL.createObjectURL(new Blob([response.text()], {type: 'application/xml'}));

              if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
                a.download = response.headers.get('content-disposition').split('filename=')[1];
                a.click();
              }
            }
        );
  }

  getDetails(id): Observable<VitamResponse> {
    return this.resourceService.post(`${this.LOGBOOK_API}/${id}`, null, {})
        .map((res: Response) => res.json());
  }

  downloadTraceabilityReport(objectId : string) {
    let tenant = this.resourceService.getTenant();
    let contractName = this.resourceService.getAccessContract();
    this.resourceService.get(`${this.REPORT_TRACEABILITY_DOWNLOAD_API}/${objectId}/content?contractId=${contractName}&tenantId=${tenant}`)
      .subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(new Blob([response.text()], {type: 'application/zip'}));

        if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
          a.download = response.headers.get('content-disposition').split('filename=')[1];
          a.click();
        }
      }
    );
  }

  downloadDIP(id) {
    this.archiveUnitService.downloadDIP(id).subscribe(
      (response) => {
        console.log(response);
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(new Blob([response['_body']], {type: 'application/zip'}));

        if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
          a.download = response.headers.get('content-disposition').split('filename=')[1];
          a.click();
        }
      }
    );
  }
}
