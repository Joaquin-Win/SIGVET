@echo off
chcp 65001 > nul

REM Ir al directorio donde está este BAT (raíz del proyecto SIGVET)
cd /d "%~dp0"

echo.
echo  ==========================================
echo   SIGVET v1.0.0 - Iniciando aplicacion...
echo  ==========================================
echo.
echo  Directorio: %CD%
echo.

REM Verificar Java
java -version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Java no encontrado. Instale JDK 17 o superior.
    echo  Descarga: https://adoptium.net/
    pause
    exit /b 1
)

REM ---- Buscar JAR y ejecutar directamente si existe ----
set JAR=%~dp0target\sigvet-1.0.0.jar

if exist "%JAR%" (
    echo  Ejecutando SIGVET con JAR existente...
    java -jar "%JAR%"
    pause
    exit /b 0
)

REM ---- JAR no existe: compilar ----
echo  JAR no encontrado. Compilando el proyecto...
echo.

REM Buscar mvn: primero en PATH, luego en rutas conocidas
set MVN=
where mvn >nul 2>&1
if not errorlevel 1 (
    set MVN=mvn
    echo  Maven encontrado en PATH.
    goto :compilar
)

REM Ruta del wrapper local (.mvn/wrapper)
set MVN_WRAPPER=%~dp0mvnw.cmd
if exist "%MVN_WRAPPER%" (
    set MVN=%MVN_WRAPPER%
    echo  Usando Maven Wrapper local.
    goto :compilar
)

REM Ruta fija del wrapper descargado
set MVN_ABS=C:\Users\winck\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd
if exist "%MVN_ABS%" (
    set MVN=%MVN_ABS%
    echo  Usando Maven en: %MVN_ABS%
    goto :compilar
)

echo  ERROR: Maven no encontrado.
echo.
echo  Opciones para solucionarlo:
echo    1. Abra una terminal en esta carpeta y ejecute:
echo       mvn clean package -DskipTests
echo    2. O instale Maven: https://maven.apache.org/download.cgi
pause
exit /b 1

:compilar
echo  Ejecutando: %MVN% clean package -DskipTests
echo.
call "%MVN%" clean package -DskipTests
if errorlevel 1 (
    echo.
    echo  ERROR: La compilacion fallo. Revise los mensajes anteriores.
    pause
    exit /b 1
)

echo.
echo  Compilacion exitosa. Iniciando SIGVET...
echo.
java -jar "%JAR%"
pause
