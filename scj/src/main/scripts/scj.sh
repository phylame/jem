#!/bin/bash
# ------------------------------------------------------------------------------
# Copyright 2014-2016 Peng Wan <phylame@163.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ------------------------------------------------------------------------------

# Get the SCJ home
if [ -z "$SCJ_HOME" -o ! -d "$SCJ_HOME" ]; then
  PRG="$0"
  # need this for relative symlinks
  while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG=`dirname "$PRG"`"/$link"
    fi
  done

  SCJ_HOME=`dirname "$PRG"`/..

  # make it fully qualified
  SCJ_HOME=`cd "$SCJ_HOME" > /dev/null && pwd`
fi

# SCJ main class
SCJ_CLASS=pw.phylame.jem.scj.app.AppKt

# Set extension JAR
SCJ_CLASS_PATH=""
LIB_DIR="$SCJ_HOME"/lib
EXT_DIR="$SCJ_HOME"/lib/ext

find_jars(){
if [ -d "$1" ]; then
  for file in "$1"/*.jar; do
    SCJ_CLASS_PATH="$SCJ_CLASS_PATH:$file"
  done
  if [ -n "$SCJ_CLASS_PATH" ]; then
    len=`expr length "$SCJ_CLASS_PATH"`
    SCJ_CLASS_PATH=`expr substr "$SCJ_CLASS_PATH" 2 "$len"`
  fi
fi
}

find_jars ${LIB_DIR}
find_jars ${EXT_DIR}

# Run Jem SCI
java -cp "${SCJ_CLASS_PATH}" ${SCJ_CLASS} "$@"
