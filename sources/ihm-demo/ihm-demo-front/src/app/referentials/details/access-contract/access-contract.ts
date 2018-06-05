export class AccessContract {
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
  DataObjectVersion: string[];
  OriginatingAgencies: string[];
  WritingPermission: boolean;
  WritingRestrictedDesc: boolean;
  EveryOriginatingAgency: boolean;
  EveryDataObjectVersion: boolean;
  RootUnits: string[];
  ExcludedRootUnits: string[];
}
