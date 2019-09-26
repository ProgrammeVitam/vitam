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
  exportChoice = 'AU';
  display = false;

  comment: string;
  archivalAgreement: string;
  originatingAgencyIdentifier: string;
  submissionAgencyIdentifier: string;
  archivalAgencyIdentifier: string;

  // for transfer onl;
  relatedTransferReference: string;
  transferRequestReplyIdentifier: string;
  transferringAgency: string;

  // for DIP onl;
  messageRequestIdentifier: string;
  requesterIdentifier: string;
  authorizationRequestReplyIdentifier: string;

  //====
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
    if (this.exportChoice === 'AU') {
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
    } else if (this.exportChoice === 'INGEST') {
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

  export(type: string) {
      switch(type) {
        case 'MINIMAL':
           this.archiveUnitService.exportMinimal(this.getQuery(), this.updatedFields.DataObjectVersion).subscribe(() => this.display = true);
            break;
        case 'FULL':
            this.archiveUnitService.exportFull(this.getQuery(), this.updatedFields.DataObjectVersion, {archivalAgreement: this.archivalAgreement, originatingAgencyIdentifier: this.originatingAgencyIdentifier, comment: this.comment, submissionAgencyIdentifier: this.submissionAgencyIdentifier, archivalAgencyIdentifier: this.archivalAgencyIdentifier, messageRequestIdentifier:this.messageRequestIdentifier, requesterIdentifier: this.requesterIdentifier, authorizationRequestReplyIdentifier: this.authorizationRequestReplyIdentifier}).subscribe(() => this.display = true);
            break;
        case 'TRANSFER':
            this.archiveUnitService.transfer(this.getQuery(), this.updatedFields.DataObjectVersion, {archivalAgreement: this.archivalAgreement, originatingAgencyIdentifier: this.originatingAgencyIdentifier, comment: this.comment, submissionAgencyIdentifier: this.submissionAgencyIdentifier, archivalAgencyIdentifier: this.archivalAgencyIdentifier, relatedTransferReference: this.relatedTransferReference, transferRequestReplyIdentifier: this.transferRequestReplyIdentifier, transferringAgency: this.transferringAgency}).subscribe(() => this.display = true);
            break;
        default:
           console.log("Not found exportType", type)
      }
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
