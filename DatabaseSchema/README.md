# DatabaseSchema

SQL schema, seed data, and a simple Java connection example for the `jjs_automation` MySQL database used by the Automation of JJ's project.

## Folder contents

- `sql/Schema.sql`: creates tables, procedures, and triggers.
- `sql/EmployeesData.sql`: seed employee records.
- `sql/TablesData.sql`: seed restaurant table records.
- `sql/MenuItemsData.sql`: seed menu data.
- `sql/ClockEventsData.sql`: seed employee clock events.
- `sql/RestData.sql`: seed inventory, orders, order items, and refund requests.
- `MySqlConnect.java`: simple JDBC connection test.

## Prerequisites

- MySQL Server 8.x installed and running
- MySQL Workbench installed
- Java 17 or newer
- MySQL Connector/J JDBC driver

## 1. Create the database in MySQL Workbench

1. Open MySQL Workbench.
2. Create or open a connection to your local MySQL server.
3. Open a new SQL tab and run:

```sql
CREATE DATABASE IF NOT EXISTS jjs_automation;
```

4. Refresh the `SCHEMAS` panel and confirm that `jjs_automation` exists.

## 2. Load the schema and seed data

Run the SQL files in this order:

1. `sql/Schema.sql`
2. `sql/EmployeesData.sql`
3. `sql/TablesData.sql`
4. `sql/MenuItemsData.sql`
5. `sql/ClockEventsData.sql`
6. `sql/RestData.sql`

`Schema.sql` starts with `USE jjs_automation;`, so the database must already exist before you run it.

Important: `sql/Schema.sql` drops and recreates objects before building the schema again. Use it only in a local/dev database unless you intentionally want to reset the schema.

## 3. Load the files in MySQL Workbench

For each file:

1. Click `File` -> `Open SQL Script...`
2. Select the SQL file.
3. Click the lightning bolt execute button.
4. Move to the next file in the order listed above.

After loading everything, you can verify the setup with:

```sql
USE jjs_automation;
SHOW TABLES;
SELECT COUNT(*) AS employees_count FROM employees;
SELECT COUNT(*) AS menu_items_count FROM menu_items;
SELECT COUNT(*) AS orders_count FROM orders;
```

## 4. Connect with Java

`MySqlConnect.java` reads these environment variables if you set them:

- `MYSQL_HOST` default: `localhost`
- `MYSQL_PORT` default: `3306`
- `MYSQL_DB` default: `jjs_automation`
- `MYSQL_USER` default: `root`
- `MYSQL_PASSWORD` default: `your_password`

### Maven dependency

If you are using Maven, add:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.3.0</version>
</dependency>
```

### Compile and run with the JDBC jar

macOS/Linux:

```bash
javac -cp .:mysql-connector-j-9.3.0.jar MySqlConnect.java
java -cp .:mysql-connector-j-9.3.0.jar MySqlConnect
```

Windows:

```bat
javac -cp .;mysql-connector-j-9.3.0.jar MySqlConnect.java
java -cp .;mysql-connector-j-9.3.0.jar MySqlConnect
```

### Example env var run

macOS/Linux:

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
java -cp .:mysql-connector-j-9.3.0.jar MySqlConnect
```

## Notes

- The sample employee seed data uses simple placeholder passwords for development.
- The schema includes procedures and triggers for order and table status synchronization.
- If you rerun `Schema.sql`, reload the seed data files afterward.
