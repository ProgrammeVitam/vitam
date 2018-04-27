export class IngestContract {
  '#id': string;
  '#tenant': string;
  Name: string;
  Identifier: string;
  Description: string;
  LinkParentId: string;
  Status: string;
  CheckParentLink: string;
  isActive : boolean;
  CreationDate: string;
  MasterMandatory: boolean;
  EveryDataObjectVersion: boolean;
  DataObjectVersion: string[];
  LastUpdate: string;
  ActivationDate: string;
  DeactivationDate: string;
  ArchiveProfiles: string[];
}
