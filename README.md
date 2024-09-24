# iBlocky Companion Mod

Welcome to the **iBlocky Companion Mod** repository! 🎉

-----------------

-----------------

This mod is designed specifically for the **iBlocky Minecraft server** to enhance the player's experience by displaying active boosters and their remaining time directly on the in-game HUD.

#### BoosterHud
![image](https://github.com/user-attachments/assets/50bcef62-ffea-4daa-af5d-27595118b6e6)![image](https://github.com/user-attachments/assets/9cbf6fc9-f680-4ca5-a54e-390464d450a7)

#### EnchantHUD
![image](https://github.com/user-attachments/assets/8b5691ae-c59c-4e09-871b-dc9313ffd891)




## Pre installation - Downloads
### Windows
1. **Download the Mod**: Grab the latest release from the [Releases](https://github.com/Holm99/iBlockyCompanion-1.20.6/releases/latest) page.
2. **Download Fabric Loader**: Only needed for vanilla client. Download **Fabric Loader** from [Fabric's official website - Direct Download](https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.exe).
3. **Install Fabric API**: Only needed for vanilla client. This mod requires **Fabric API version 0.100.8+1.20.6**. [CurseForge - Direct Download](https://mediafilez.forgecdn.net/files/5577/501/fabric-api-0.100.8%2B1.20.6.jar).
4. **Install Java 21**: Ensure that you have Java 21 installed. You can download it from [x64 Installer - Direct Download](https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.exe).

### MAC
1. **Download the Mod**: Grab the latest release from the [LTS - Releases](https://github.com/Holm99/iBlockyCompanion-1.20.6/releases/latest) page.
2. **Download Fabric Loader**: Only needed for vanilla client. Download **Fabric Loader** from [Fabric's official website - Direct Download](https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar).
3. **Install Fabric API**: Only needed for vanilla client. This mod requires **Fabric API version 0.100.8+1.20.6**. [CurseForge - Direct Download](https://mediafilez.forgecdn.net/files/5577/501/fabric-api-0.100.8%2B1.20.6.jar).
4. **Install Java 21**: Ensure that you have Java 21 installed. You can download it from:
   1. For Apple Silicon (M1, M2, etc): [macOS Arm 64 DMG Installer - Direct Download](https://download.oracle.com/java/21/latest/jdk-21_macos-aarch64_bin.dmg).
   2. For Intel-Based Macs:  [macOS x64 DMG Installer - Direct Download](https://download.oracle.com/java/21/latest/jdk-21_macos-x64_bin.dmg).

# Installation
## Windows
### Install For Vanilla Minecraft Client 
**[Vanilla Client - YouTube Tutorial](https://www.youtube.com/watch?v=hmYcvN4ViZ4)**
1. Install Java 21 by double-clicking on the `jdk-x.x.x_windows-x64_bin.exe` (Follow its instructions).
2. **Install Fabric Loader**: Open the application and select **Minecraft version 1.20.6** & **Loader Version 0.16.0**, then click install!
3. **Adding the installed mods**: Open Minecraft goto installations. Hover a release and click the Folder icon
   1. Open the Mods folder (Create it if it doesn't exist)
   2. Copy over Fabric-API and iBlockyCompanion mod for your downloads folder into here.
4. **Launch Minecraft**: Start the game using the Fabric profile. (Recommend increasing your memory whilst you are there.)
5. Play

### Install For Lunar Client
**[Lunar Client - YouTube Tutorial](https://youtu.be/_x--w80u_P0)**
1. Open Lunar Client and select versions
2. Select 1.20, then on the right hand side select addon: `Lunar + Fabric`
3. Version settings > Mods > drag the iBlockyCompanion-x.x.x.jar into the application
4. Play

## Mac
### Install For Vanilla Minecraft Client
[YouTube Tutorial](https://www.youtube.com/watch?v=aaGtGCnsD2g) _(Not my tutorial but tested and submitted to me by macOS users, use the links above which I listed as they are more up-to-date)_.
1. Install Java 21 by double-clicking on the `jdk-xx_macos-xyz64_bin.dmg` (Follow its instructions).
2. **Install Fabric Loader**: Open the application and select **Minecraft version 1.20.6** & **Loader Version 0.16.0**, then click install!
3. **Adding the installed mods**: Open Minecraft goto installations. Hover a release and click the Folder icon
   1. Open the Mods folder (Create it if it doesn't exist)
   2. Copy over Fabric-API and iBlockyCompanion mod for your downloads folder into here.
4. **Launch Minecraft**: Start the game using the Fabric profile. (Recommend increasing your memory whilst you are there.)
5. Play

## Compatibility

- **Minecraft Version**: **1.20.6**
- **Fabric Loader Version**: **0.16.0**
- **Fabric API Version**: **0.100.8+1.20.6**
- **Java Version**: **21**

**Note**: This mod is only compatible with the specified versions of Minecraft, Fabric Loader, Fabric API, and Java. Please ensure your environment matches these versions for optimal performance.

## Features

- **HUD Display**: Shows active boosters and their remaining time.
- **Dynamic Countdown**: Real-time countdown for boosters like "Tokens" and "Rich Pet" boosters.
- **Customizable Position**: Easily drag and drop the HUD elements to reposition them on your screen.
- **Lightweight**: Built with performance in mind, ensuring minimal impact on game performance.
- **Toggle HUD Visibility**: Press the `H` button to toggle the HUD visibility on or off.
- **Send Booster Command**: Press the `B` button to send the `/booster` command.
- **Lunar Client Support**: Now compatible with Lunar Client!
- **Configuration File**: Edit the configuration file located at `.minecraft/config/iBlockyCompanion.json` to adjust settings like HUD position and fetch intervals.

## Usage

- **Displaying Boosters**: The HUD will automatically show the currently active boosters and their remaining time.
- **Dragging the HUD**: To reposition the HUD, simply click and drag it to your desired location on the screen.
- **Modifying Configurations**: Adjust mod settings like the HUD position and fetch interval by editing the configuration file.

## Configuration

The configuration file allows you to set various options:

- **HUD Position**: Configure the X and Y coordinates of the HUD elements.
- **Fetch Interval**: Set how often the mod fetches rank information.

## Development

If you're interested in contributing or learning how the mod works:

### Prerequisites

- **Java 21**: Ensure you have JDK 21 installed.
- **Fabric Mod Development Kit (MDK)**: Download from the Fabric website.
- **IDE**: An IDE like IntelliJ IDEA or Eclipse.

### Building from Source

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Holm99/iBlockyCompanion-1.20.6.git
   cd iBlockyCompanion-1.20.6
     ```

2. **Set Up Your IDE**:
   - Import the project as a Gradle project.
   - Allow Gradle to download dependencies and set up your environment.

3. **Build the Mod**:
   ```bash
   ./gradlew build
     ```
   
4. **Find Your Build**: The mod `.jar` file will be in the `build/libs` directory.

## Contributing

Contributions are welcome! If you have suggestions or improvements, feel free to open an issue or submit a pull request. Please follow the contribution guidelines:

1. **Fork the Repository**
2. **Create a Feature Branch**
3. **Commit Your Changes**
4. **Push to Your Fork**
5. **Create a Pull Request**

## Issues

If you encounter any issues or have questions, please open an issue on GitHub. I will do our best to help you out!

---

## License
This project is licensed under the MIT License.

### MIT License
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

**THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.**

---

Thank you for using the iBlockyCompanion Mod! Enjoy your enhanced gaming experience!
