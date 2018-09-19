#!/bin/bash

echo "Stoping all service"

ps ax | grep IngestExternalMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep IngestInternalMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep AccessInternalMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep AccessExternalMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep MetadataMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep LogbookMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep StorageMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep WorkspaceMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep AdminManagementMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep WorkerMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep DefaultOfferMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep ServerApplication | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep ProcessManagementMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep IdentityMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep IhmDemoMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
ps ax | grep IhmRecetteMain | grep -v grep | awk '{ print $1 }'  | xargs kill -9
