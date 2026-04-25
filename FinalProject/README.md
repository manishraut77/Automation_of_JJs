# Automation of JJ's

JavaFX restaurant management/POS application for JJ's Corner.

## How to Run the App on Mac

For Mac users who do not have Java installed, use the packaged Mac app:

```text
FinalProject/dist/Automation-of-JJs-mac.zip
```

Download and unzip `Automation-of-JJs-mac.zip`, then open:

```text
Automation of JJs.app
```

Special note for Mac: if macOS says Apple cannot verify the app, right-click `Automation of JJs.app`, choose **Open**, then choose **Open** again. This happens because the app is not signed with an Apple Developer account.

## How to Run the JAR

The runnable JAR file is located here:

```text
FinalProject/main/target/automation-of-jjs-1.0.0.jar
```

From the repository root, run:

```bash
java -jar FinalProject/main/target/automation-of-jjs-1.0.0.jar
```

From inside the `FinalProject` folder, run:

```bash
java -jar main/target/automation-of-jjs-1.0.0.jar
```

Special note for the JAR: Java JDK 25 is required to run the JAR directly. Check Java with:

```bash
java -version
```

## Default Accounts

These accounts already exist when the app starts with no saved employee data.

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

Recommended test login:

```text
Username: manager1
Password: m1n2g3
```

## Folder Structure

```text
FinalProject/
|-- README.md
|-- dist/
|   `-- Automation-of-JJs-mac.zip
`-- main/
    |-- pom.xml
    |-- java/
    |   `-- com/jjcorner/app/
    |-- resources/
    |   `-- com/jjcorner/view/
    `-- target/
        `-- automation-of-jjs-1.0.0.jar
```

## Special Remarks

- The Mac app ZIP includes its own Java runtime.
- The JAR is the main executable file for users who already have Java installed.
- The app saves employee, menu, inventory, table, and ticket data after use.
- If data files already exist from a previous run, the default accounts may not be recreated.
