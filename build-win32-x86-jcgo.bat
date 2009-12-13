@echo off
rem @(#) build-win32-x86-jcgo.bat - Windows build script for JadRetro.
rem Used tools: JCGO, MinGW.

set PROJ_UNIX_NAME=jadretro
set DIST_DIR=.dist-win32-x86-jcgo

echo Building Win32/x86 executable using JCGO+MinGW...

if "%JCGO_HOME%"=="" set JCGO_HOME=C:\JCGO
if "%MINGW_ROOT%"=="" set MINGW_ROOT=C:\MinGW

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
if not exist "%DIST_DIR%\.jcgo_Out" mkdir "%DIST_DIR%\.jcgo_Out"

if not exist "%DIST_DIR%\%PROJ_UNIX_NAME%" mkdir "%DIST_DIR%\%PROJ_UNIX_NAME%"
if exist "%DIST_DIR%\%PROJ_UNIX_NAME%.exe" del "%DIST_DIR%\%PROJ_UNIX_NAME%.exe"

%JCGO_HOME%\jcgo -d "%DIST_DIR%\.jcgo_Out" -src $~/goclsp/clsp_asc -src src net.sf.%PROJ_UNIX_NAME%.Main @$~/stdpaths.in
if errorlevel 1 goto exit

"%MINGW_ROOT%\bin\gcc" -o "%DIST_DIR%\%PROJ_UNIX_NAME%\%PROJ_UNIX_NAME%" -I%JCGO_HOME%\include -I%JCGO_HOME%\include\boehmgc -I%JCGO_HOME%\native -Os -fwrapv -fno-strict-aliasing -freorder-blocks -DJCGO_FFDATA -DJCGO_USEGCJ -DJCGO_NOJNI -DJCGO_NOSEGV -DEXTRASTATIC=static -DJNIIMPORT=static/**/inline -DJNIEXPORT=JNIIMPORT -DJNUBIGEXPORT=static -DGCSTATICDATA= -DJCGO_GCRESETDLS -DJCGO_NOFP -DJCGO_SYSWCHAR -DJCGO_SYSDUALW -fno-optimize-sibling-calls -s "%DIST_DIR%\.jcgo_Out\Main.c" %JCGO_HOME%\libs\x86\mingw\libgc.a
if errorlevel 1 goto exit

copy /y /b GNU_GPL.txt "%DIST_DIR%\%PROJ_UNIX_NAME%"
copy /y /b %PROJ_UNIX_NAME%.txt "%DIST_DIR%\%PROJ_UNIX_NAME%"
echo .

"%DIST_DIR%\%PROJ_UNIX_NAME%\%PROJ_UNIX_NAME%"

echo .
echo BUILD SUCCESSFUL

:exit
