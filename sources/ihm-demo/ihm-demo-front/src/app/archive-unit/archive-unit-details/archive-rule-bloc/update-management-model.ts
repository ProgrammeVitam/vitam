export class StorageProperties {
  FinalAction?: string;
}

export class AppraisalProperties {
  FinalAction?: string;
}

export class ClassificationProperties {
  ClassificationOwner?: string;
  ClassificationLevel?: string;
  ClassificationAudience?: string;
  ClassificationReassessingDate?: Date;
  NeedReassessingAuthorization?: boolean;
}

export class UpdatePropertiesModel {
  AppraisalRule: AppraisalProperties = {};
  ClassificationRule: ClassificationProperties = {ClassificationReassessingDate: null};
  StorageRule: StorageProperties = {};
}

export class UpdateInfo {
  updated: number;
  added: number;
  deleted: number;
  categories: string[];
  rules: any[];
  ArchiveUnitProfile?: string;
  NeedAuthorization?: boolean;
}