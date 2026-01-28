# StaffPanel Plugin

[![Stars](https://img.shields.io/github/stars/GongSunFangYun/StaffPanel?style=flat-square)]()
[![Forks](https://img.shields.io/github/forks/GongSunFangYun/StaffPanel?style=flat-square)]()
[![Issues](https://img.shields.io/github/issues/GongSunFangYun/StaffPanel?style=flat-square)]()
[![License](https://img.shields.io/github/license/GongSunFangYun/StaffPanel?style=flat-square)]()
[![Resource](https://github.com/GongSunFangYun/StaffPanel)

## Overview

StaffPanel is a comprehensive administrative plugin for Nukkit servers that provides a powerful suite of staff management tools through both graphical user interfaces and console commands. Designed for server administrators and moderators, it offers robust player management capabilities with detailed logging and configuration systems.

## Key Features

### 1. **Dual Interface System**
   - **Graphical UI**: Intuitive form-based interface accessible via `/staff` command
   - **Console Commands**: Full command-line support for all administrative actions
   - **Permission-based access**: Different UI views for staff and admin users

### 2. **Player Management Suite**
   - **Ban System**: Temporary or permanent bans with customizable durations (minutes, hours, days, years)
   - **Mute System**: Time-based chat restrictions with detailed reason tracking
   - **Kick & Warn**: Immediate player removal and warning system with title notifications
   - **Direct Actions**: Teleport, kill, and weaken effects for in-game management

### 3. **Intelligent Player Matching**
   - **Advanced Search**: Finds players by partial names from online, banned, and muted lists
   - **Multi-source matching**: Searches across online players, banned records, and muted players simultaneously
   - **Case-insensitive**: Handles various name formats and capitalizations

### 4. **Comprehensive Logging**
   - **Action Tracking**: Every administrative action is logged with timestamp, executor, and target
   - **File Persistence**: Logs are saved to files and loaded on plugin restart
   - **Pagination**: View logs through organized pages in the UI
   - **Auto-cleanup**: Automatic removal of expired bans and mutes

### 5. **Smart Record Management**
   - **Automatic Expiration**: Background task checks and removes expired bans/mutes every minute
   - **Player Notification**: Players receive messages when mutes expire or are removed
   - **Config Persistence**: All records saved in JSON format for easy backup and management

### 6. **Permission System**
   - **Granular Controls**: Separate permissions for each action type
   - **Role-based Access**: Different UI panels for staff vs admin permissions
   - **Command Protection**: All actions require specific permissions

## Command Reference

### Basic Commands
- `/staff` - Open staff management interface (form-based)
- `/staff help` - Display comprehensive help with all available commands

### Player Management Commands
- `/staff ban <player> <reason> [duration]` - Ban a player (default: 10y)
- `/staff unban <player>` - Remove a player's ban
- `/staff mute <player> <reason> [duration]` - Mute a player (default: 1h)
- `/staff unmute <player>` - Remove a player's mute
- `/staff kick <player> <reason>` - Kick a player with specified reason
- `/staff warn <player> <message>` - Send warning title to player
- `/staff kill <player>` - Instantly kill a player
- `/staff tp <player>` - Teleport to player and enter spectator mode
- `/staff weaken <player>` - Apply weakness, blindness, and slowness effects

### Information Commands
- `/staff check <player>` - View ban/mute records for a player
- `/staff log [page]` - View action logs with pagination

### Duration Format
- Use suffixes: `m` (minutes), `h` (hours), `d` (days), `y` (years)
- Examples: `30m`, `2h`, `7d`, `1y`

## Permission Nodes

### Staff Permissions
- `staff.use` - Access basic staff interface
- `staff.ban` - Ban players
- `staff.mute` - Mute players
- `staff.kick` - Kick players
- `staff.warn` - Warn players
- `staff.kill` - Kill players
- `staff.tp` - Teleport to players
- `staff.weaken` - Apply weakening effects

### Admin Permissions
- `staff.admin` - Access admin panel with additional features
- `staff.unban` - Unban players
- `staff.unmute` - Unmute players
- `staff.log` - View action logs
- `staff.check` - Check player records

## Configuration Files

The plugin creates and manages three main files in its data folder:

1. **banned.json** - Stores all ban records with details
2. **muted.json** - Stores all mute records with details
3. **staff_log.log** - Plain text log of all administrative actions

## Advanced Features

### 1. **Smart Form Interface**
   - Context-aware buttons (only show actions applicable to online/offline players)
   - Dynamic content based on player status
   - Permission-based button visibility

### 2. **Player Status Detection**
   - Real-time online status checking
   - Automatic application of bans/mutes on player join
   - Chat interception for muted players

### 3. **Error Handling**
   - Comprehensive exception catching
   - Detailed error logging
   - User-friendly error messages

### 4. **Performance Optimizations**
   - Efficient player matching algorithms
   - Background task for expiration checks
   - Memory-efficient log management

## Usage Examples

### Example 1: Quick Player Ban
```
/staff ban Steve Hacking 7d
```
Bans player "Steve" for 7 days with reason "Hacking"

### Example 2: Mute with UI
1. Type `/staff`
2. Enter player name in search
3. Click "Mute"
4. Enter reason and duration (e.g., "Spam", "2h")

### Example 3: Check Player History
```
/staff check Steve
```
Displays detailed ban and mute history for "Steve"

### Example 4: Review Staff Actions
```
/staff log 2
```
Views second page of action logs

## Technical Details

### Player Matching Algorithm
The plugin uses a three-tier matching system:
1. Exact case-insensitive match
2. Starts-with partial match
3. Contains partial match

Searches across:
- Online players
- Banned players (from banned.json)
- Muted players (from muted.json)

### Log Format
```
[YYYY-MM-DD HH:mm:ss] [ACTION] Executor --> Target
```
Example: `[2026-01-28 11:45:14] [BAN] Alex --> Steve`

### Effect Details
- **Weaken**: Applies maximum level weakness (255), blindness (255), and slowness (2) effects
- **Teleport**: Automatically sets staff to spectator mode after teleportation
- **Warn**: Displays a red warning title to the target player

## Installation

1. Place the plugin JAR file in your server's `plugins` folder
2. Start or restart your server
3. Configure permissions using your preferred permission plugin
4. Use `/staff` to access the management interface

## Requirements

- Nukkit server
- Java 8 or higher
- Appropriate permissions configured for staff members

## Building from Source

### Prerequisites
- Java 21 or higher
- Apache Maven 3.6+
- Git

### Clone the Repository and install Requirements
```bash
git clone https://github.com/GongSunFangYun/StaffPanel.git
cd StaffPanel
mvn install
```

### Project Structure
```
StaffPanel/
├── src/main/java/cn/gsfy/
│   ├── StaffMain.java          # Main plugin class
│   ├── StaffCommandParser.java # Command handler
│   ├── StaffForm.java         # GUI form manager
│   └── StaffEventHandler.java # Event listener
├── pom.xml                    # Maven configuration
└── README.md                  # Documentation
```

### Build Process

1. **Compile the Plugin:**
   ```bash
   mvn clean compile
   ```

2. **Package into JAR:**
   ```bash
   mvn package
   ```

3. **Locate the Output:**
   The compiled JAR file will be available at:
   ```
   target/StaffPanel-1.0-SNAPSHOT.jar
   ```

### Maven Configuration Details

The `pom.xml` file configures the following:

- **Java Version**: 21 (source and target compatibility)
- **Dependencies**: 
  - Nukkit API (provided scope, not bundled in final JAR)
- **Build Process**: Standard Maven lifecycle (compile, test, package)

### Development Setup

1. **Import to IDE:**
   - IntelliJ IDEA: File → Open → Select `pom.xml`
   - Eclipse: File → Import → Maven → Existing Maven Projects

2. **Build Configuration:**
   The project uses standard Maven directory structure:
   - Source code: `src/main/java/`
   - Resources: `src/main/resources/`
   - Tests: `src/test/java/`

3. **Dependency Management:**
   ```xml
   <dependency>
       <groupId>cn.nukkit</groupId>
       <artifactId>Nukkit</artifactId>
       <version>MOT-SNAPSHOT</version>
       <scope>provided</scope>
   </dependency>
   ```
   - `provided` scope means Nukkit API is expected to be available at runtime
   - No external dependencies beyond Nukkit API

### Quick Build Script

Create a build script `build.sh` (Linux/Mac) or `build.bat` (Windows):

**Linux/Mac:**
```bash
#!/bin/bash
echo "Building StaffPanel..."
mvn clean package
if [ $? -eq 0 ]; then
    echo "Build successful! JAR file: target/StaffPanel-1.0-SNAPSHOT.jar"
else
    echo "Build failed!"
fi
```

**Windows:**
```batch
@echo off
echo Building StaffPanel...
call mvn clean package
if %errorlevel% equ 0 (
    echo Build successful! JAR file: target/StaffPanel-1.0-SNAPSHOT.jar
) else (
    echo Build failed!
)
```

### Testing the Build

After successful build, you can test the plugin by:

1. Copy the JAR to your Nukkit server's `plugins` folder:
   ```bash
   cp target/StaffPanel-1.0-SNAPSHOT.jar /path/to/nukkit/plugins/
   ```

2. Start/Restart your Nukkit server

3. Verify the plugin loads:
   ```
   [StaffPanel] Plugin successfully enabled!
   ```

### Troubleshooting

**Common Issues:**

1. **Maven not found:**
   ```bash
   # Install Maven on Ubuntu/Debian
   sudo apt-get install maven
   
   # Install Maven on macOS
   brew install maven
   ```

2. **Java version mismatch:**
   Ensure Java 21 is installed and set as default:
   ```bash
   java -version
   # Should show version 21 or higher
   ```

3. **Nukkit dependency not resolved:**
   Make sure you have access to the Nukkit Maven repository, or have the Nukkit JAR in your local Maven repository.

4. **Build failures:**
   Clean the project and rebuild:
   ```bash
   mvn clean
   mvn compile
   ```

### Continuous Integration

The project is Maven-ready for CI/CD pipelines. Sample GitHub Actions workflow:

```yaml
name: Build StaffPanel

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 21
      uses: actions/setup-java@v2
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package
      
    - name: Upload Artifact
      uses: actions/upload-artifact@v2
      with:
        name: StaffPanel
        path: target/*.jar
```

---

*The build process follows standard Maven conventions, making it easy to integrate with existing development workflows and CI/CD pipelines.*

## Support

The plugin includes comprehensive error logging and recovery mechanisms. All actions are logged both to console and file for auditing purposes. The automatic expiration system ensures your ban/mute lists stay clean and up-to-date.

---

*StaffPanel - Professional player management made simple*
