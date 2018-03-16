export class ArchiveUnitMetadata {
  '#id': string;
  '#unitType': string;
  '#object': string;
  Title: string;
  StartDate: string;
  EndDate: string;
}

export class ArchiveUnitSelection {
  archiveUnitMetadata: ArchiveUnitMetadata;
  children: ArchiveUnitSelection[];
  selected: boolean;
  displayChildren: boolean;
  isChild: boolean;
  haveChildren: boolean;

  constructor(metadata: ArchiveUnitMetadata, haveChildren: boolean) {
    this.archiveUnitMetadata = metadata;
    this.haveChildren = haveChildren;
    this.children = [];
    this.selected = false;
    this.displayChildren = false;
    this.isChild = false;
  }
}

export class BasketInfo {
  constructor(public id: string, public child: boolean) {};
}