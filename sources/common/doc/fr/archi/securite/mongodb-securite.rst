Securité de MongoDB
####################

Objectifs
==========

L'objectif est de sécuriser l'accèss à la base de donnée MongoDB.
MongoDB exige que tous les clients de s'authentifier afin de déterminer leur accès.

Pour contrôler l'accèss à Mongo, vous avez besoin de créer une 
base de donnée pour chaque module (ex. MetaData, Logbook et Functional-administration) et 
ajouter les comptes applicatifs aux bases de données.

    * Pour functional-administration : db-functional-administration ; user : user-functional-administration
    * Pour logbook : db-logbook ; user : user-logbook
    * Pour metadata : db-metadata ; user : user-metadata

Lorsque vous ajoutez un compte applicatif, vous créez l'utilisateur avec son mot de passe 
dans une base de données spécifique. 
Cette base de données est la base de données d'authentification pour l'utilisateur.
