import { Component, OnInit, OnChanges, Input, Output, EventEmitter } from '@angular/core';
import { ArchiveUnitHelper } from '../../archive-unit/archive-unit.helper';
import { ReferentialHelper } from '../../referentials/referential.helper';
import { SelectItem } from 'primeng/primeng';

@Component({
  selector: 'vitam-metadata-field',
  templateUrl: './metadata-field.component.html',
  styleUrls: ['./metadata-field.component.css']
})
export class MetadataFieldComponent implements OnInit, OnChanges {
  @Input() title: string;
  @Input() originalTitle: string;
  @Input() fieldCode: string;
  @Input() value: any;
  @Input() labelSize: number;
  @Input() keyToLabel: (x) => string = (x: string) => x;
  @Input() collapse = true;
  @Input() noTitle = false;
  @Input() yearRange = '1970:2500'; // Default value for Calendar yearRange property

  @Input() updateMode = false;
  @Input() canUpdate = true;
  @Input() disabled: boolean;
  @Input() updatedFields: {};
  // Actually applied only for Referential Helper in case of multiselect
  @Input() optionsFilter: string[] = [];

  initialValue: any;
  typeOfField: string;
  displayMode: string;
  physicalUnitMode: boolean;
  schemaJsonMode: boolean;
  @Output() updatedFieldsChange = new EventEmitter<{}>();

  frLocale = {
    dayNames: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
    dayNamesShort: ['Dim.', 'Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.'],
    dayNamesMin: ['Di', 'Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa'],
    monthNames: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Aout', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
    monthNamesShort: ['Jan', 'Fév', 'Mars', 'Avr', 'Mai', 'Juin', 'Juil', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec'],
    firstDayOfWeek: 1, today: 'Aujourd\'hui', clear: 'Vider'
  };
  labelClass = 'ui-g-3';
  inputClass = 'ui-g-9';
  dateValue: Date;
  fields: any[] = [];
  options: SelectItem[];

  elementClass = 'ui-g-12';
  arrayValue: any[];
  displayError = false;

  constructor(public archiveUnitHelper: ArchiveUnitHelper, public referentialHelper: ReferentialHelper) {
  }

  ngOnChanges(change) {
    if (change.updateMode) {
      if (change.updateMode.currentValue === false && change.updateMode.previousValue === true) {
        if (this.typeOfField === 'Object') {
          this.value = JSON.parse(this.initialValue);
        } else {
          this.value = this.initialValue;
        }
        this.init();
      }
    }

    if (change.value && this.typeOfField == 'Array') {
      this.init();
    }
    
    if(change.optionsFilter && this.updateMode && this.referentialHelper.useMultiSelect(this.originalTitle)) {
      this.options = this.referentialHelper.getOptions(this.originalTitle, this.optionsFilter);
    }
  }

  ngOnInit() {
    if (!this.fieldCode) {
      this.fieldCode = this.originalTitle;
    }

    if (this.value instanceof Array) {
      this.typeOfField = 'Array';
      this.initialValue = this.value;
    } else if (typeof this.value === 'object' && this.value != null) {
      this.typeOfField = 'Object';
      this.initialValue = JSON.stringify(this.value);
    } else {
      this.typeOfField = 'other';
      this.initialValue = this.value;
    }

    this.init();
  }

  init() {
    if (this.typeOfField === 'Array') {
      this.arrayValue = [];
      for (let i = 0, len = this.value.length; i < len; i++) {
        const item = this.value[i];
        if (typeof item === 'object') {
          const fields = [];
          for (const field in item) {
            if (item.hasOwnProperty(field)) {
              fields.push({
                title: this.keyToLabel(`${this.originalTitle}.${field}`), value: item[field],
                originalTitle: `${this.originalTitle}.${field}`, fieldCode: `${this.fieldCode}[${i}].${field}`
              });
            }
          }
          this.arrayValue.push({isObject: true, value: fields});

        } else {
          this.arrayValue.push({isObject: false, value: item, fieldCode: `${this.fieldCode}[${i}]`});
        }
      }

    } else if (this.typeOfField === 'Object') {
      for (const field in this.value) {
        if (this.value.hasOwnProperty(field)) {
          this.fields.push({
            title: this.keyToLabel(`${this.originalTitle}.${field}`), value: field,
            originalTitle: `${this.originalTitle}.${field}`, fieldCode: `${this.fieldCode}.${field}`
          });
        }
      }
    } else {
      if (!!this.originalTitle && this.originalTitle.toUpperCase().indexOf('DATE') !== -1) {
        this.dateValue = new Date(this.value);
        if (isNaN(this.dateValue.getTime())) {
          this.dateValue = null;
        }
        this.displayMode = 'Date';
      }
    }

    if (!this.displayMode) {
      this.schemaJsonMode = this.archiveUnitHelper.isSchemaJsonMode(this.originalTitle);
      if (this.archiveUnitHelper.isTextArea(this.originalTitle)) {
        this.displayMode = 'TextArea';
      } else if (this.archiveUnitHelper.isSelection(this.originalTitle)) {
        this.displayMode = 'DropDown';
        this.options = this.archiveUnitHelper.getOptions(this.originalTitle);
      } else if (this.referentialHelper.useSwitchButton(this.originalTitle)) {
        this.displayMode = 'SwitchButton';
      } else if (this.referentialHelper.useMultiSelect(this.originalTitle)) {
        this.options = this.referentialHelper.getOptions(this.originalTitle);
        this.displayMode = 'MultiSelect';
        this.typeOfField = 'other';
      } else if (this.referentialHelper.useChips(this.originalTitle)) {
        this.displayMode = 'Chips';
        this.typeOfField = 'other';
      } else {
        this.displayMode = 'TextInput';
      }
    }

    if (this.originalTitle !== undefined && this.originalTitle.indexOf('PhysicalDimensions') !== -1
      && this.originalTitle.indexOf('unit') !== -1) {
      this.physicalUnitMode = true;
    }

    // Handle Specific field size
    if (this.noTitle) {
      this.inputClass = 'ui-g-12';
    } else if (!!this.labelSize && this.labelSize > 0 && this.labelSize < 12) {
      this.labelClass = `ui-g-${this.labelSize}`;
      this.inputClass = `ui-g-${12 - this.labelSize}`;
    }
  }

  checkDateValid() {
    setTimeout(() => {
      if (this.dateValue === null) {
        this.displayError = true;
        this.dateValue = new Date(this.value);
        if (isNaN(this.dateValue.getTime())) {
          this.dateValue = null;
        }
      }
    }, 200);
  }

  valueChange() {
    if (this.displayMode === 'Date' && !this.dateValue) {
      this.dateValue = new Date(this.value);
      if (isNaN(this.dateValue.getTime())) {
        this.dateValue = null;
      }
      return;
    }
    if (this.dateValue) {
      this.updatedFields[this.fieldCode] = this.dateValue;
    } else {
      this.updatedFields[this.fieldCode] = this.value;
    }
    this.updatedFieldsChange.emit(this.updatedFields);
  }

  getLabelPhysicalDimensions(value: string) {
    const labels = [];
    labels.push(this.archiveUnitHelper.getDimensions(value));
    return labels;
  }

  getLabels(values: string | string[]) {
    const labels = [];
    if (values instanceof Array) {
      values.forEach(value => {
        labels.push(this.getLabel(value));
      });
    } else {
      labels.push(this.getLabel(values));
    }
    return labels.join(', ');
  }

  getLabel(value: string) {
    const matchingOption = this.options.find(option => option.value === value);
    if (!matchingOption) {
      return value;
    }
    return matchingOption.label;
  }
}
