export class VitamResponse {
  public $context: Context;
  public $hits: Hits;
  public $results: any[];
  public httpCode: number
}

export class Context {
  public $filter: any;
  public $projection: any;
  public $query: any
}

export class Hits {
  public total: number;
  public offset: number;
  public limit: number;
  public size: number;
}
