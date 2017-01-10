#!/bin/bash
# ------------------------------------------------------------------------------
# Copyright 2014-2016 Peng Wan <phylame@163.com>
#
# This file is part of Imabw.
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

message()
{
  TITLE="Cannot start PW Imabw"
  if [ -n "`which zenity`" ]; then
    zenity --error --title="$TITLE" --text="$1"
  elif [ -n "`which kdialog`" ]; then
    kdialog --error --title "$TITLE" "$1"
  elif [ -n "`which xmessage`" ]; then
    xmessage -center "ERROR: $TITLE: $1"
  elif [ -n "`which notify-send`" ]; then
    notify-send "ERROR: $TITLE: $1"
  else
    echo "ERROR: $TITLE\n$1"
  fi
}

GREP=`which egrep`
CAT=`which cat`

# Get the IMABW home
if [ -z "$IMABW_HOME" -o ! -d "$IMABW_HOME" ]; then
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

  IMABW_HOME=`dirname "$PRG"`/..

  # make it fully qualified
  IMABW_HOME=`cd "$IMABW_HOME" > /dev/null && pwd`
fi

VM_OPTIONS_FILE=""
if [ -n "$IMABW_VM_OPTIONS" -a -r "$IMABW_VM_OPTIONS" ]; then
  # explicit
  VM_OPTIONS_FILE="$IMABW_VM_OPTIONS"
elif [ -r "$HOME/.imabw/imabw.vmoptions" ]; then
  # user-overridden
  VM_OPTIONS_FILE="$HOME/.imabw/imabw.vmoptions"
elif [ -r "$IMABW_HOME/bin/imabw.vmoptions" ]; then
  # default, standard installation
  VM_OPTIONS_FILE="$IMABW_HOME/bin/imabw.vmoptions"
else
  VM_OPTIONS_FILE="$IMABW_HOME/imabw.vmoptions"
fi

VM_OPTIONS=""
if [ -r "$VM_OPTIONS_FILE" ]; then
  VM_OPTIONS=`"$CAT" "$VM_OPTIONS_FILE" | "$GREP" -v "^#.*"`
else
  message "Cannot find VM options file"
fi

# IMABW main class
IMABW_CLASS=jem.imabw.app.ImabwKt

# Set extension JAR
IMABW_CLASS_PATH=""
LIB_DIR="$IMABW_HOME"/lib
EXT_DIR="$IMABW_HOME"/lib/ext

find_jars(){
if [ -d "$1" ]; then
  for file in "$1"/*.jar; do
    IMABW_CLASS_PATH="$IMABW_CLASS_PATH:$file"
  done
  if [ -n "$IMABW_CLASS_PATH" ]; then
    len=`expr length "$IMABW_CLASS_PATH"`
    IMABW_CLASS_PATH=`expr substr "$IMABW_CLASS_PATH" 2 "$len"`
  fi
fi
}

find_jars ${LIB_DIR}
find_jars ${EXT_DIR}

# Run Jem SCI
java $VM_OPTIONS -cp "${IMABW_CLASS_PATH}" ${IMABW_CLASS} "$@"
