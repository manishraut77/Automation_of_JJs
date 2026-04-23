USE jjs_automation;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS refund_requests;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS menu_items;
DROP TABLE IF EXISTS inventory_items;
DROP TABLE IF EXISTS tables;
DROP TABLE IF EXISTS restaurant_tables;
DROP TABLE IF EXISTS clock_events;
DROP TABLE IF EXISTS employees;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE employees (
    employee_id CHAR(6) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('manager', 'waiter', 'busboy', 'kitchen') NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    hourly_rate DECIMAL(6,2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    login_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clock_events (
    clock_event_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id CHAR(6) NOT NULL,
    clock_in_time DATETIME NOT NULL,
    clock_out_time DATETIME NULL,
    hours_worked DECIMAL(5,2) NULL,
    work_date DATE NOT NULL,
    CONSTRAINT fk_clock_employee
        FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);

CREATE TABLE tables (
    table_id VARCHAR(2) PRIMARY KEY,
    seat_count INT NOT NULL DEFAULT 4,
    status ENUM('open', 'occupied', 'dirty', 'order_completed') NOT NULL DEFAULT 'open',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_waiter_id CHAR(6) NULL,
    CONSTRAINT fk_table_waiter
        FOREIGN KEY (assigned_waiter_id) REFERENCES employees(employee_id),
    CONSTRAINT chk_valid_table_id
        CHECK (
            table_id IN (
                'A1','A2','A3','A4','A5','A6',
                'B1','B2','B3','B4','B5','B6',
                'C5','C6',
                'D5','D6',
                'E1','E2','E3','E4','E5','E6',
                'F1','F2','F3','F4','F5','F6'
            )
        )
);

CREATE TABLE menu_items (
    menu_item_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    table_id VARCHAR(2) NOT NULL,
    server_id CHAR(6) NOT NULL,
    order_status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_order_table
        FOREIGN KEY (table_id) REFERENCES tables(table_id),
    CONSTRAINT fk_order_server
        FOREIGN KEY (server_id) REFERENCES employees(employee_id)
);

CREATE TABLE inventory_items (
    inventory_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    stock_quantity INT NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    reorder_level INT NOT NULL DEFAULT 0,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    special_notes VARCHAR(255),
    item_price DECIMAL(8,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    seat_no INT NOT NULL,
    CONSTRAINT fk_orderitem_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_orderitem_menu
        FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id)
);

CREATE TABLE refund_requests (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    requested_by CHAR(6) NOT NULL,
    approved_by CHAR(6) NULL,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reason VARCHAR(255) NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_refund_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_refund_requested_by
        FOREIGN KEY (requested_by) REFERENCES employees(employee_id),
    CONSTRAINT fk_refund_approved_by
        FOREIGN KEY (approved_by) REFERENCES employees(employee_id)
);