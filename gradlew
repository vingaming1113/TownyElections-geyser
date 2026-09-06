#!/bin/bash

#
# Copyright © 2015-2021 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    app_path=$APP_HOME${app_path##*/}
done

# This is normally unused
# shellcheck disable=SC2034
DEFAULT_JVM_OPTS=("-Xmx64m" "-Xms64m")

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in                #(
  CYGWIN* )           cygwin=true  ;;
  Darwin* )           darwin=true  ;;
  MSYS* | MINGW* )    msys=true    ;;
  NonStop* )          nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD=$JAVA_HOME/bin/java
    else
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptor limit if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        MAX_FD=$(ulimit -H -n) ||
            warn "Could not query maximum file descriptor limit"
      ;;
    esac
    case $MAX_FD in  #(
      '' | soft) :;; # Leave empty or 'soft' as-is
      *)              
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
      ;;
    esac
fi

# Collect all arguments for the java command, following the shell quoting and substitution rules
# We use the  "[email protected]" variant to ensure that we handle the arguments correctly where they
# contain whitespace

JVM_OPTS=("$@")

# by default, don't use a native launcher
USE_LAUNCHER= ""

# if we have a native launcher and we have a JAVA_HOME, use the native launcher
if [ -f "$APP_HOME/bin/native-platform" ] && [ -n "$JAVA_HOME" ] ; then
    USE_LAUNCHER= "-Dorg.gradle.launcher.daemon.helper=native-platform"
fi

# Determine the class file
MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

# Add the daemon options
if [ -n "$USE_LAUNCHER" ] ; then
    JVM_OPTS+=("$USE_LAUNCHER")
fi

# Add the wrapper jar to the classpath
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$CLASSPATH"

# Add the daemon jar to the classpath if it exists
if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper-daemon.jar" ]; then
    CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-wrapper-daemon.jar"
fi

# Add the daemon native library directory to the classpath if it exists
if [ -d "$APP_HOME/gradle/wrapper/lib" ]; then
    CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/lib/*"
fi

# Add the daemon native library directory to the LD_LIBRARY_PATH if it exists
if [ -d "$APP_HOME/gradle/wrapper/lib" ]; then
    LD_LIBRARY_PATH="$APP_HOME/gradle/wrapper/lib:$LD_LIBRARY_PATH"
    export LD_LIBRARY_PATH
fi

# Build the native library path
if [ -d "$APP_HOME/gradle/wrapper/lib" ]; then
    DYLD_LIBRARY_PATH="$APP_HOME/gradle/wrapper/lib:$DYLD_LIBRARY_PATH"
    export DYLD_LIBRARY_PATH
fi

# Add the daemon native library directory to the PATH if it exists
if [ -d "$APP_HOME/gradle/wrapper/bin" ]; then
    PATH="$APP_HOME/gradle/wrapper/bin:$PATH"
    export PATH
fi

# Add the daemon native library directory to the DYLD_LIBRARY_PATH if it exists
if [ -d "$APP_HOME/gradle/wrapper/bin" ]; then
    DYLD_LIBRARY_PATH="$APP_HOME/gradle/wrapper/bin:$DYLD_LIBRARY_PATH"
    export DYLD_LIBRARY_PATH
fi

# Use the maximum available, or set MAX_FD != -1 to use that value.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        MAX_FD=$(ulimit -H -n) ||
            warn "Could not query maximum file descriptor limit"
      ;;
    esac
    case $MAX_FD in  #(
      '' | soft) :;; # Leave empty or 'soft' as-is
      *)              
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
      ;;
    esac
fi

# Collect all arguments for the java command, following the shell quoting and substitution rules
# We use the  "[email protected]" variant to ensure that we handle the arguments correctly where they
# contain whitespace

exec "$JAVACMD" "${DEFAULT_JVM_OPTS[@]}" "${JVM_OPTS[@]}" -Dorg.gradle.appname="${APP_BASE_NAME}" -classpath "$CLASSPATH" $MAIN_CLASS "$@"
