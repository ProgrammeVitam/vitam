Gestion des droits sur IHM demo
###############################

Cette documentation décrit la gestion des droits sur IHM-demo.
--------------------------------------------------------------

La gestion des droits (authorisations et habilitations) sur IHM demo (et VITAM en génénral) se fait grâce à shiro.

Gestion des autorisations
*************************

Les utilisateurs sont définis dans le fichier `shiro.ini` sous la forme d'un login suivi du mot de passé encodé avec l'algorithme md5.

Gestion des permissions
***********************

Sur chaque endpoint (couple URI / verbe HTTP), qui correpond à une méthode Java, on définit une permission grâce à l'annotation `RequiresPermissions`.

Par convention, la permission est nommée en fonction de l'URI est du verbe HTTP correspondant. Par exemple, la permission définissant la lecture sur l'URL `/logbook` est : `logbook:read`.
Si une l'URL possède une sous collection, par exemple `/logbook/operations`, alors le nom de la permission pour lire les informations est : `logbook:operation:read`.

La correspondance entre les verbes HTTP et les permissions est la suivante :
 - GET : read
 - POST : update
 - PUT : create
 - DELETE : delete

Par contre, dans le cas ou on utilise un POST pour de la lecture (cas typique du DSL), on nommera quand même la permission avec `read`.

Au niveau du fichier shiro.ini, dans la section `roles`, on définit trois rôles (admin, user et guest), auxquels on associe les différentes permissions définies précédemment.

Enfin, dans la section `users`, on associe le rôle à un utilisateur.
