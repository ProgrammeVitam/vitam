import {Component, OnInit, Input, OnChanges} from '@angular/core';
import {ArchiveUnitHelper} from '../../archive-unit.helper';
import {ArchiveUnitService} from '../../archive-unit.service';

@Component({
  selector: 'vitam-archive-extra-description',
  templateUrl: './archive-extra-description.component.html',
  styleUrls: ['./archive-extra-description.component.css']
})
export class ArchiveExtraDescriptionComponent implements OnInit, OnChanges {
  @Input() archiveUnit;
  fields: any[] = [];
  translations;
  keyToLabel: (field: string) => string;
  update = false;

  saveRunning = false;
  displayOK = false;
  displayKO = false;
  updatedFields = {};

  constructor(private archiveUnitHelper: ArchiveUnitHelper, public archiveUnitService: ArchiveUnitService) {
    this.translations = this.archiveUnitHelper.getTranslationConstants();
    this.keyToLabel = (field: string) => {
      let translateObject = this.translations;
      for (const keyPart of field.split('.')) {
        if (translateObject && translateObject[keyPart]) {
          translateObject = translateObject[keyPart];
        } else {
          translateObject = keyPart;
        }
      }
      if (typeof translateObject === 'object') {
        translateObject = translateObject['@@'];
      }
      return translateObject;
    }
  }

  ngOnChanges(change) {
    if (change.archiveUnit) {
      this.initFields();
    }
  }

  ngOnInit() {
    if (this.archiveUnit) {
      this.initFields();
    }
  }

  initFields() {
    if (!this.archiveUnit) {
      return;
    }
    this.fields = [];
    for (const field in this.archiveUnit) {
      if (this.archiveUnit.hasOwnProperty(field)) {
        if (!this.archiveUnitHelper.mustExcludeFields(field)) {
          this.fields.push({title: this.keyToLabel(field), value: field});
        }
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
    this.saveRunning = true;
    for (const field in this.updatedFields) {
      if (this.updatedFields.hasOwnProperty(field)) {
        let linkedObject = this.archiveUnit;
        let fieldName = '';
        let updatedValue: any;
        const updatedField = [];

        // This case handle array fields. For them, we need to completely copy the original array and change the updated value before update
        if (field.indexOf('[') !== -1) {
          const splittedField = field.split('.');
          let findArray = false;
          for (const fieldPart of splittedField) {
            if (!findArray) {
              if (fieldPart.indexOf('[') !== -1) {
                findArray = true;
                const fieldArray = fieldPart.split('[');
                updatedValue = JSON.parse(JSON.stringify(linkedObject[fieldArray[0]]));
                fieldName === '' ? fieldName = fieldArray[0] : fieldName += '.' + fieldArray[0];
                const arrayValue = fieldArray[1].split(']')[0];
                updatedField.push(arrayValue);
              } else {
                linkedObject = linkedObject[fieldPart];
                fieldName === '' ? fieldName = fieldPart : fieldName += '.' + fieldPart;
              }
            } else {
              if (fieldPart.indexOf('[') !== -1) {
                const fieldArray = fieldPart.split('[');
                updatedField.push(fieldArray[0]);
                updatedField.push(fieldArray[1].split(']')[0]);
              } else {
                updatedField.push(fieldPart);
              }
            }
          }
          if (findArray) {
            const sameFieldIndex: number = requestFields.findIndex((x) => {
              return x.fieldId === fieldName
            });
            if (sameFieldIndex !== -1) {
              updatedValue = requestFields[sameFieldIndex].newFieldValue;
              requestFields.splice(sameFieldIndex, 1);
            }
            let nestedFields = updatedValue;
            const len = updatedField.length;
            for (let i = 0; i < len - 1; i++) {
              const elem = updatedField[i];
              nestedFields = nestedFields[elem];
            }
            nestedFields[updatedField[len - 1]] = this.updatedFields[field];
          }

          requestFields.push({fieldId: fieldName, newFieldValue: updatedValue});
        } else {
          requestFields.push({fieldId: field, newFieldValue: this.updatedFields[field]});
        }
      }
    }
    this.archiveUnitService.updateMetadata(this.archiveUnit['#id'], requestFields)
      .subscribe(() => {
        this.archiveUnitService.getDetails(this.archiveUnit['#id'])
          .subscribe((data) => {
            this.archiveUnit = data.$results[0];
            this.initFields();
            this.updatedFields = {};
            this.update = !this.update;
            this.saveRunning = false;
            this.displayOK = true;
          }, () => {
            this.saveRunning = false;
          });
      }, () => {
        this.saveRunning = false;
        this.displayKO = true;
      });
  }

}
