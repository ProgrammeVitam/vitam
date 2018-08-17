export enum FacetType {
  TERMS = 'TERMS', DATE_RANGE = 'DATE_RANGE', FILTERS = 'FILTERS'
}

export enum RequestOrder {
  ASC, DESC
}

export class Facet {
  constructor(public name: string, public facetType: FacetType) {}
}

export class TermFacet extends Facet {
  constructor(name: string, public field: string, public order: RequestOrder, public size: number = 100) {
    super(name, FacetType.TERMS);
  }
}

export class DateRangeFacet extends Facet {
  public ranges: any[];
  constructor(name: string, public field: string, public format: string, dateMin: number, dateMax: number) {
    super(name, FacetType.DATE_RANGE);
    this.ranges = [{dateMin: dateMin, dateMax: dateMax}];
  }
}

// TODO Implem and use for FilterFacet
export class FilterFacet extends Facet {
  constructor(name: string, public filters: any[]) {
    super(name, FacetType.FILTERS);
  }
}

export class FacetDefinition {
  label: string;
  name: string;
  id: string;
  createFacet: (x) => Facet;
  facetType: FacetType;
  getBaseInput: () => any = () => {};

  // Used for DateRange Facets
  minRange?: number;
  maxRange?: number;

  static makeTermFacetDefinition = (label: string, name: string, id: string, createFacet?: (x) => TermFacet): FacetDefinition => {
    let def = new FacetDefinition();
    def.facetType = FacetType.TERMS;
    def.label = label;
    def.name = name;
    def.id = id;
    def.getBaseInput = () => null;
    if (createFacet) {
      def.createFacet = createFacet;
    } else {
      def.createFacet = () => new TermFacet(name, id, RequestOrder.ASC)
    }

    return def;
  };

  static makeDateFacetDefinition = (label: string, name: string, id: string, minRange: number, maxRange: number, createFacet?: (x) => DateRangeFacet): FacetDefinition => {
    let def = new FacetDefinition();
    def.facetType = FacetType.DATE_RANGE;
    def.label = label;
    def.name = name;
    def.id = id;
    def.minRange = minRange;
    def.maxRange = maxRange;
    def.getBaseInput = () => [minRange, maxRange];
    if (createFacet) {
      def.createFacet = createFacet;
    } else {
      def.createFacet = (input) => new DateRangeFacet(name, id, 'yyyy', input[0], input[1])
    }

    return def;
  };
}