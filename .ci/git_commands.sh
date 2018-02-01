#!/bin/env bash
RAPPORT=rapport.txt
set -e
echo "Début du script $0"

function supprimetemporaires() {
    echo "Suppression des fichiers résiduels..."
    for  i  in base_branch.txt fork_commit.txt nombre_commit_branch.txt nombre_commit_parent.txt
    do
        rm -f ${i}
    done
    echo "Fichiers résiduels supprimés !"
}

supprimetemporaires

#git fetch --all

echo "Branche mère :"
git log --oneline --merges $@ | grep into | sed 's/.* into //g' | head -n 1| sed "s/'//g" | tee base_branch.txt
echo "----------------------------------------------------------"
echo "Commit de fork :"
git merge-base origin/$(cat base_branch.txt) HEAD | tee fork_commit.txt
echo "----------------------------------------------------------"
echo "Nombre de commit sur la branche :"
git log $(cat fork_commit.txt)..HEAD --oneline | wc -l
echo "----------------------------------------------------------"
echo "Nombre de commit sur la branche source depuis le fork :" 
git log $(cat fork_commit.txt)..origin/$(cat base_branch.txt) --oneline | wc -l
echo "----------------------------------------------------------"

#env | tee -a ${RAPPORT}

supprimetemporaires

echo "Fin du script $0"