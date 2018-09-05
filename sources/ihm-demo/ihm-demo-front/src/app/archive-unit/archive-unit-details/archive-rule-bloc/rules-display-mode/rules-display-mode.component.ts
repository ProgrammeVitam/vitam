import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BaseInheritedItem, ManagementModel, PropertyModel, InheritedRule} from '../management-model';
import {ArchiveUnitHelper} from '../../../archive-unit.helper';
import {ArchiveUnitService} from '../../../archive-unit.service';

@Component({
  selector: 'vitam-rules-display-mode',
  templateUrl: './rules-display-mode.component.html',
  styleUrls: ['./rules-display-mode.component.css']
})
export class RulesDisplayModeComponent implements OnInit {
  @Input() computedData: ManagementModel;
  @Input() unitProperties: any;
  @Input() unitId: string;
  @Output() switchUpdate = new EventEmitter<any>();

  MANAGEMENT_PROPERTIES = this.archiveUnitHelper.ruleProperties;
  MANAGEMENT_RULE_CATEGORIES = this.archiveUnitHelper.rulesCategories;

  displayDetails: any = {};
  titles: any = {};

  constructor(public archiveUnitHelper: ArchiveUnitHelper, public archiveUnitService: ArchiveUnitService) { }

  ngOnInit() { }

  updateTitle(paths: string[][]) {
    const ids = new Set();
    for (const path of paths) {
      for (const id of path) {
        if (!this.titles[id]) {
          ids.add(id);
        }
      }
    }

    ids.forEach(
      id =>
        this.archiveUnitService.getDetails(id)
          .subscribe(response => {
            this.titles[id] = response.$results[0].Title;
          })
    );
  }

  toggleRuleDetails(rule: InheritedRule) {
    const uniqueRuleIdentifier = `${rule.Rule}-${rule.UnitId}`;
    this.displayDetails[uniqueRuleIdentifier] = !this.displayDetails[uniqueRuleIdentifier];
    if (this.displayDetails[uniqueRuleIdentifier]) {
      this.updateTitle(rule.Paths);
    }
  }

  togglePropertyDetails(property: PropertyModel) {
    const uniquePropertyIdentifier = `${property.PropertyName}-${property.PropertyValue}-${property.UnitId}`;
    this.displayDetails[uniquePropertyIdentifier] = !this.displayDetails[uniquePropertyIdentifier];
    if (this.displayDetails[uniquePropertyIdentifier]) {
      this.updateTitle(property.Paths);
    }
  }

  isInherited(item: BaseInheritedItem) {
    return item.UnitId === this.unitId ? 'Non' : 'Oui';
  }

  enableUpdateMode() {
    this.switchUpdate.emit();
  }
}
