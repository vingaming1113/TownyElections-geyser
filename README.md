# TownyElections-geyser

[![GitHub Actions](https://img.shields.io/github/actions/workflow/status/vingaming1113/TownyElections-geyser/build.yml?branch=main&style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/actions)
[![GitHub Release](https://img.shields.io/github/v/release/vingaming1113/TownyElections-geyser?style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/releases)
[![GitHub License](https://img.shields.io/github/license/vingaming1113/TownyElections-geyser?style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/blob/main/LICENSE)

**TownyElections-geyser** is a [GeyserMC](https://geysermc.org) extension that provides **native Bedrock Forms GUI** for [TownyElections](https://github.com/vingaming1113/TownyElections).

This allows Bedrock Edition players to use **native Bedrock forms** instead of the Java inventory GUI for all election interactions, providing a much better user experience for Bedrock players.

## Features

- **Native Bedrock Forms GUI**: Bedrock players see beautiful native forms instead of inventory GUIs
- **Automatic Form Display**: Forms are automatically shown to Bedrock players when they join
- **Full Election Support**: Works with all TownyElections features:
  - Nomination phase (view candidates)
  - Voting phase (vote for candidates with one click)
  - Results display (view election results)
- **Candidate Selection**: Easy one-click voting with candidate buttons
- **Party Display**: Shows candidate party affiliations with colors
- **Time Remaining**: Shows countdown for current election phase
- **Vote Confirmation**: Confirms successful vote casting
- **Error Handling**: Shows user-friendly messages for errors

## Installation

### Prerequisites

- **Minecraft Server**: Paper, Spigot, or any fork supporting plugins
- **GeyserMC**: Version 2.9.0 or higher (required for Forms API)
- **Towny**: Version 0.102.0.13 or higher
- **TownyElections**: Version 1.2.0 or higher
- **Java**: Java 21 or higher

### Setup

1. **Install TownyElections** on your server (if not already installed)
2. **Install GeyserMC** following the [official installation guide](https://geysermc.org/download/)
3. **Download** the latest TownyElections-geyser extension from [Releases](https://github.com/vingaming1113/TownyElections-geyser/releases)
4. **Place** the `.jar` file in Geyser's `extensions` folder (not the server's plugins folder!)
5. **Restart** Geyser or your server

## Usage

### For Bedrock Players

**No commands needed!** When a Bedrock player joins the server:

1. **If there's an active election in their town**, they will automatically see a form:
   - **Nomination Phase**: Shows current candidates and time remaining
   - **Voting Phase**: Shows all candidates as clickable buttons to vote
   - **Results Phase**: Shows election results with vote counts

2. **Voting**: Simply click on a candidate's button to vote for them
3. **Confirmation**: After voting, they'll see a confirmation form

### For Server Admins

The extension works automatically. Bedrock players will:
- Receive election forms automatically when they join
- See forms appropriate to their town's election status
- Be able to vote with one click
- See confirmation and error messages in native Bedrock forms

## Building from Source

### Prerequisites

- Java 21 JDK
- Gradle 8.10.2 or higher

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/vingaming1113/TownyElections-geyser.git
   cd TownyElections-geyser
   ```

2. Build the extension:
   ```bash
   ./gradlew build shadowJar
   ```

3. The built JAR will be in `build/libs/TownyElections-geyser-1.0.0.jar`

## Configuration

Currently, TownyElections-geyser requires no additional configuration. All settings are inherited from TownyElections.

## Troubleshooting

### Common Issues

- **Forms not showing**: Ensure you have Geyser 2.9.0+ (Forms API was added in 2.9.0)
- **Extension not loading**: Check that the JAR is in Geyser's `extensions` folder, not the server's `plugins` folder
- **Players can't vote**: Verify they are residents of a town with an active election
- **Errors in console**: Enable debug logging in Geyser's config:
  ```yaml
  geyser:
    debug: true
  ```

### Checking if it Works

1. Join as a Bedrock player
2. Check the Geyser console for: `TownyElections Geyser Extension enabled successfully!`
3. You should automatically receive an election form when you join

## Contributing

Contributions are welcome! Please read our [Contributing Guide](https://github.com/vingaming1113/TownyElections/blob/main/CONTRIBUTING.md) for details on how to contribute.

### Reporting Issues

When reporting issues, please include:
- Minecraft version
- Geyser version (must be 2.9.0+)
- TownyElections version
- TownyElections-geyser version
- Server software (Paper, Spigot, etc.)
- Screenshot of the form (if applicable)
- Error logs from Geyser console

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- [TownyElections](https://github.com/vingaming1113/TownyElections) - Main plugin
- [GeyserMC](https://geysermc.org) - Bedrock to Java proxy
- [Geyser Extensions Documentation](https://geysermc.org/wiki/geyser/extensions)
- [Geyser Forms API Documentation](https://geysermc.org/wiki/geyser/forms)

## Support

For support, please:
1. Check the [Issues](https://github.com/vingaming1113/TownyElections-geyser/issues) for existing problems
2. Join our Discord server (link in TownyElections README)
3. Open a new issue with all required information
