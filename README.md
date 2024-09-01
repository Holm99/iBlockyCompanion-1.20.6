# iBlocky Booster Notification Mod

Welcome to the **iBlocky Booster Notification Mod** repository! 🎉

This mod is designed specifically for the **iBlocky Minecraft server** to enhance the player's experience by displaying active boosters and their remaining time directly on the in-game HUD.

![image](https://github.com/user-attachments/assets/8eab260b-0c05-4453-a98d-d106dc325aa9)![image](https://github.com/user-attachments/assets/4e7d86c7-b645-452c-ba69-98a7cbdf10e5)


## Installation

[Youtube tutorial](https://youtu.be/UYzXPUDZ3gw?si=zAk-UiDwzFPrTRGY)
1. **Download the Mod**: Grab the latest release from the [Releases](https://github.com/Holm99/iblocky-boosternotification-1.20.6/releases) page.
2. **Install Fabric Loader**: Make sure you have **Fabric Loader 0.16.3** installed for Minecraft **1.20.6**. You can download it from [Fabric's official website](https://fabricmc.net/use/).
3. **Install Fabric API**: This mod requires **Fabric API version 0.100.8+1.20.6**. Download it from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api/files/all?page=1&pageSize=20&version=1.20.6&gameVersionTypeId=4).
4. **Install Java 21**: Ensure that you have Java 21 installed. You can download it from [Oracle's official website](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).
5. **Add the Mod to Your Mods Folder**: Place the two downloaded mods (Fabric API and my mod) into your `C:\Users\yourusernamehere\AppData\Roaming\.minecraft\mods` folder. (If the mods fodler do not exist just create it)
6. **Launch Minecraft**: Start the game using the Fabric profile.

## Compatibility

- **Minecraft Version**: **1.20.6**
- **Fabric Loader Version**: **0.16.3**
- **Fabric API Version**: **0.100.8+1.20.6**
- **Java Version**: **21**

**Note**: This mod is only compatible with the specified versions of Minecraft, Fabric Loader, Fabric API, and Java. Please ensure your environment matches these versions for optimal performance.

## Features

- **HUD Display**: Shows active boosters and their remaining time.
- **Dynamic Countdown**: Real-time countdown for boosters like "Tokens" and "Rich Pet" boosters.
- **Customizable Position**: Easily drag and drop the HUD elements to reposition them on your screen.
- **Lightweight**: Built with performance in mind, ensuring minimal impact on game performance.


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

If you encounter any issues or have questions, please open an issue on GitHub. We will do our best to help you out!


## Acknowledgements

- **Minecraft Modding Community**: For all the tutorials and support.
- **FabricMC**: For providing a flexible modding platform.

---

Thank you for using the iBlocky Booster Notification Mod! Enjoy your enhanced gaming experience!



## License

This project is licensed under the **CC0 1.0 Universal (CC0 1.0) Public Domain Dedication**.

### CC0 1.0 Universal (CC0 1.0) Public Domain Dedication

The person who associated a work with this deed has dedicated the work to the public domain by waiving all of their rights to the work worldwide under copyright law, including all related and neighboring rights, to the extent allowed by law.

You can copy, modify, distribute, and perform the work, even for commercial purposes, all without asking permission.

#### Other Information

In no way are the patent or trademark rights of any person affected by CC0, nor are the rights that other persons may have in the work or in how the work is used, such as publicity or privacy rights.

Unless expressly stated otherwise, the person who associated a work with this deed makes no warranties about the work, and disclaims liability for all uses of the work, to the fullest extent permitted by applicable law.

When using or citing the work, you should not imply endorsement by the author or the affirmer.

For more details, please refer to the full [Creative Commons Legal Code](https://creativecommons.org/publicdomain/zero/1.0/legalcode).

---

Thank you for using the iBlocky Booster Notification Mod! Enjoy your enhanced gaming experience!

