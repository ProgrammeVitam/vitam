export class AccessionRegister {
  '#id' : string;
  OriginatingAgency: string;
  CreationDate : string;
  '#tenant' : string;
  ObjectSize : Register;
  TotalObjectGroups : Register;
  TotalObjects : Register;
  TotalUnits : Register;
}

class Register {
  attached : number;
  deleted : number;
  detached : number;
  ingested : number;
  remained : number;
  symbolicRemained : number
}

export class RegisterData {
  ObjectSize : string;
  TotalObjectGroups : string;
  TotalObjects : string;
  TotalUnits : string;
}

export class AccessionRegisterDetail {
  '#id' : string;
 '#tenant' : number;
  ArchivalAgreement : string;
  EndDate : string;
  LastUpdate ; string;
  ObjectSize : Register;
  OperationIds : string[];
  OriginatingAgency : string;
  StartDate : string ;
  Status : string;
  SubmissionAgency : string;
  Symbolic : string;
  TotalObjectGroups : Register;
  TotalObjects : Register;
  TotalUnits : Register;
  objectSize : Register;
}
