/* Enum used in Rule Structures */
export enum RuleAction { ADD = "Add", UPDATE = "Update", DELETE = "Delete"}
export enum AppraisalFinalAction { Keep, Destroy }
export enum StorageFinalAction { RestrictAccess, Transfer, Copy }

/* Rule Structure */
export class RuleInformation {
  OriginRule?: string;
  Rule: string;
  StartDate?: Date;
  Action: RuleAction;
}

/* Rules category Types */
export class RuleCategory {
  PreventInheritance: boolean;
  PreventRuleIds: string[] = [];
  AllowRuleIds: string[] = [];
  Rules: RuleInformation[] = [];
}

export class AppraisalRulesCategory extends RuleCategory {
  FinalAction: AppraisalFinalAction;
}

export class StorageRulesCategory extends RuleCategory {
  FinalAction: StorageFinalAction;
}

export class ClassificationRuleCategory extends RuleCategory {
  ClassificationOwner: string;
  ClassificationLevel: string;
}

/* Global RuleCategories structure */
export class RulesInformation {
  AccessRule = new RuleCategory();
  AppraisalRule = new AppraisalRulesCategory();
  ClassificationRule = new ClassificationRuleCategory();
  DisseminationRule = new RuleCategory();
  ReuseRule = new RuleCategory();
  StorageRule = new StorageRulesCategory();
  ArchiveUnitProfile: string;
  RemoveArchiveUnitProfile: boolean;
}
