import {Component, Input, OnInit} from '@angular/core';
import {MySelectionService} from "../../../my-selection/my-selection.service";
import {ResourcesService} from "../../../common/resources.service";

@Component({
  selector: 'vitam-archive-unit-selection',
  templateUrl: './archive-unit-selection.component.html',
  styleUrls: ['./archive-unit-selection.component.css']
})
export class ArchiveUnitSelectionComponent implements OnInit {
  @Input() id = '';
  @Input() operation = '';
  exportChoice = 'AU';
  display = false;
  displayError = false;

  constructor(private mySelectionService: MySelectionService, private resourcesService: ResourcesService) { }

  ngOnInit() { }

  addToBasket() {
    const isIngest: boolean = this.exportChoice === 'INGEST';
    this.mySelectionService.getIdsToSelect(isIngest, isIngest ? this.operation : this.id).subscribe(
      (response) => {
        const ids: string[] = response.$results.reduce(
          (x, y) => {
            x.push(y['#id']);
            return x;
          }, []);
        this.mySelectionService.addToSelection(this.exportChoice === 'FULL', ids, this.resourcesService.getTenant());
        this.display = true;
      }, () => {
        this.displayError = true;
      }
    );
  }
}
