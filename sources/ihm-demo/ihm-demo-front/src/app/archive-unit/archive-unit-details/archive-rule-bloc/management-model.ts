export class BaseInheritedItem {
  UnitId: string;
  OriginatingAgency: string;
  Paths: string[][];
}

export class InheritedRule extends BaseInheritedItem {
  Rule: string;
  StartDate: string;
  EndDate: string;
  newRule?: boolean;
  editionStartDate?: Date;
}

export class PropertyModel extends BaseInheritedItem {
  PropertyName: string;
  PropertyValue: string;
}

export class InheritanceProperties {
  PreventRulesId?: string[];
  PreventInheritance?: boolean;
}

export class RuleCategory {
  Rules: InheritedRule[];
  Properties: PropertyModel[];
  Inheritance?: InheritanceProperties;
}

export class ManagementModel {
  AccessRule?: RuleCategory;
  AppraisalRule?: RuleCategory;
  ClassificationRule?: RuleCategory;
  DisseminationRule?: RuleCategory;
  ReuseRule?: RuleCategory;
  StorageRule?: RuleCategory;
  ArchiveUnitProfile?: string;
  NeedAuthorization?: boolean;
}
