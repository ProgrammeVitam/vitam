export class Context {
  '#id' : string;
  Identifier: string;
  CreationDate : string;
  LastUpdate : string;
  ActivationDate : string;
  DeactivationDate : string;
  Name : string;
  Status : boolean;
  Description : string;
  'tenant' : string;
  EnableControl : boolean;
  Permissions : Permission[];
}

export class Permission {
  'tenant': number;
  AccessContracts: string[];
  IngestContracts: string[];
}