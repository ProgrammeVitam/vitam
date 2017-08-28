import {Component, OnInit, Input, OnChanges} from '@angular/core';
import {ArchiveUnitHelper} from "../../archive-unit.helper";
import {ArchiveUnitService} from "../../archive-unit.service";

@Component({
  selector: 'vitam-archive-extra-description',
  templateUrl: './archive-extra-description.component.html',
  styleUrls: ['./archive-extra-description.component.css']
})
export class ArchiveExtraDescriptionComponent implements OnInit, OnChanges {
  @Input() archiveUnit;
  fields: any[] = [];
  translations;
  translate: (field: string) => string;
  update = false;

  updatedFields = {};

  constructor(private archiveUnitHelper: ArchiveUnitHelper, public archiveUnitService: ArchiveUnitService) {
    this.translations = this.archiveUnitHelper.getTranslationConstants();
    this.translate = (field: string) => {
      const value = this.translations[field];
      if (this.translations[field]) {
        return value;
      } else {
        return field;
      }
    }
  }

  ngOnChanges(change) {
    if(change.archiveUnit) {
      this.initFields();
    }
  }

  ngOnInit() {
    if (this.archiveUnit) {
      this.initFields();
    }
  }

  initFields() {
    if(!this.archiveUnit) {
      return;
    }
    this.fields = [];
    for (let field in this.archiveUnit) {
      if (!this.archiveUnitHelper.mustExcludeFields(field)) {
        this.fields.push({title: this.translate(field), value: field});
      }
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
    const requestFields = [];
    for (let field in this.updatedFields) {
      let linkedObject = this.archiveUnit;
      let fieldName = '';
      let updatedValue: any;
      let updatedField = [];

      // This case handle array fields. For them, we need to completely copy the original array and change the updated value before update
      if (field.indexOf('[') !== -1) {
        let splittedField = field.split('.');
        let findArray = false;
        for(let fieldPart of splittedField) {
          if (!findArray) {
            if (fieldPart.indexOf('[') !== -1) {
              findArray = true;
              let fieldArray = fieldPart.split('[');
              updatedValue = JSON.parse(JSON.stringify(linkedObject[fieldArray[0]]));
              fieldName === '' ? fieldName = fieldArray[0] : fieldName += fieldArray[0];
              let arrayValue = fieldArray[1].split(']')[0];
              updatedField.push(arrayValue);
            } else {
              linkedObject = linkedObject[fieldPart];
              fieldName === '' ? fieldName = fieldPart : fieldName += fieldPart;
            }
          } else {
            if (fieldPart.indexOf('[') !== -1) {
              let fieldArray = fieldPart.split('[');
              updatedField.push(fieldArray[0]);
              updatedField.push(fieldArray[1].split(']')[0]);
            } else {
              updatedField.push(fieldPart);
            }
          }
        }
        if (findArray) {
          let sameFieldIndex: number = requestFields.findIndex((x) => {return x.fieldId === fieldName});
          if(sameFieldIndex !== -1) {
            updatedValue = requestFields[sameFieldIndex].newFieldValue;
            requestFields.splice(sameFieldIndex, 1);
          }
          var nestedFields = updatedValue;
          let len = updatedField.length;
          for (let i = 0; i < len-1; i++) {
            var elem = updatedField[i];
            nestedFields = nestedFields[elem];
          }
          nestedFields[updatedField[len-1]] = this.updatedFields[field];
        }

        requestFields.push({fieldId: fieldName, newFieldValue: updatedValue});
      } else {
        requestFields.push({fieldId: field, newFieldValue: this.updatedFields[field]});
      }
    }
    this.archiveUnitService.updateMetadata(this.archiveUnit['#id'], requestFields)
     .subscribe((data) => {
     return data;
     });
  }

}
