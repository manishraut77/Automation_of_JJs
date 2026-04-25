# Automation of JJ's

This project is a JavaFX restaurant management app for JJ's Corner. It was built as a desktop application, with separate workflows for restaurant roles like manager, waiter, busboy, and cook.

## Special Remarks

- The main project folder is `FinalProject/`.
- The runnable JAR is already included in the repo at `FinalProject/main/target/automation-of-jjs-1.0.0.jar`.
- A Mac app ZIP is also included at `FinalProject/dist/Automation-of-JJs-mac.zip`.
- The Mac app ZIP is the easiest option for Mac users because it includes its own Java runtime.
- Running the JAR directly requires Java JDK 25.
- The app saves employee, menu, inventory, table, and ticket data after it runs.
- These default accounts are created when the app starts with no saved employee data. If saved data already exists from a previous run, the app may use that saved data instead.

## Existing Username and Password Combinations

These accounts are available when the app starts with no saved employee data.

| Role | Employee ID | Username | Password |
| --- | --- | --- | --- |
| Manager | MGR001 | manager | m1n2g3 |
| Waiter | WTR001 | waiter1 | a1b2c3 |
| Waiter | WTR002 | waiter2 | a2b3c4 |
| Waiter | WTR003 | waiter3 | a3b4c5 |
| Busboy | BUS001 | busboy1 | b1c2d3 |
| Cook | CK001A | cook1 | c1d2e3 |
| Cook | CK002A | cook2 | c2d3e4 |
| Cook | CK003A | cook3 | c3d4e5 |

The easiest account to use for testing is:

```text
Username: manager
Password: m1n2g3
```

## How to Run the JAR

The JAR file is located here:

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

To check whether Java is installed:

```bash
java -version
```

## How to Run the Mac App

For Mac users, the packaged app is here:

```text
FinalProject/dist/Automation-of-JJs-mac.zip
```

Direct GitHub link:

```text
https://github.com/manishraut77/Automation_of_JJs/blob/main/FinalProject/dist/Automation-of-JJs-mac.zip
```

Unzip it, then open:

```text
Automation of JJs.app
```

Mac may show a warning saying Apple cannot verify the app. If that happens, right-click `Automation of JJs.app`, click **Open**, and then click **Open** again. This happens because the app is not signed with an Apple Developer account.

## Repo Structure

```text
Automation_of_JJs/
|-- README.md
|-- FinalProject/
|   |-- README.md
|   |-- README.docx
|   |-- dist/
|   |   `-- Automation-of-JJs-mac.zip
|   `-- main/
|       |-- pom.xml
|       |-- java/
|       |   `-- com/jjcorner/app/
|       |-- resources/
|       |   `-- com/jjcorner/view/
|       `-- target/
|           `-- automation-of-jjs-1.0.0.jar
`-- DatabaseSchema/
```

## Project Approach

The app is organized like a normal JavaFX/Maven project. The Java code is under `FinalProject/main/java`, and the FXML screens, stylesheet, and image resources are under `FinalProject/main/resources`.

The project separates the main responsibilities into packages:

- `model` contains the main data objects, like employees, tables, tickets, menu items, and payments.
- `service` contains the app logic for authentication, orders, menu data, inventory, tables, sessions, and activity records.
- `controller` connects the FXML screens to the app logic.
- `persist` handles saving and loading data so the app can remember changes after it closes.
- `nav` manages switching between screens.
- `ui` contains custom JavaFX UI screens and dialogs.

The goal was to keep the app usable as a restaurant workflow demo while still keeping the code organized enough to follow and extend.
