@echo off
:: ----------------------------------------------------------------------------
:: Copyright 2014-2016 Peng Wan <phylame@163.com>
::
:: This file is part of Imabw.
::
:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
:: ----------------------------------------------------------------------------

:: -- Get the IMABW home
if "%IMABW_HOME%" == "" set "IMABW_HOME=%~dp0.."

:: -- IMABW main class
set IMABW_CLASS=pw.phylame.jem.imabw.app.ImabwKt

:: -- Set extension JAR
setlocal EnableDelayedExpansion
set IMABW_CLASS_PATH=
for %%i in ("%IMABW_HOME%"\lib\*.jar) do set IMABW_CLASS_PATH=!IMABW_CLASS_PATH!;%%i
for %%i in ("%IMABW_HOME%"\lib\ext\*.jar) do set IMABW_CLASS_PATH=!IMABW_CLASS_PATH!;%%i
set IMABW_CLASS_PATH=%IMABW_CLASS_PATH:~1%

:: -- Run Jem SCI
java -cp %IMABW_CLASS_PATH% %IMABW_CLASS% %*

endlocal
