export enum MetadataAction { ADD = "Add", UPDATE = "Update", DELETE = "Delete"}

export class MetadataInformation {
  FieldName: string;
  FieldValue?: string;
  Action: MetadataAction;
}
