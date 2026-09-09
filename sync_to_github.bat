@echo off
REM BhashaSetu Git Sync Script
REM Syncs changes from J:\Quixotic to GitHub

echo ========================================
echo BhashaSetu - Git Sync Script
echo ========================================
echo.

REM Check if we're in the right directory
if not exist "src" (
    echo ERROR: Please run this script from J:\Quixotic directory
    pause
    exit /b 1
)

REM Add all changes
echo [1/4] Adding changes...
git add -A

REM Check if there are changes to commit
git diff --cached --quiet
if %errorlevel% equ 0 (
    echo No changes to commit.
    goto :push
)

REM Get commit message from user
echo.
set /p commitmsg="Enter commit message: "
if "%commitmsg%"=="" set commitmsg="Update project files"

REM Commit changes
echo [2/4] Committing changes...
git commit -m "%commitmsg%"

:push
REM Push to GitHub
echo [3/4] Pushing to GitHub...
git push origin main

if %errorlevel% equ 0 (
    echo [4/4] Push successful!
    echo.
    echo Changes have been pushed to GitHub.
) else (
    echo.
    echo ERROR: Push failed. Please check your GitHub credentials.
    echo You may need to run: git remote add origin ^<your-repo-url^>
)

echo.
pause
