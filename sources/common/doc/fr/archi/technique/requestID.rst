Request ID
##########

Le **Request ID** est un identifiant métier de corrélation qui doit être positionné par l'appelant.

Il permet de suivre un traitement à travers tous les services qui y participent.

Cet identifiant est transporté par le header HTTP "**X-REQUEST-ID**".

Filtre client
*************

*Classe :* **fr.gouv.vitam.common.client2.RequestIdClientFilter**

Récupère le **Request ID** depuis le **VitamSession** et le positionne dans le Header "**X-REQUEST-ID**".

Ce filtre est référencé dans *fr.gouv.vitam.common.client2.AbstractCommonClient.AbstractCommonClient(VitamClientFactoryInterface<?>)*.

Sauvegarde dans le thread local
*******************************

*Package :* **fr.gouv.vitam.common.thread**

Le **Request ID** est sauvegardé dans l'objet **VitamSession** qui est positionné dans le **VitamThreadFactory.VitamThread** qui étend le thread local.

Le **VitamThreadPoolExecutor** gère la recopie du **VitamSession** d'un thread père vers un thread fils.

Le **VitamThreadPoolExecutor.VitamRunnable** encapsule le **VitamThreadFactory.VitamThread**.

**VitamThreadUtils** permet de récupérer le **VitamSession**. Si l'état du thread ne le permet pas, une **VitamThreadAccessException** est levée.

Filtre Serveur
**************

*Classe :* **fr.gouv.vitam.common.server2.RequestIdContainerFilter**

Extrait le **Request ID** depuis le Header "**X-REQUEST-ID**" et le positionne dans le **VitamSession**.

Ce filtre est référencé dans *fr.gouv.vitam.common.server2.application.AbstractVitamApplication.buildApplicationHandler()*

Si le request ID présent dans la session n'était pas nul, on trace un warning.

Affichage dans les logs
***********************

Pour afficher le request ID dans les logs, le mécanisme MDC de Logback est utilisé : http://logback.qos.ch/manual/mdc.html

Dans le **VitamSession**, lorsque qu'on fait un **setRequestId**, cela positionne la valeur au niveau du MDC :

  MDC.put(GlobalDataRest.X_REQUEST_ID, newRequestId);

Dans la configuration de Logback, on rajoute **%X{X-REQUEST-ID}** dans le pattern de log. Par exemple :

  <pattern>%d{ISO8601} [%thread] [**%X{X-REQUEST-ID}**] %-5level %logger - %replace(%caller{1..2}){'Caller\+1	 at |\n',''} : %msg %rootException{5}%n</pattern>
