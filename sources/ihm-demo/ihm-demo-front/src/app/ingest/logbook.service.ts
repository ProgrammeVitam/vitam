import { Injectable } from '@angular/core';
import { ResourcesService } from "../common/resources.service";
import { HttpHeaders } from '@angular/common/http';
import { Observable } from "rxjs/Observable";
import 'rxjs/add/observable/of';
import { VitamResponse} from "../common/utils/response";
import { IngestUtilsService } from "../common/utils/ingest-utils.service";
import { ArchiveUnitService } from '../archive-unit/archive-unit.service';

@Injectable()
export class LogbookService {
  LOGBOOK_API = 'logbook/operations';
  REPORT_DOWNLOAD_API = 'rules/report/download';
  REPORT_TRACEABILITY_DOWNLOAD_API = 'traceability';

  constructor(private resourceService: ResourcesService, private ingestUtilsService: IngestUtilsService,
              public archiveUnitService: ArchiveUnitService) { }

  getResults(body: any, offset: number = 0): Observable<VitamResponse> {

    const headers = new HttpHeaders().set('X-Limit', '125').set('X-Offset', '' + offset);

    return this.resourceService.post(this.LOGBOOK_API, headers, body);
  }

  downloadReport(objectId) {
    this.resourceService.get(`${this.REPORT_DOWNLOAD_API}/${objectId}`, null, 'blob')
        .subscribe(
            (response) => {
              const a = document.createElement('a');
              document.body.appendChild(a);

              a.href = URL.createObjectURL(response.body);

              if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
                a.download = response.headers.get('content-disposition').split('filename=')[1];
                a.click();
              }
            }
        );
  }

  downloadObject(objectId: string) {
      this.ingestUtilsService.downloadObject(objectId, 'archivetransferreply');
  }

  getDetails(id): Observable<VitamResponse> {
    return this.resourceService.post(`${this.LOGBOOK_API}/${id}`, null, {});
  }

  downloadTraceabilityReport(objectId : string) {
    let tenant = this.resourceService.getTenant();
    let contractName = this.resourceService.getAccessContract();
    this.resourceService.get(`${this.REPORT_TRACEABILITY_DOWNLOAD_API}/${objectId}/content?contractId=${contractName}&tenantId=${tenant}`, null, 'blob')
      .subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(response.body);

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
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(response.body);

        if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
          a.download = response.headers.get('content-disposition').split('filename=')[1].replace(/"/gi, '');
          a.click();
        }
      }
    );
  }
}
