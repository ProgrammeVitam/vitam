/***
 * Field Definition class
 *
 * Defines the properties needed for a field to be created in the search component
 */
import {SelectItem} from 'primeng/primeng';
import {FormGroup} from "@angular/forms";

export class DynamicSelectItem {
  label: string;
  value: any;
  data: any;
  styleClass?: string;

  constructor(label: string, value: string, data: string) {
    this.label = label;
    this.value = value;
    this.data = data;
  }

  toSelectItem(): SelectItem {
    if (this.styleClass) {
      return {label: this.label, value: this.value, styleClass: this.styleClass};
    }
    return {label: this.label, value: this.value};
  }

  static toSelectItems(items: DynamicSelectItem[]): SelectItem[] {
    if (items && items.length > 0) {
      return items.map((x: DynamicSelectItem) => x.toSelectItem());
    }
    return [];
  }
}

export class FieldDefinition {
  public label = '';
  public type = 'text';
  public totalSize: number = 3;
  public inputSize: number = 12;
  public blankSize: number = 0;
  public required = false;
  public disableOtherFields = false;

  public options: SelectItem[];
  public baseOptions: DynamicSelectItem[];
  public computeSelectItems: (x: DynamicSelectItem[], y: any) => SelectItem[] = DynamicSelectItem.toSelectItems;
  public updateFunction: (data: any, form: FormGroup) => void = (x, y) => {};

  public value : string;

  static createIdField(name: string, placeholder: string, totalSize?: number, inputSize?: number) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.disableOtherFields = true;
    if (totalSize) {
      newFieldDefinition.totalSize = totalSize;
    }
    if (inputSize) {
      newFieldDefinition.inputSize = inputSize;
      newFieldDefinition.blankSize = (12 - inputSize) / 2;
    }
    return newFieldDefinition;
  }

  static createDateField(name: string, placeholder: string, totalSize?: number, inputSize?: number) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.type = 'date';
    if (totalSize) {
      newFieldDefinition.totalSize = totalSize;
    }
    if (inputSize) {
      newFieldDefinition.inputSize = inputSize;
      newFieldDefinition.blankSize = (12 - inputSize) / 2;
    }
    return newFieldDefinition;
  }

  static createSelectField(name: string, label: string, placeholder: string, options: SelectItem[], totalSize?: number, inputSize?: number) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.label = label;
    newFieldDefinition.type = 'select';
    newFieldDefinition.options = options;
    if (totalSize) {
      newFieldDefinition.totalSize = totalSize;
    }
    if (inputSize) {
      newFieldDefinition.inputSize = inputSize;
      newFieldDefinition.blankSize = (12 - inputSize) / 2;
    }
    return newFieldDefinition;
  }

  static createSelectMultipleField(name: string, placeholder: string, options: SelectItem[], totalSize?: number, inputSize?: number) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.type = 'selectMultiple';
    newFieldDefinition.options = options;
    if (totalSize) {
      newFieldDefinition.totalSize = totalSize;
    }
    if (inputSize) {
      newFieldDefinition.inputSize = inputSize;
      newFieldDefinition.blankSize = (12 - inputSize) / 2;
    }
    return newFieldDefinition;
  }

  static createDynamicSelectField(name: string, placeholder: string, baseOptions: DynamicSelectItem[],
                                  computeSelectItems: (x: DynamicSelectItem[], y: any) => SelectItem[] = DynamicSelectItem.toSelectItems,
                                  totalSize = 3, inputSize = 12) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.type = 'dynamicSelect';
    newFieldDefinition.baseOptions = baseOptions;
    newFieldDefinition.computeSelectItems = computeSelectItems;
    newFieldDefinition.options = computeSelectItems(baseOptions, '');

    if (totalSize) {
      newFieldDefinition.totalSize = totalSize;
    }
    if (inputSize) {
      newFieldDefinition.inputSize = inputSize;
      newFieldDefinition.blankSize = (12 - inputSize) / 2;
    }
    return newFieldDefinition;
  }

  constructor(
    public name: string,
    public placeholder: string,
    totalSize?: number, inputSize?: number, value? : string) {

    if (totalSize) {
      this.totalSize = totalSize;
    }
    if (inputSize) {
      this.inputSize = inputSize;
      this.blankSize = (12 - inputSize) / 2;
    }
    if (value) {
      this.value = value;
    }
  }

  setValue(value : string) {
    this.value = value;
  }

  onChange(updateFunction: (data: any, form: FormGroup) => void) {
    this.updateFunction = updateFunction;
    return this;
  }

}
