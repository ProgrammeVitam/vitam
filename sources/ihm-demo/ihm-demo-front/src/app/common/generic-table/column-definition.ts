import {ObjectsService} from "../utils/objects.service";

export class ColumnDefinition {
  public field: string;
  public label: string;
  public icons: string[] = [];
  public httpService: any;
  public sortable: any;
  public sortFunction: (resultToSort, event?) => any;
  public additionnalValue?: string;

  static makeStaticColumn(field: string, label: string, transform?: (value) => string, computeCss?: () => any, sortable: any = true, sortFunction: (resultToSort, event) => any = (items) => true) {
    const col = new ColumnDefinition(field, label, []);
    if (transform !== undefined) {
      col.transform = transform;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    col.sortable = sortable;
    col.sortFunction = sortFunction;
    return col;
  }


  static makeIconColumn(label: string, icons: string[], onClick: (item, httpService?, index?) => void, shouldDisplay?: (item) => boolean, computeCss?: () => any, httpService?, sortable: any = true, sortFunction: (resultToSort, event) => any = (items) => true) {
    const col = new ColumnDefinition('', label, icons);
    if (shouldDisplay !== undefined) {
      col.shouldDisplay = shouldDisplay;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }

    if (httpService !== undefined) {
      col.httpService = httpService;
      col.onClick = (item, index) => {
        onClick(item, col.httpService, index)
      };
    } else {
      col.onClick = onClick;
    }
    col.sortable = sortable;
    col.sortFunction = sortFunction;
    return col;
  }

  static makeSpecialValueColumn(label: string, getValue: (item, col?: ColumnDefinition) => string, transform?: (value) => string,
                                computeCss?: () => any, sortable: any = true, sortFunction: (resultToSort, event) => any = (items) => true, field?: string) {
    const col = new ColumnDefinition('', label, []);
    if (transform !== undefined) {
      col.transform = transform;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    col.sortable = sortable;
    col.sortFunction = sortFunction;
    if (field !== undefined) {
      col.field = field;
    }
    col.getValue = getValue;
    return col;
  }


  static makeSpecialIconColumn(label: string, getIcons: (item, icons: string[]) => string[], computeCss?: () => any, onClick?: (item, httpService?, index?) => void, httpService?, sortable: any = true, sortFunction: (resultToSort) => any = (items) => true, getLabel?: (iconType: string) => string) {
    const col = new ColumnDefinition('', label, []);
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    if (httpService !== undefined) {
      col.httpService = httpService;
      col.onClick = (item, iconType) => {
        onClick(item, col.httpService, iconType)

      };
    } else {
      col.onClick = onClick;
    }
    if (getLabel !== undefined) {
        col.getLabel = getLabel;                 
    }
    col.sortable = sortable;
    col.sortFunction = sortFunction;
    col.getIcons = getIcons;
    col.forceIcon = true;
    return col;
  }

  public computeCss: () => any = () => '';
  public onClick: (item, iconType?: string) => void = () => null;
  public getLabel: (iconType: string) => string = () => '';
  public transform: (value) => string = (x) => ObjectsService.stringify(x);
  public getValue: (item, column?: ColumnDefinition) => string = (x) => ObjectsService.stringify(x[this.field]);
  public getIcons: (item, icons: string[]) => string[] = (x, y) => y;
  public shouldDisplay: (item) => boolean = (x) => true;
  public forceIcon: boolean = false;
  public executionMode: (globalState: string, stepByStep: string) => string;


  private constructor(field: string, label: string, icons: string[]) {
    this.field = field;
    this.label = label;
    this.icons = icons;
  }
}
