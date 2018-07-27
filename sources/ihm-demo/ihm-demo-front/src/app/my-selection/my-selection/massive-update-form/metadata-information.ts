export enum MetadataAction { PATTERN = "Pattern", UPDATE = "Update", DELETE = "Delete"}

export class MetadataInformation {
  FieldName: string;
  FieldValue?: string;
  FieldPattern?: string;
  Action: MetadataAction;
}
