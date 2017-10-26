export class NodeData {
  public haveMoreChildren: boolean = false;
  public haveMoreParents: boolean = false;
  constructor(public type: string, public unitups: string[]) {}
}

export class TreeNode {
  leaf = false;
  expended = false;
  children: TreeNode[] = [];
  parents: TreeNode[] = [];

  constructor(public label: string, public id: string, public data: NodeData) {}
}
