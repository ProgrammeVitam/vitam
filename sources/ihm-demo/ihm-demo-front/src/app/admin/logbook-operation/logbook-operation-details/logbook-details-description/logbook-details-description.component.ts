import { Component, OnInit, Input } from '@angular/core';
import { Logbook } from "../../../../common/utils/logbook";
import { plainToClass } from 'class-transformer';
import { LogbookService } from "../../../../ingest/logbook.service";

@Component({
  selector: 'vitam-logbook-details-description',
  templateUrl: './logbook-details-description.component.html',
  styleUrls: ['./logbook-details-description.component.css']
})
export class LogbookDetailsDescriptionComponent implements OnInit {
  results: Logbook[];
  @Input() operationId: string;
  @Input() isIngestOperation: boolean;

  public parse = JSON.parse;

  constructor(private logbookService: LogbookService) { }

  ngOnInit() {
    this.logbookService.getDetails(this.operationId).subscribe(
        (data) => {
          this.results = [plainToClass<Logbook, Object>(Logbook, data.$results[0])];
        }
    );
  }

}
