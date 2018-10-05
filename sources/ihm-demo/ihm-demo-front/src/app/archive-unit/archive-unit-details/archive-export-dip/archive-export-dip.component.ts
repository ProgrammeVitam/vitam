import { Component, OnInit, Input } from '@angular/core';
import { ArchiveUnitService } from '../../archive-unit.service';
import { ReferentialHelper } from '../../../referentials/referential.helper';
import { plainToClass } from 'class-transformer';
import { AccessContract } from '../../../referentials/details/access-contract/access-contract';
import { ReferentialsService } from '../../../referentials/referentials.service';
import { ErrorService } from '../../../common/error.service';
import { AccessContractService } from '../../../common/access-contract.service';

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

  contract: AccessContract;
  updatedFields: any = {};

  constructor(private archiveUnitService: ArchiveUnitService,
    private searchReferentialsService: ReferentialsService,
    private errorService: ErrorService,
    private referentialHelper: ReferentialHelper,
    private accessContractService: AccessContractService) { }

  ngOnInit() {
    this.initCurrentContract(localStorage.getItem("accessContract"));
    this.accessContractService.getUpdate().subscribe(
      (contractId: string) => {
        this.initCurrentContract(contractId);
      }
    );
  }

  getQuery() {
    let query: any;
    if (this.exportType === 'AU') {
      query = {
        "$query": [
          {
            "$eq": {
              "#id": this.id
            }
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
            }
          }
        ],
        "$projection": {}
      };
    } else {
      query = {
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
            ]
          }
        ],
        "$filter": {},
        "$projection": {}
      };
    }
    return query
  }

  exportDIP() {
    this.archiveUnitService.exportDIP(this.getQuery(), this.updatedFields.DataObjectVersion).subscribe(() => this.display = true);
  }

  initCurrentContract(accessContract: string) {
    this.searchReferentialsService.getAccessContractById(accessContract).subscribe(
      (value) => {
        this.initData(value);
      }, (error) => {
        this.errorService.handle404Error(error);
      }
    );
  }

  initData(value) {
    this.contract = plainToClass(AccessContract, value.$results)[0];
    if (this.contract.DataObjectVersion === undefined) {
      this.contract.DataObjectVersion = [];
    }

    if(this.contract.EveryDataObjectVersion) {
      this.updatedFields = ReferentialHelper.optionLists.DataObjectVersion;
    } else {
      this.updatedFields = this.contract.DataObjectVersion;
    }
  }
}
