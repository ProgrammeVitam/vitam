export enum ActionTypePreservation {
  GENERATE,
  IDENTIFY,
  ANALYSE,
  EXTRACT
}

export class ValuesPreservation {
  Extension: string;
  Args: string[];
  FilteredExtractedObjectGroupData: string[];
  FilteredExtractedUnitData: string[];
}

export class ActionPreservation {

  Type: ActionTypePreservation;
  Values: ValuesPreservation;
}

export class GriffinByFormat {

  FormatList: string [];
  GriffinIdentifier: string;
  Timeout: string;
  MaxSize: string;
  Debug: boolean;
  ActionDetail: ActionPreservation[];
}

export class Scenario {
  '#id': string;
  Identifier: string;
  Name: string;
  Description: string;
  CreationDate: string;
  LastUpdate: string;
  GriffinByFormat: GriffinByFormat;
  ActionList: ActionTypePreservation;
  DefaultGriffin: GriffinByFormat;
}
