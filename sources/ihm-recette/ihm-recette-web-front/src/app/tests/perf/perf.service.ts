import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/toPromise';
import { saveAs } from 'file-saver/FileSaver';

import { ResourcesService } from '../../common/resources.service';

@Injectable()
export class PerfService {
  GENERATE_STAT_URL = 'performances/reports';
  SIP_TO_UPLOAD_URL = 'performances/sips';
  SIP_TO_UPLOAD = 'performances';
  REPORT =   'performances/reports/';

  constructor(private client: ResourcesService) { }

  //Get Available SIP on server for upload
  getAvailableSipForUpload() {
    return this.client.get(this.SIP_TO_UPLOAD_URL);
  }

  // upload selected
  uploadSelected(data) {
    return this.client.post(this.SIP_TO_UPLOAD, undefined, data);
  }

  // Generate INGEST statistics report
  generateIngestStatReport() {
    return this.client.get(this.GENERATE_STAT_URL);
  }

  downloadURL(fileName) {
    return this.client.get(this.REPORT + fileName).toPromise()
      .then(response => this.saveToFileSystem(response, fileName));
  }

  saveToFileSystem(response, fileName : string) {
    const blob = new Blob([response._body], { type: 'text/plain' });
    saveAs(blob, fileName);
  }
}
