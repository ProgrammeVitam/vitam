import { Component, OnInit, Input } from '@angular/core';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ReferentialsService } from "../../../referentials/referentials.service";
import { ResourcesService } from "../../../common/resources.service";
import { ArchiveUnitService } from "../../archive-unit.service";
import { Router } from "@angular/router";

@Component({
  selector: 'vitam-archive-object-group',
  templateUrl: './archive-object-group.component.html',
  styleUrls: ['./archive-object-group.component.css']
})
export class ArchiveObjectGroupComponent implements OnInit {
  @Input() objects;
  @Input() objectGroupId;
  @Input() unitId;
  translations;
  keyToLabel: (x) => string;
  displayObject = {};
  userContract;

  constructor(private archiveUnitHelper: ArchiveUnitHelper, private referentialsService: ReferentialsService,
              private resourceService: ResourcesService, private archiveUnitService: ArchiveUnitService,
              private router : Router) {
    this.translations = this.archiveUnitHelper.getObjectGroupTranslations();
    this.keyToLabel = (field: string) => {
      const value = this.translations[field];
      if (this.translations[field]) {
        return value;
      } else {
        return field;
      }
    };



    const contractName = this.resourceService.getAccessContract();
    const criteria = {
      ContractName: contractName
    };

    this.referentialsService.getAccessContract(criteria).subscribe(
      (response) => {
      if (response.httpCode == 200 && response.$results && response.$results.length > 0) {
        response.$results.forEach((contract) => {
          if (contract.Name == contractName) {
            this.userContract = contract;
          }
        });
      }
    }, function (error) {
      console.log('Error while get tenant. Set default list : ', error);
    });
  }

  ngOnInit() {
  }

  toogleDetails(id) {
    this.displayObject[id] = !this.displayObject[id];
  }

  downloadObject(usage, fileName) {
    const options = {
      usage: usage,
      filename: fileName
    };
    window.open(this.archiveUnitService.getObjectURL(this.unitId, options), '_blank');
  }

  isDownloadable(version) {
    return this.userContract.DataObjectVersion.indexOf(version.split('_')[0]) !== -1;
  }

  goToUnitLifecycles() {
    this.router.navigate(['search/archiveUnit/' + this.objectGroupId + '/objectgrouplifecycle']);
  }

}
