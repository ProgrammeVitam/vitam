import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import {ArchiveUnitHelper} from "../../archive-unit/archive-unit.helper";
import {ReferentialHelper} from "../../referentials/referential.helper";
import { SelectItem } from 'primeng/primeng';


@Component({
  selector: 'vitam-metadata-field',
  templateUrl: './metadata-field.component.html',
  styleUrls: ['./metadata-field.component.css']
})
export class MetadataFieldComponent implements OnInit {
  @Input() title: string;
  @Input() originalTitle: string;
  @Input() fieldCode: string;
  @Input() value: any;
  @Input() labelSize: number;
  @Input() translate: (x) => string;
  @Input() collapse = true;
  @Input() noTitle = false;

  @Input() updateMode = false;
  @Input() canUpdate = true;
  @Input() updatedFields: {};

  typeOfField: string;
  displayMode: string;
  @Output() updatedFieldsChange = new EventEmitter<{}>();

  labelClass: string = 'ui-g-3';
  inputClass: string = 'ui-g-9';
  dateValue: Date;
  fields: any[] = [];
  options: SelectItem[];

  elementClass = 'ui-g-12';
  arrayValue: any[];

  constructor(public archiveUnitHelper: ArchiveUnitHelper, public referentialHelper : ReferentialHelper) { }

  ngOnInit() {
    if (!this.fieldCode) {
      this.fieldCode = this.originalTitle;
    }
    if (this.value instanceof Array) {
      // Handle array of values
      this.typeOfField = 'Array';
      this.arrayValue = [];
      for (var i=0, len=this.value.length; i<len; i++) {
        let item = this.value[i];
        if (typeof item === 'object') {
          let fields = [];
          for (let field in item) {
            fields.push({title: this.translate(`${this.originalTitle}.${field}`), value: item[field],
              originalTitle: `${this.originalTitle}.${field}`, fieldCode: `${this.fieldCode}[${i}].${field}`});
          }
          this.arrayValue.push({ isObject: true, value: fields });

        } else {
          this.arrayValue.push({isObject: false, value: item, fieldCode: `${this.fieldCode}[${i}]`});
        }
      }

    } else if (typeof this.value === 'object' && this.value != null) {
      this.typeOfField = 'Object';
      for (let field in this.value) {
        this.fields.push({title: this.translate(`${this.originalTitle}.${field}`), value: field,
          originalTitle: `${this.originalTitle}.${field}`, fieldCode: `${this.fieldCode}.${field}`});
      }
    } else {
      this.typeOfField = 'other';
      // Handle Date
      if (!!this.title && this.title.indexOf('Date') !== -1) {
        this.dateValue = new Date(this.value);
      }
    }

    if (this.dateValue) {
      this.displayMode = 'Date';
    } else if (this.archiveUnitHelper.isTextArea(this.originalTitle)) {
      this.displayMode = 'TextArea';
    } else if (this.archiveUnitHelper.isSelection(this.originalTitle)) {
      this.displayMode = 'DropDown';
      this.options = this.archiveUnitHelper.getOptions(this.originalTitle);
    } else if (this.referentialHelper.useSwitchButton(this.title)) {
      this.displayMode = 'SwitchButton';
    } else if (this.referentialHelper.useMultiSelect(this.title)) {
      this.options = this.referentialHelper.getOptions(this.title);
      this.displayMode = 'MultiSelect';
      this.typeOfField = 'other';
    } else if (this.referentialHelper.useChips(this.title)) {
      this.displayMode = 'Chips';
    } else {
      this.displayMode = 'TextInput';
    }


    console.log(this.title + ' display ' + this.displayMode);
    // Handle Specific field size
    if (this.noTitle) {
      this.inputClass = 'ui-g-12';
    } else if (!!this.labelSize && this.labelSize > 0 && this.labelSize < 12) {
      this.labelClass = `ui-g-${this.labelSize}`;
      this.inputClass = `ui-g-${12 - this.labelSize}`;
    }

  }

  valueChange() {
    if (this.dateValue) {
      this.updatedFields[this.fieldCode] = this.dateValue;
    } else {
      this.updatedFields[this.fieldCode] = this.value;
    }
    this.updatedFieldsChange.emit(this.updatedFields);
  }

}
