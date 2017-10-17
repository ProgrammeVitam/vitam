import { Injectable } from '@angular/core';
import { IngestService } from '../../ingest/ingest.service';

@Injectable()
export class IngestUtilsService {

  constructor(private ingestService: IngestService) { }

  // TODO : Download object function
  downloadObject(objectId, type) {
    this.ingestService.getObject(objectId, type)
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

}
