/***
 * Field Definition class
 *
 * Defines the properties needed for a field to be created in the search component
 */
import {SelectItem} from 'primeng/primeng';

export class FieldDefinition {
  public type = 'text';
  public totalSize: number = 3;
  public inputSize: number = 12;
  public blankSize: number = 0;
  public required = false;
  public disableOtherFields = false;
  public options: SelectItem[];

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

  static createSelectField(name: string, placeholder: string, options: SelectItem[]) {
    let newFieldDefinition = new FieldDefinition(name, placeholder);
    newFieldDefinition.type = 'select';
    newFieldDefinition.options = options;
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

  constructor(
    public name: string,
    public placeholder: string,
    totalSize?: number, inputSize?: number) {

    if (totalSize) {
      this.totalSize = totalSize;
    }
    if (inputSize) {
      this.inputSize = inputSize;
      this.blankSize = (12 - inputSize) / 2;
    }
  }

}
