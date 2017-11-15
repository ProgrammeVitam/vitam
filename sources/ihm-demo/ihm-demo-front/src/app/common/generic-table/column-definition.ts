
export class ColumnDefinition {
  public field: string;
  public label: string;
  public icons: string[] = [];
  public httpService: any;

  static makeStaticColumn(field: string, label: string, transform?: (value) => string, computeCss?: () => any) {
    const col = new ColumnDefinition(field, label, []);
    if (transform !== undefined) {
      col.transform = transform;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    return col;
  }

  static makeIconColumn(label: string, icons: string[], onClick: (item, httpService?, index?) => void, shouldDisplay?: (item) => boolean, computeCss?: () => any, httpService?) {
    const col = new ColumnDefinition('', label, icons);
    if (shouldDisplay !== undefined) {
      col.shouldDisplay = shouldDisplay;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }

    if (httpService !== undefined) {
      col.httpService = httpService;
      col.onClick = (item, index) => { onClick(item, col.httpService, index) };
    } else {
      col.onClick = onClick;
    }

    return col;
  }

  static makeSpecialValueColumn(label: string, getValue: (item) => string, transform?: (value) => string, computeCss?: () => any) {
    const col = new ColumnDefinition('', label, []);
    if (transform !== undefined) {
      col.transform = transform;
    }
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    col.getValue = getValue;
    return col;
  }

  static makeSpecialIconColumn(label: string, getIcons: (item, icons: string[]) => string[], computeCss?: () => any, onClick?: (item, httpService?, index?) => void, httpService?) {
    const col = new ColumnDefinition('', label, []);
    if (computeCss !== undefined) {
      col.computeCss = computeCss;
    }
    if (httpService !== undefined) {
      col.httpService = httpService;
      col.onClick = (item, index) => { onClick(item, col.httpService, index) };
    } else {
      col.onClick = onClick;
    }
    col.getIcons = getIcons;
    col.forceIcon = true;
    return col;
  }

  public computeCss: () => any = () => '';
  public onClick: (item, index?) => void = () => null;
  public transform: (value) => string = (x) => '' + x;
  public getValue: (item) => string = (x) => !!x[this.field] ? '' + x[this.field]:'';
  public getIcons: (item, icons: string[]) => string[] = (x, y) => y;
  public shouldDisplay: (item) => boolean = (x) => true;
  public forceIcon: boolean = false;

  private constructor(field: string, label: string, icons: string[]) {
    this.field = field;
    this.label = label;
    this.icons = icons;
  }
}
