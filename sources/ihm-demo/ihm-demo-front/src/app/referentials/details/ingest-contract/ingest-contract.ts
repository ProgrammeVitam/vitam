export class IngestContract {
  '#id': string;
  '#tenant': string;
  Name: string;
  Identifier: string;
  Description: string;
  LinkParentId: string;
  Status: string;
  CheckParentLink: string;
  CheckParentId: string[];
  isActive : boolean;
  CreationDate: string;
  MasterMandatory: boolean;
  EveryDataObjectVersion: boolean;
  FormatUnidentifiedAuthorized: boolean;
  EveryFormatType: boolean;
  FormatType: string[];
  DataObjectVersion: string[];
  LastUpdate: string;
  ActivationDate: string;
  DeactivationDate: string;
  ArchiveProfiles: string[];
  ManagementContractId: string;
}
