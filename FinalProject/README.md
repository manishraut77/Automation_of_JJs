# Automation of JJ's

JavaFX restaurant management/POS application for JJ's Corner.

## Run the Mac App

For Mac users who do not have Java installed, download:

```text
FinalProject/dist/Automation-of-JJs-mac.zip
```

Unzip it, then open:

```text
Automation of JJs.app
```

If macOS says Apple cannot verify the app, right-click the app, choose **Open**, then choose **Open** again. This happens because the app is not signed with an Apple Developer account.

## Run the JAR

The runnable JAR is included at:

```text
main/target/automation-of-jjs-1.0.0.jar
```

From the `FinalProject` folder, run:

```bash
java -jar main/target/automation-of-jjs-1.0.0.jar
```

If you are already inside the `main` folder, run:

```bash
java -jar target/automation-of-jjs-1.0.0.jar
```

## Requirements

Install Java JDK 25 before running the JAR.

Check Java:

```bash
java -version
```

The project currently uses Java release 25 in `main/pom.xml`:

```xml
<maven.compiler.release>25</maven.compiler.release>
```

## Build the JAR

Install Apache Maven if you want to rebuild the JAR from source.

From the `FinalProject/main` folder:

```bash
mvn clean package
```

The built JAR will be created here:

```text
main/target/automation-of-jjs-1.0.0.jar
```

## Run from Maven

Open a terminal in the downloaded `FinalProject` folder, then go into the Maven project folder:

```bash
cd main
mvn javafx:run
```

If you are already outside the folder, use the full path instead:

```bash
cd path/to/FinalProject/main
mvn javafx:run
```

## First-Time Setup

After downloading or cloning the project:

```bash
cd FinalProject/main
mvn clean compile
mvn javafx:run
```

Maven will automatically download the project dependencies:

- JavaFX Controls
- JavaFX FXML
- Jackson Databind
- Jackson JSR310 datatype support

## Default Test Accounts

These accounts are created automatically when the app starts with no saved employee data.

| Role | Employee ID | Username | Password |
| --- | --- | --- | --- |
| Manager | MGR001 | manager1 | m1n2g3 |
| Waiter | WTR001 | waiter1 | a1b2c3 |
| Waiter | WTR002 | waiter2 | a2b3c4 |
| Waiter | WTR003 | waiter3 | a3b4c5 |
| Busboy | BUS001 | busboy1 | b1c2d3 |
| Cook | CK001A | cook1 | c1d2e3 |
| Cook | CK002A | cook2 | c2d3e4 |
| Cook | CK003A | cook3 | c3d4e5 |

Use the manager account to test manager features:

```text
Username: manager1
Password: m1n2g3
```

## Notes

- Run commands from the `main` folder because that is where `pom.xml` is located.
- The first run may take longer while Maven downloads dependencies.
- Employee, menu, inventory, table, and ticket data are persisted by the app after use.
