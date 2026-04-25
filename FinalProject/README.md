# Automation of JJ's

This is our JavaFX restaurant management app for JJ's Corner. It includes screens for logging in, managing restaurant work, and testing the different employee roles.

## Running the Mac App

If you are on a Mac and do not want to install Java, use this file:

```text
FinalProject/dist/Automation-of-JJs-mac.zip
```

Unzip it and open:

```text
Automation of JJs.app
```

Mac may show a warning saying Apple cannot verify the app. If that happens, right-click `Automation of JJs.app`, click **Open**, and then click **Open** again. The warning shows up because this app was not signed with an Apple Developer account.

## Running the JAR

The runnable JAR is here:

```text
FinalProject/main/target/automation-of-jjs-1.0.0.jar
```

From the main GitHub repo folder, run:

```bash
java -jar FinalProject/main/target/automation-of-jjs-1.0.0.jar
```

If you are already inside the `FinalProject` folder, run:

```bash
java -jar main/target/automation-of-jjs-1.0.0.jar
```

The JAR needs Java JDK 25. To check your Java version:

```bash
java -version
```

## Default Login Accounts

These accounts are available when the app starts with no saved employee data.

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

The easiest account to use for testing is:

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

## Notes

- The Mac ZIP is the easiest option for Mac users because it already includes Java.
- The JAR is located in `FinalProject/main/target/`.
- If you run the JAR directly, Java JDK 25 needs to be installed.
- The app saves data after it runs, including employees, menu items, inventory, tables, and tickets.
- If saved data already exists, the default accounts may not be recreated again.
