#!/bin/bash

set -ex

targetdir="../velrok.github.io-master"

lein gen

cp -r public/* $targetdir

cd $targetdir
git add .
git commit -m 'publish'
git push

echo DONE
