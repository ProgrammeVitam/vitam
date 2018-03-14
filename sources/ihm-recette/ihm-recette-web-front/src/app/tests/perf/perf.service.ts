import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/toPromise';
import { saveAs } from 'file-saver/FileSaver';

import { ResourcesService } from '../../common/resources.service';
import { HttpHeaders } from "@angular/common/http";

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
    let header = new HttpHeaders().set('Accept', 'text/plain');
    this.client.get(this.REPORT + fileName, header, 'blob').subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(response.body);

        a.download = fileName;
        a.click();
      }
    );
  }
}
