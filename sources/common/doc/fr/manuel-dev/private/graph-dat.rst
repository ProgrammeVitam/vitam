DAT : module Graph
###################################

Ce document présente l'ensemble de manuel développement concernant l'algorithme de graph qui représente le story #510, qui contient :

modules & packages
--------------------------


1. Modules et packages

Au présent : nous proposons le schéma ci-dessous représentant le module principal
et ses sous modules.

graph

    |---DirectedCycle : Directed cycle detection :  un graphe orienté donné a un cycle dirigé? Si oui, trouver un tel cycle. DirectedCycle.java résout ce problème en utilisant la recherche en profondeur d'abord.

        Depth-first orders: fait de recherche en profondeur d'abord sur chaque sommet exactement une fois. Trois ordres de sommet sont d'un intérêt dans des 
        applications typiques:

        Preorder: Mettre le sommet(vertex) sur une file d'attente avant  les appels récursifs.
        Postorder: Mettre le sommet(vertex)  sur une file d'attente après les appels récursifs.
        Reverse postorder: Mettre le sommet(vertex)  sur une pile après les appels récursifs.

        Le Depth-first search est l'algorithme de recherche des composantes fortement connexes.
        L'algorithme consiste à démarrer d'un sommet et à avancer dans le graphe en ne repassant pas deux fois par le même sommet. Lorsque l'on est bloqué, on ''revient sur ses pas'' jusqu'à pouvoir repartir vers un sommet non visité. Cette opération de ''retour sur ses pas'' est très élégamment prise en charge  par l'écriture d'une procédure récursive.

Après la parse de Unit recursive et la creation d'arbre orienté.Le choix de la racine de départ de l'arbre orienté se fait en faisant le test récursive si l'élément ne possède pas  un up alors c'est un racine .


    |---DirectedGraph  : Un graphe orienté (ou digraphe) est un ensemble de sommets et une collection de bords orientés qui relie chacun une paire ordonnée de sommets.

        Un bord dirigé pointe du premier sommet de la paire et les points au deuxième sommet de la paire. 

    |---Graph Un graphe est composé d'un ensemble de sommets et un ensemble d'arêtes . Chaque arête représente une liaison entre deux sommets.

        Deux sommets sont voisins s'ils sont reliés par un bord , et le degré d'un sommet est le nombre de ses voisins.
        Graph data type. Graph-processing algorithms généralement d'abord construit une représentation interne d'un graphe en ajoutant des arêtes (edges), puis le traiter par itération sur les sommets et sur les sommets adjacents à un sommet donné.

        L'algorithme de chemin le plus long est utilisé pour trouver la longueur maximale d'un graph donné. 
        La longueur maximale peut être mesuré par le nombre maximal d'arêtes ou de la somme des poids dans un graph pondéré.
         
        L'algorithme de chemin le plus long permet de définir dans notre cas le niveau d'indexation de chaque Unit .


***L'algorithme de parcours en profondeur (ou DFS, pour Depth First Search) est un algorithme de parcours d'arbre, et plus généralement de parcours de graphe, qui se décrit naturellement de manière récursive. 
Son application la plus simple consiste à déterminer s'il existe un chemin d'un sommet à un autre. 

