import {Component, OnInit, OnChanges, Input, Output, EventEmitter} from '@angular/core';
import { ArchiveUnitService } from "../../archive-unit.service";
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { Router, ActivatedRoute } from "@angular/router";
import {DateService} from "../../../common/utils/date.service";

@Component({
  selector: 'vitam-archive-main-description',
  templateUrl: './archive-main-description.component.html',
  styleUrls: ['./archive-main-description.component.css']
})
export class ArchiveMainDescriptionComponent implements OnInit, OnChanges {
  @Input() archiveUnit;
  @Output() titleUpdate = new EventEmitter<string>();
  dataToDisplay: any;
  keyToLabel = (x) => x;
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
    let newTitle;
    let updateStructure = [];
    for(let fieldName in this.updatedFields) {
      if (fieldName === 'Title') {
        newTitle = this.updatedFields[fieldName];
      }

      //In case of EndDate field, we must adjust date picked by user to be set to the latest minute of the given day
      //to insure successful compare with StartDate with same day as value
      
      if(fieldName === 'EndDate'){
          let adjustedEndUTCDate = DateService.transformUTCDate(this.updatedFields[fieldName], 23, 59, 59, 999);
          updateStructure.push({fieldId: fieldName, newFieldValue: adjustedEndUTCDate});
      } else {
        updateStructure.push({fieldId: fieldName, newFieldValue: this.updatedFields[fieldName]});
      }
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
            if (newTitle) {
              this.titleUpdate.emit(newTitle)
            }
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
