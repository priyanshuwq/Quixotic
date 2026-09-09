# BhashaSetu - Backup & Sync Workflow

## Folder Structure

```
J:\
├── Quixotic\          # Working folder (edits go here)
└── Quixotic_backup\   # Backup clone (safe copy)
```

## Workflow

### 1. Make Changes
- Edit files in `J:\Quixotic` (working folder)

### 2. Sync to GitHub
```batch
# Double-click or run:
J:\Quixotic\sync_to_github.bat
```

### 3. Restore from Backup (if needed)
```batch
# Double-click or run:
J:\Quixotic\sync_from_backup.bat
```

## Manual Git Commands

```bash
# Navigate to working folder
cd J:\Quixotic

# Add changes
git add -A

# Commit
git commit -m "Your commit message"

# Push to GitHub
git push origin main

# Pull from GitHub
git pull origin main
```

## Creating a New Backup

```powershell
# Remove old backup
Remove-Item -Recurse -Force "J:\Quixotic_backup"

# Create new backup
Copy-Item -Path "J:\Quixotic" -Destination "J:\Quixotic_backup" -Recurse -Force
```

## First-Time GitHub Setup

```bash
# Add remote (replace with your repo URL)
git remote add origin https://github.com/your-username/bhashasetu.git

# Push initial commit
git push -u origin main
```

## Notes

- **Always work in `J:\Quixotic`** (working folder)
- **Backup is a safe copy** - use it to restore if something goes wrong
- **Run `sync_to_github.bat`** after making changes to push to GitHub
