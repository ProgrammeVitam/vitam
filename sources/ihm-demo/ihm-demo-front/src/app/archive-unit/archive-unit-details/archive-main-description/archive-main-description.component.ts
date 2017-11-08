import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { ArchiveUnitService } from "../../archive-unit.service";
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { Router, ActivatedRoute } from "@angular/router";

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
  saveRunning = false;
  displayOK = false;
  displayKO = false;
  id: string;

  constructor(public archiveUnitService: ArchiveUnitService, public archiveUnitHelper: ArchiveUnitHelper,
              private activatedRoute: ActivatedRoute, private router : Router) { }

  ngOnChanges(change) {
    if(change.archiveUnit) {
      this.initFields();
    }
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
    });
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
    this.saveRunning = true;
    let updateStructure = [];
    for(let fieldName in this.updatedFields) {
      updateStructure.push({fieldId: fieldName, newFieldValue: this.updatedFields[fieldName]});
    }
    this.archiveUnitService.updateMetadata(this.archiveUnit['#id'], updateStructure)
      .subscribe((data) => {
        this.archiveUnitService.getDetails(this.archiveUnit['#id'])
          .subscribe((data) => {
            this.archiveUnit = data.$results[0];
            this.initFields();
            this.update = false;
            this.saveRunning = false;
            this.displayOK = true;
          }, (error) => {
            this.saveRunning = false;
          });
      }, (error) => {
        this.saveRunning = false;
        this.displayKO = true;
      });
  }

  goToUnitLifecycles() {
    this.router.navigate(['search/archiveUnit/' + this.id + '/unitlifecycle']);
  }


}
