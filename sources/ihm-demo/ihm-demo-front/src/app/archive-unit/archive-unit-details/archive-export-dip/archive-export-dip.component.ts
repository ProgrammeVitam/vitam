import { Component, OnInit, Input } from '@angular/core';
import { SelectItem, ConfirmationService } from 'primeng/primeng';

import { ArchiveUnitService } from '../../archive-unit.service';

@Component({
  selector: 'vitam-archive-export-dip',
  templateUrl: './archive-export-dip.component.html',
  styleUrls: ['./archive-export-dip.component.css']
})
export class ArchiveExportDIPComponent implements OnInit {
  @Input() id: string = '';
  @Input() operation: string = '';
  exportType = 'AU';
  display = false;

  constructor(private archiveUnitService: ArchiveUnitService) { }

  ngOnInit() {
  }

  getQuery() {
    let query: any;
    if (this.exportType === 'AU') {
      query = {
        "$roots": [],
        "$query": [
          {
            "$eq": {
              "#id": this.id
            },
            "$depth": 0
          }
        ],
        "$filter": {},
        "$projection": {}
      };
    } else if (this.exportType === 'INGEST') {
      query = {
        "$query": [
          {
            "$eq": {
              "#operations": this.operation
            },
            "$depth": 20
          }
        ],
        "$projection": {}
      };
    } else {
      query = {
        "$roots": [],
        "$query": [
          {
            "$or": [
              {
                "$eq": {
                  "#id": this.id
                }
              },
              {
                "$in": {
                  "#allunitups": [
                    this.id
                  ]
                }
              }
            ],
            "$depth": 0
          }
        ],
        "$filter": {},
        "$projection": {}
      };
    }
    return query;
  }

  exportDIP() {
    this.archiveUnitService.exportDIP(this.getQuery()).subscribe(
        (response) => {
          this.display = true;
        }
    );
  }
}
