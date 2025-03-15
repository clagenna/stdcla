@echo off
:: ---------------------------------------------------
:: il file  dovrebbe essere un Hard Link 
:: con     \java\photon2\stdcla\bin\UpdVersione.cmd
::    <--> \java\bin\UpdVersione.cmd
:: Verificare con:
:: 	"fsutil.exe hardlink list UpdVersione.cmd"
:: ----------- Settaggi Iniziali --------------------    

:: setto la var per ESC 0x1b
for /F %%a in ('echo prompt $E ^| cmd') do @set "ESC=%%a["
if "%DEBUG%" == "1" @echo on
set curr=%CD%
set ConPOM=0
if "%DEBUG%" == "1" @echo "current : %curr%"
:: aggiorno il progetto corrente
set searchd=%CD%\src\main
:: Se mi hai passato il dir di partenza, utilizzo quello
if "%1" NEQ "" set searchd=%1
if "%2" NEQ "" set ConPOM=%2

@echo %ESC%7mUpdate Versione.java%ESC%0m in %ESC%105m%searchd%%ESC%0m
if "%DEBUG%" == "1" pause
:: mi sposto sotto gli STD/bin
:: portandomi dietro dove cercare la "Versione.java"
:: nella var %searchd%
pushd "%~dp0"
if "%DEBUG%" == "1" @echo sono in %CD%
pwsh -f UpdVersionJava.ps1 %searchd% %ConPOM%
popd
@echo %ESC%7mFine UpdVersione%ESC%0m
