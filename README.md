# iBlocky Booster Notification Mod

Welcome to the **iBlocky Booster Notification Mod** repository! 🎉

This mod is designed specifically for the **iBlocky Minecraft server** to enhance the player's experience by displaying active boosters and their remaining time directly on the in-game HUD.

![image](https://github.com/user-attachments/assets/c170541f-478b-441c-aad2-097b6a7c6717)![image](https://github.com/user-attachments/assets/14803cc8-58a3-4f8c-ab4d-5056c0e10958)


## Installation

Following the YouTube tutorial is probably easier 🙂
**[YouTube Tutorial](https://youtu.be/UYzXPUDZ3gw?si=zAk-UiDwzFPrTRGY)**
1. **Download the Mod**: Grab the latest release from the [Releases](https://github.com/Holm99/iblocky-boosternotification-1.20.6/releases) page.
2. **Download Fabric Loader**: Download **Fabric Loader** from [Fabric's official website - Direct Download]([https://fabricmc.net/use/](https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.exe)).
3. **Install Fabric Loader**: Open the application and select **Minecraft version 1.20.6** & **Loader Version 0.16.0**, then click install!
4. **Install Fabric API**: This mod requires **Fabric API version 0.100.8+1.20.6**. [CurseForge - Direct Download]([https://mediafilez.forgecdn.net/files/5577/501/fabric-api-0.100.8%2B1.20.6.jar]).
5. **Install Java 21**: Ensure that you have Java 21 installed. You can download it from [Oracle's official website - Direct Download](https://download.oracle.com/java/21/archive/jdk-21.0.3_windows-x64_bin.exe).
6. **Add the Mod to Your Mods Folder**: Place the two downloaded mods (Fabric API and this mod) into your (Do Windows key + R and type `%appdata%\.minecraft`) `C:\Users\yourusernamehere\AppData\Roaming\.minecraft\mods` folder. (If the mods folder does not exist, just create it)
7. **Launch Minecraft**: Start the game using the Fabric profile.

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
- **Configuration File**: Edit the configuration file located at `.minecraft/config/iblocky_boosternotification.json` to adjust settings like HUD position and fetch intervals.

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
   git clone https://github.com/Holm99/iblocky-boosternotification-1.20.6.git
   cd iblocky-boosternotification-1.20.6
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

Thank you for using the iBlocky Booster Notification Mod! Enjoy your enhanced gaming experience!
