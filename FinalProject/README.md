# Automation of JJ's

Automation of JJ's is a JavaFX restaurant management application for JJ's Corner. The app supports restaurant staff workflows for logging in, managing tables, creating and updating orders, handling checkout, maintaining menu and inventory data, and using role-specific home screens.

## Special Remarks

- The main project folder is `FinalProject/`.
- The runnable JAR is included at `FinalProject/main/target/automation-of-jjs-1.0.0.jar`.
- Running the JAR directly requires Java JDK 25.
- The app saves employee, menu, inventory, table, and ticket data after it runs.
- Default accounts are created when the app starts with no saved employee data. If saved data already exists from a previous run, the app may use the saved data instead.
- A Mac app ZIP may be provided at `FinalProject/dist/Automation-of-JJs-mac.zip` for Mac users who do not want to install Java separately.

## Default Login Accounts

These accounts are available for testing when the app starts with no saved employee data.

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

Check your Java version with:

```bash
java -version
```

## Running with Maven

If Maven is installed, you can run the app from the Maven project folder:

```bash
cd FinalProject/main
mvn javafx:run
```

You can rebuild the runnable JAR with:

```bash
cd FinalProject/main
mvn package
```

The packaged JAR will be written to:

```text
FinalProject/main/target/automation-of-jjs-1.0.0.jar
```

## Running the Mac App

If the Mac app ZIP is included with the submission, unzip:

```text
FinalProject/dist/Automation-of-JJs-mac.zip
```

Then open:

```text
Automation of JJs.app
```

macOS may show a warning saying Apple cannot verify the app. If that happens, right-click `Automation of JJs.app`, click **Open**, and then click **Open** again. The warning appears because the app is not signed with an Apple Developer account.

## Project Approach

The app is organized as a JavaFX/Maven project. Java source code is under `FinalProject/main/java`, and FXML screens, stylesheets, and image resources are under `FinalProject/main/resources`.

The project separates the main responsibilities into packages:

| Package | Responsibility |
| --- | --- |
| `model` | Main data objects, including employees, tables, tickets, menu items, ticket items, and payments. |
| `service` | Application logic for authentication, sessions, orders, menus, inventory, tables, activity records, and clocks. |
| `controller` | JavaFX controller classes that connect FXML screens to application logic. |
| `persist` | Saving and loading local data so the app can remember changes after it closes. |
| `nav` | Scene and screen navigation. |
| `ui` | Custom JavaFX screens, checkout views, and dialogs. |
| `util` | Shared helpers and reusable UI utilities. |

The goal was to keep the application usable as a restaurant workflow while keeping the code organized enough to follow, test, and extend.

## Repository Structure

```text
FinalProject/
|-- README.md
`-- main/
    |-- pom.xml
    |-- java/
    |   `-- com/jjcorner/app/
    |       |-- App.java
    |       |-- Launcher.java
    |       |-- controller/
    |       |-- model/
    |       |-- nav/
    |       |-- persist/
    |       |-- service/
    |       |-- ui/
    |       `-- util/
    |-- resources/
    |   `-- com/jjcorner/view/
    |       |-- Login.fxml
    |       |-- SignUp.fxml
    |       |-- Welcome.fxml
    |       |-- busboy/
    |       |-- manager/
    |       |-- waiter/
    |       |-- jj-logo.png
    |       `-- styles.css
    `-- target/
        `-- automation-of-jjs-1.0.0.jar
```

## Notes

- The JAR is located in `FinalProject/main/target/`.
- If you run the JAR directly, Java JDK 25 must be installed.
- The Maven project uses JavaFX 21.0.6 and Jackson 2.17.2.
- The app persists local data after it runs, including employees, menu items, inventory, tables, and tickets.
- If saved employee data already exists, default accounts may not be recreated again.
