import { Component, OnInit, Input } from '@angular/core';
import {FormGroup, FormControl} from "@angular/forms";
import {Preresult} from "../../../../common/search/preresult";
import {ArchiveUnitService} from "../../../archive-unit.service";

@Component({
  selector: 'vitam-tree-search',
  templateUrl: './tree-search.component.html',
  styleUrls: ['./tree-search.component.css']
})
export class TreeSearchComponent implements OnInit {
  @Input() id: string;
  searchForm: FormGroup;

  constructor(public archiveUnitService: ArchiveUnitService) { }

  ngOnInit() {
    this.searchForm = new FormGroup({
      titleAndDescription: new FormControl(''),
      startDate: new FormControl(''),
      endDate: new FormControl('')
    })
  }

  public onSubmit() {

    let request: any = {};
    if (this.searchForm.valid) {
      Object.keys(this.searchForm.controls).forEach(key => {
        request[key] = this.searchForm.controls[key].value;
      });
    } else {
    }

    let preResult = new Preresult();
    preResult.searchProcessSkip = false;

    let criteriaSearch = {
      // TODO Add Id in node.data
      SELECT_BY_ID: this.id,
      /*StartDate: request.startDate,
       EndDate: request.endDate,*/
      titleAndDescription: request.titleAndDescription,
      isAdvancedSearchFlag: "No",
      projection_title: 'Title',
      projection_id: '#id',
      projection_unitups: '#unitups',
      projection_unitType: '#unittype'
    };

    this.archiveUnitService.getResults(criteriaSearch).subscribe(
        (response) => {
          //responseEvent.emit({response: response, form: criteriaSearch});
        },
        (error) => console.log('Error: ', error)
    );
  }

  clearFields() {
    this.searchForm.reset();
    this.searchForm.enable();
  }

}
