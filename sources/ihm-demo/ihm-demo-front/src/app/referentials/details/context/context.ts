export class Context {
  _id : string;
  Identifier: string;
  CreationDate : string;
  LastUpdate : string;
  ActivationDate : string;
  DeactivationDate : string;
  Name : string;
  Status : boolean;
  Description : string;
  _tenant : string;
  EnableControl : boolean;
  Permissions : any[];
}