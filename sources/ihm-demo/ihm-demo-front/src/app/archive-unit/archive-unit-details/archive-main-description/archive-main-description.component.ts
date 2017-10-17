import { Component, OnInit, OnChanges, Input } from '@angular/core';
import {ArchiveUnitService} from "../../archive-unit.service";
import {ArchiveUnitHelper} from "../../archive-unit.helper";

@Component({
  selector: 'vitam-archive-main-description',
  templateUrl: './archive-main-description.component.html',
  styleUrls: ['./archive-main-description.component.css']
})
export class ArchiveMainDescriptionComponent implements OnInit, OnChanges {
  @Input() archiveUnit;
  dataToDisplay: any;
  translate = (x) => x;
  update = false;
  updatedFields = {};

  constructor(public archiveUnitService: ArchiveUnitService, public archiveUnitHelper: ArchiveUnitHelper) { }

  ngOnChanges(change) {
    if(change.archiveUnit) {
      this.initFields();
    }
  }

  ngOnInit() {
    this.initFields();
  }

  initFields() {
    if (this.archiveUnit) {
      this.dataToDisplay = Object.assign({}, this.archiveUnit);
    }
  }

  switchUpdateMode() {
    this.update = !this.update;
    if (!this.update) {
      this.initFields();
      this.updatedFields = {};
    }
  }

  saveUpdate() {
    let updateStructure = [];
    for(let fieldName in this.updatedFields) {
      updateStructure.push({fieldId: fieldName, newFieldValue: this.updatedFields[fieldName]});
    }
    this.archiveUnitService.updateMetadata(this.archiveUnit['#id'], updateStructure)
      .subscribe((data) => {
        return data;
      });
  }


}
