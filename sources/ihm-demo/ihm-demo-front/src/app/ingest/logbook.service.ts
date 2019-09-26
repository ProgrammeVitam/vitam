import {Injectable} from '@angular/core';
import {ResourcesService} from '../common/resources.service';
import {HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import {VitamResponse} from '../common/utils/response';
import {IngestUtilsService} from '../common/utils/ingest-utils.service';
import {ArchiveUnitService} from '../archive-unit/archive-unit.service';

@Injectable()
export class LogbookService {
  LOGBOOK_API = 'logbook/operations';
  LOGBOOK_API_LAST = 'logbook/operations/last';
  LOGBOOK_LIFECYCLE_UNIT_API = 'logbookunitlifecycles';
  LOGBOOK_LIFECYCLE_OBJECT_GROUP_API = 'logbookobjectslifecycles';
  REPORT_DOWNLOAD_API = 'rules/report/download';
  REPORT_MASS_UPDATE_API = 'report/distribution/download';
  REPORT_BATCH_API = 'report/batchreport/download';
  REPORT_TRACEABILITY_DOWNLOAD_API = 'traceability';
  REFERENTIAL_CSV_DOWNLOAD = 'referential/download';

  constructor(private resourceService: ResourcesService, private ingestUtilsService: IngestUtilsService,
              public archiveUnitService: ArchiveUnitService) {
  }

  getResults(body: any, offset: number = 0, limit: number = 125): Observable<VitamResponse> {
    const headers = new HttpHeaders().set('X-Limit', `${limit}`).set('X-Offset', `${offset}`);
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

    downloadBatchReport(objectId) {
    this.resourceService.get(`${this.REPORT_BATCH_API}/${objectId}`, null, 'blob')
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

  downloadMassUpdateReport(objectId) {
    this.resourceService.get(`${this.REPORT_MASS_UPDATE_API}/${objectId}`, null, 'blob')
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

  downloadReferentialCSV(objectId, type) {
    this.resourceService.get(`${this.REFERENTIAL_CSV_DOWNLOAD}/${objectId}/${type}`, null, 'blob')
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

  getDetails(id, limit: number = 125, offset: number = 0, body: any = {}): Observable<VitamResponse> {
    const headers = new HttpHeaders()
      .set('X-Limit', `${limit}`)
      .set('X-Offset', `${offset}`);
    return this.resourceService.post(`${this.LOGBOOK_API}/${id}`, headers, body);
  }

  getLastImportAgenciesOkOrStpImportRulesOperation(body: any) {
    return this.resourceService.post(this.LOGBOOK_API_LAST, null, body);
  }


  getLifecycleLogbook(id: string, type: string): Observable<VitamResponse> {
    if (type.toUpperCase() === 'UNIT') {
      return this.resourceService.get(`${this.LOGBOOK_LIFECYCLE_UNIT_API}/${id}`, null, 'json');
    } else if (type.toUpperCase() === 'OBJECTGROUP') {
      return this.resourceService.get(`${this.LOGBOOK_LIFECYCLE_OBJECT_GROUP_API}/${id}`, null, 'json');
    }
  }

  downloadTraceabilityReport(objectId: string) {
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

  downloadTransferSIP(id) {
      this.archiveUnitService.downloadTransferSIP(id).subscribe(
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
