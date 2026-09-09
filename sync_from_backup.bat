@echo off
REM BhashaSetu Backup Sync Script
REM Syncs changes from backup to working folder

echo ========================================
echo BhashaSetu - Backup Sync Script
echo ========================================
echo.

REM Check if backup exists
if not exist "J:\Quixotic_backup" (
    echo ERROR: Backup folder not found at J:\Quixotic_backup
    pause
    exit /b 1
)

REM Sync from backup to working folder
echo Syncing from backup to working folder...
xcopy /E /Y /I "J:\Quixotic_backup\*" "J:\Quixotic\"

if %errorlevel% equ 0 (
    echo.
    echo Sync completed successfully!
    echo Working folder (J:\Quixotic) is now synced with backup.
) else (
    echo.
    echo ERROR: Sync failed.
)

echo.
pause
