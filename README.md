# TownyElections-geyser

[![GitHub Actions](https://img.shields.io/github/actions/workflow/status/vingaming1113/TownyElections-geyser/build.yml?branch=main&style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/actions)
[![GitHub Release](https://img.shields.io/github/v/release/vingaming1113/TownyElections-geyser?style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/releases)
[![GitHub License](https://img.shields.io/github/license/vingaming1113/TownyElections-geyser?style=flat-square)](https://github.com/vingaming1113/TownyElections-geyser/blob/main/LICENSE)

**TownyElections-geyser** is a [GeyserMC](https://geysermc.org) extension that brings [TownyElections](https://github.com/vingaming1113/TownyElections) functionality to Bedrock Edition players.

## Features

- **Full Election Participation**: Bedrock players can vote in town elections alongside Java players
- **Real-time Notifications**: Bedrock players receive notifications about active elections in their towns
- **Bedrock-specific Commands**: Optimized commands for Bedrock players:
  - `/townyelections vote <candidate>` - Cast your vote
  - `/townyelections candidates` - List all candidates
  - `/townyelections status` - Check election status
- **Seamless Integration**: Works with all TownyElections features including:
  - Multiple voting systems (Plurality, Ranked Choice, Approval)
  - Party support
  - Election phases (Nomination, Voting, Runoff)
  - Candidate management

## Installation

### Prerequisites

- **Minecraft Server**: Paper, Spigot, or any fork supporting plugins
- **GeyserMC**: Version 2.9.0 or higher
- **Towny**: Version 0.102.0.13 or higher
- **TownyElections**: Version 1.2.0 or higher
- **Java**: Java 21 or higher

### Setup

1. **Install TownyElections** on your server (if not already installed)
2. **Install GeyserMC** following the [official installation guide](https://geysermc.org/download/)
3. **Download** the latest TownyElections-geyser extension from [Releases](https://github.com/vingaming1113/TownyElections-geyser/releases)
4. **Place** the `.jar` file in Geyser's `extensions` folder
5. **Restart** Geyser or your server

## Usage

### For Bedrock Players

Once installed, Bedrock players can use the following commands:

```
/townyelections vote <candidate>    - Vote for a candidate
/townyelections candidates          - List all candidates in your town's election
/townyelections status              - Check the status of your town's election
```

### For Server Admins

The extension automatically:
- Detects when Bedrock players join
- Notifies them of active elections in their towns
- Allows them to participate in all election activities

No additional configuration is required beyond installing the extension.

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

- **Extension not loading**: Ensure you have Geyser 2.9.0+ and TownyElections 1.2.0+
- **Commands not working**: Check that the extension is in Geyser's `extensions` folder, not the server's `plugins` folder
- **Bedrock players can't vote**: Verify they are residents of a town with an active election

### Debugging

Enable debug logging in Geyser's config to see extension loading messages:
```yaml
geyser:
  debug: true
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](https://github.com/vingaming1113/TownyElections/blob/main/CONTRIBUTING.md) for details on how to contribute.

### Reporting Issues

When reporting issues, please include:
- Minecraft version
- Geyser version
- TownyElections version
- TownyElections-geyser version
- Server software (Paper, Spigot, etc.)
- Error logs or screenshots

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- [TownyElections](https://github.com/vingaming1113/TownyElections) - Main plugin
- [GeyserMC](https://geysermc.org) - Bedrock to Java proxy
- [Geyser Extensions Documentation](https://geysermc.org/wiki/geyser/extensions)

## Support

For support, please:
1. Check the [Issues](https://github.com/vingaming1113/TownyElections-geyser/issues) for existing problems
2. Join our Discord server (link in TownyElections README)
3. Open a new issue with all required information
