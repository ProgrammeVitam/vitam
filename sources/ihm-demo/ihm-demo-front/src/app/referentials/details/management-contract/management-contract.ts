export class ManagementContractStorage {
  UnitStrategy: string;
  ObjectGroupStrategy: string;
  ObjectStrategy: string;
}

export class ManagementContract {
  '#id': string;
  '#tenant': string;
  Name: string;
  Identifier: string;
  Description: string;
  Status: string;
  CreationDate: string;
  LastUpdate: string;
  ActivationDate: string;
  DeactivationDate: string;
  Storage: ManagementContractStorage;
}
