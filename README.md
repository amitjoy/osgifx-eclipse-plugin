<!--
  Copyright 2026 Amit Kumar Mondal
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.  You may obtain a copy
  of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  License for the specific language governing permissions and limitations under
  the License.
-->
# OSGi.fx Eclipse Plugin

This repository contains the Eclipse IDE integration for the [OSGi.fx](https://github.com/amitjoy/osgifx) diagnostic tool. It provides a seamless way to manage connections and launch the OSGi.fx console directly from your Eclipse environment.

## 🚀 Key Features

- **Connection Profiles**: Easily create and manage connection profiles for both **Socket** and **MQTT** protocols.
- **Visual Status Tracking**: Real-time icons in the connection manager show success, failure, or neutral status of your profiles.
- **Auto-managed Java 25 Runtime**: Automatically downloads and configures the required **Azul Zulu FX 25** runtime on the first launch, ensuring all JavaFX modules are available without manual setup.
- **Flexible Source Selection**: Launch OSGi.fx using a specific local JAR file or by providing Maven GAV (Group:Artifact:Version) coordinates.
- **Cache Management**: Dedicated preferences to manage and clear the downloaded runtime cache.

## 📁 Project Structure

- `com.osgifx.eclipse.target`: Target platform definition for Eclipse 2022-06.
- `com.osgifx.eclipse.plugin`: The main OSGi.fx Launcher plugin.
- `com.osgifx.eclipse.feature`: The Eclipse feature project that includes the launcher plugin.
- `org.osgifx.eclipse.repository`: The P2 update site for installing the plugin.

## 🛠️ Prerequisites

- **Java 17** (Required for building the workspace).
- **Maven 3.8** or higher.

> [!NOTE]
> Even though Java 17 is required for building, the generated artifacts are compatible with **Java 11**.

## 📦 Building

To build the project, ensure you are using **Java 17** and run the following command from the root directory:

```bash
mvn clean verify -Dtycho.p2.transport=apache
```

This will:
1. Resolve target platform dependencies.
2. Compile the Java source code.
3. Generate OSGi bundles and the Eclipse feature.
4. Create the P2 update site.

### 🔏 GPG Signing
To sign the P2 repository (e.g., for official releases), use the `ossrh` profile:
```bash
mvn clean verify -Possrh
```

## 📥 Installation

Once the build is successful, you can install the plugin via the generated P2 repository:

1. In Eclipse, go to **Help > Install New Software...**.
2. Click **Add...** and then **Local...**.
3. Select the `org.osgifx.eclipse.repository/target/repository/` folder.
4. Follow the installation wizard.

## ⚙️ Configuration

Access the configuration via **Window > Preferences > OSGi.fx**.

- **Java Runtime**: 
    - Choose "Auto-manage Azul Zulu FX 25" (Recommended) to let the plugin handle the environment.
    - Or specify a "Custom Java 25 Executable" if you already have one installed.
- **OSGi.fx Source**:
    - Select "Use local OSGi.fx JAR" to point to a specific version on your disk.
    - Or provide a "Maven OSGi.fx Version (GAV)" (e.g., `com.osgifx:com.osgifx.console.application:1.0.0`) to fetch it automatically.

## 🔧 Development

The projects are fully Eclipse IDE compliant. To import:
1. Open Eclipse IDE (2022-06 or later recommended).
2. Go to `File > Import... > Maven > Existing Maven Projects`.
3. Select the root directory.
4. Open `com.osgifx.eclipse.target/osgifx.target` and click "Set as Active Target Platform".

## ⚖️ License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
