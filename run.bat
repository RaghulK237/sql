@echo off
:: ============================================================
:: run.bat -- EventSphere v2 Build and Run Script
:: Windows CMD -- double-click or type "run" in this folder.
::
:: BEFORE FIRST RUN:
::   1. Install Java JDK 8+:  java -version / javac -version
::   2. Install MySQL 8+:     mysql -u root -p
::   3. Download mysql-connector-j-8.x.x.jar from
::      https://dev.mysql.com/downloads/connector/j/
::      (Platform Independent -> ZIP -> extract .jar here)
::   4. Set up the database (run once, in this order):
::        mysql -u root -p < database.sql
::        mysql -u root -p < procedures.sql
::        mysql -u root -p < triggers.sql
::        mysql -u root -p < indexes.sql
::        mysql -u root -p < sample_data.sql
::   5. Open DBConnection.java, change PASSWORD to your MySQL password.
::
:: DEFAULT LOGIN:
::   Admin   : admin@eventsphere.com  /  Admin@123
::   Customer: arjun@example.com      /  Pass@123
:: ============================================================

title EventSphere - Event Management System v2

:: -- Step 1: Locate Java Compiler (javac) ---------------------
set JAVAC_CMD=javac
where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javac.exe" set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
    )
)
where %JAVAC_CMD% >nul 2>&1
if %ERRORLEVEL% neq 0 (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
        if exist "%%d\bin\javac.exe" set JAVAC_CMD="%%d\bin\javac.exe"
    )
)
where %JAVAC_CMD% >nul 2>&1
if %ERRORLEVEL% neq 0 (
    for /d %%d in ("C:\Program Files\Java\jdk*") do (
        if exist "%%d\bin\javac.exe" set JAVAC_CMD="%%d\bin\javac.exe"
    )
)

:: -- Step 2: Locate Java Runtime (java) -----------------------
set JAVA_CMD=java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\java.exe" set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
    )
)
where %JAVA_CMD% >nul 2>&1
if %ERRORLEVEL% neq 0 (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
        if exist "%%d\bin\java.exe" set JAVA_CMD="%%d\bin\java.exe"
    )
)

:: -- Step 3: Find the MySQL JDBC JAR -------------------------
set JAR=
for %%f in (mysql-connector-j-*.jar mysql-connector-java-*.jar) do set JAR=%%f

if "%JAR%"=="" (
    echo.
    echo  ERROR: mysql-connector-j-*.jar not found in this folder.
    echo  Download from: https://dev.mysql.com/downloads/connector/j/
    echo  Place the .jar file in the same folder as run.bat.
    echo.
    pause
    exit /b 1
)
echo.
echo  [OK] JDBC Driver found: %JAR%

:: -- Step 4: Compile all Java source files -------------------
echo  Compiling Java source files using UTF-8 encoding...
%JAVAC_CMD% -encoding UTF-8 -cp ".;%JAR%" ^
    DBConnection.java ^
    User.java ^
    AuthService.java ^
    Event.java ^
    EventDAO.java ^
    BookingDAO.java ^
    AdminPanel.java ^
    CustomerPanel.java ^
    Main.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo  ERROR: Compilation failed. Fix the errors shown above.
    echo.
    pause
    exit /b 1
)
echo  [OK] Compilation successful!
echo.

:: -- Step 5: Run the application -----------------------------
echo  Starting EventSphere...
echo  ============================================================
%JAVA_CMD% -cp ".;%JAR%" Main

echo.
echo  ============================================================
echo  EventSphere has exited.
pause
