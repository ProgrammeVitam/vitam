export class NodeData {
  public haveMoreChildren: boolean = false;
  public haveMoreParents: boolean = false;
  public allunitups: string[] = [];
  public nbUnits: number = 0;
  constructor(public type: string, public unitups: string[]) {}
}

export class TreeNode {
  leaf = false;
  expended = false;
  children: TreeNode[] = [];
  parents: TreeNode[] = [];
  roots: TreeNode[] = [];

  constructor(public label: string, public id: string, public data: NodeData) {}
}
