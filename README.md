# iBlocky Booster Notification Mod

Welcome to the **iBlocky Booster Notification Mod** repository! 🎉

This mod is designed for the iBlocky Minecraft server to enhance the player's experience by displaying active boosters and their remaining time directly on the in-game HUD.

## Features

- **HUD Display**: Shows active boosters and their remaining time.
- **Dynamic Countdown**: Real-time countdown for boosters like "Tokens" and "Rich Pet" boosters.
- **Customizable Position**: Easily drag and drop the HUD elements to reposition them on your screen.
- **Lightweight**: Built with performance in mind, ensuring minimal impact on game performance.
- **Configurable Logging**: Logs mod activity to a custom log file (`iBlockynotify.log`) to keep your `latest.log` clean.

## Installation

1. **Download the Mod**: Grab the latest release from the [Releases](https://github.com/Holm99/iblocky-boosternotification-1.20.6/releases) page.
2. **Install Fabric Loader**: Make sure you have Fabric Loader installed for Minecraft 1.20.6. You can download it from [Fabric's official website](https://fabricmc.net/use/).
3. **Install Fabric API**: This mod requires Fabric API. Download version `0.100.8+1.20.6` from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api/files/all?page=1&pageSize=20&version=1.20.6&gameVersionTypeId=4).
4. **Add the Mod to Your Mods Folder**: Place the downloaded mod `.jar` file into your `.minecraft/mods` folder.
5. **Launch Minecraft**: Start the game using the Fabric profile.

## Usage

- **Displaying Boosters**: The HUD will automatically show the currently active boosters and their remaining time.
- **Dragging the HUD**: To reposition the HUD, simply click and drag it to your desired location on the screen.
- **Modifying Configurations**: To adjust mod settings like the HUD position, you can edit the configuration file located at `.minecraft/config/iblocky-boosternotification.json`.

## Configuration

The configuration file allows you to set various options:

- **HUD Position**: Configure the X and Y coordinates of the HUD elements.
- **Logging Level**: Adjust the logging level to control what gets logged in `iBlockynotify.log`.

### Example Configuration

```json
{
    "hudPosition": {
        "x": 10,
        "y": 10
    },
    "loggingLevel": "INFO"
}
```
