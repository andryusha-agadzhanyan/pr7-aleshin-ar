@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\ru\edu\pr04\Product.java src\ru\edu\pr04\Employee.java src\ru\edu\pr04\OrderItem.java src\ru\edu\pr04\Order.java src\ru\edu\pr04\OrderStatus.java src\ru\edu\pr04\OrderService.java src\ru\edu\pr04\SelfCheck.java
if errorlevel 1 exit /b 1
java -cp out ru.edu.pr04.SelfCheck %*
exit /b %errorlevel%
