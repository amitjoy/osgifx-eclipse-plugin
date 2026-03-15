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

This repository contains the Eclipse IDE integration for the OSGi.fx diagnostic tool. It is built as an Eclipse RCP plugin and feature, targeting **Java 17** and **Eclipse 2022-06**.

## Project Structure

- `com.osgifx.eclipse.target`: Target platform definition for Eclipse 2022-06.
- `com.osgifx.eclipse.plugin`: The main OSGi.fx Launcher plugin.
- `com.osgifx.eclipse.feature`: The Eclipse feature project that includes the launcher plugin.

## Prerequisites

- **Java 17** or higher.
- **Maven 3.8** or higher.

## Building

To build the project, run the following command from the root directory:

```bash
mvn clean verify
```

This will:
1. Resolve the target platform dependencies.
2. Compile the Java source code (Java 17).
3. Generate the OSGi bundles (plugins).
4. Create the Eclipse feature.

## Installation

Once the build is successful, you can find the generated JARs in the `target` directories of the respective projects:
- Feature: `com.osgifx.eclipse.feature/target/com.osgifx.eclipse.feature-8.0.0-SNAPSHOT.jar`
- Plugin: `com.osgifx.eclipse.plugin/target/com.osgifx.eclipse.plugin-8.0.0-SNAPSHOT.jar`

## Development

The projects are Eclipse IDE compliant. To import them:
1. Open Eclipse IDE (2022-06 or later recommended).
2. Go to `File > Import... > Maven > Existing Maven Projects`.
3. Select the root directory of this repository.
4. Set the target platform by opening `com.osgifx.eclipse.target/osgifx.target` and clicking "Set as Active Target Platform" in the editor.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
