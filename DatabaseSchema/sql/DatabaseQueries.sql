USE jjs_automation;


-- all of these are ready for PreparedStatement with ? params


-- =========================
-- login / employees
-- =========================

-- grab employee row for login
-- 1 = employee_id
SELECT employee_id, first_name, last_name, role, password_hash,
       hourly_rate, is_active, login_attempts
FROM employees
WHERE employee_id = ?;

-- bad login count goes up
-- 1 = employee_id
UPDATE employees
SET login_attempts = login_attempts + 1
WHERE employee_id = ?;

-- reset bad login count after good login / manager unlock
-- 1 = employee_id
UPDATE employees
SET login_attempts = 0
WHERE employee_id = ?;

-- manager employee list
SELECT employee_id, first_name, last_name, role, hourly_rate,
       is_active, login_attempts, created_at
FROM employees
ORDER BY role, employee_id;

-- add employee from manager screen
-- 1=id 2=first 3=last 4=role 5=password_hash 6=hourly_rate 7=is_active 8=login_attempts
INSERT INTO employees (
    employee_id, first_name, last_name, role, password_hash,
    hourly_rate, is_active, login_attempts
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- edit employee
-- 1=first 2=last 3=role 4=password_hash 5=hourly_rate 6=is_active 7=login_attempts 8=employee_id
UPDATE employees
SET first_name = ?,
    last_name = ?,
    role = ?,
    password_hash = ?,
    hourly_rate = ?,
    is_active = ?,
    login_attempts = ?
WHERE employee_id = ?;

-- soft delete employee basically
-- 1 = employee_id
UPDATE employees
SET is_active = FALSE
WHERE employee_id = ?;


-- =========================
-- clock in / out
-- =========================

-- check if this employee already has open shift
-- 1 = employee_id
SELECT clock_event_id, employee_id, clock_in_time, clock_out_time, hours_worked, work_date
FROM clock_events
WHERE employee_id = ?
  AND clock_out_time IS NULL
ORDER BY clock_in_time DESC
LIMIT 1;

-- clock in
-- 1=employee_id 2=clock_in_time 3=work_date
INSERT INTO clock_events (employee_id, clock_in_time, work_date)
VALUES (?, ?, ?);

-- clock out
-- 1=clock_out_time 2=hours_worked 3=clock_event_id
UPDATE clock_events
SET clock_out_time = ?,
    hours_worked = ?
WHERE clock_event_id = ?;

-- manager looking at one employee time history
-- 1 = employee_id
SELECT clock_event_id, employee_id, clock_in_time, clock_out_time, hours_worked, work_date
FROM clock_events
WHERE employee_id = ?
ORDER BY clock_in_time DESC;


-- =========================
-- tables / floor plan
-- =========================

-- floor plan load
SELECT table_id, seat_count, status, is_active, assigned_waiter_id
FROM tables
WHERE is_active = TRUE
ORDER BY table_id;

-- waiter my tables
-- 1 = waiter_id
SELECT table_id, seat_count, status, is_active, assigned_waiter_id
FROM tables
WHERE assigned_waiter_id = ?
ORDER BY table_id;

-- one table
-- 1 = table_id
SELECT table_id, seat_count, status, is_active, assigned_waiter_id
FROM tables
WHERE table_id = ?;

-- manager assigns waiter to table
-- 1 = waiter_id 2 = table_id
UPDATE tables
SET assigned_waiter_id = ?
WHERE table_id = ?;

-- change table color/status
-- 1 = status 2 = table_id
UPDATE tables
SET status = ?
WHERE table_id = ?;


-- =========================
-- menu
-- =========================

-- waiter menu load
SELECT menu_item_id, item_name, category, price, is_available, description
FROM menu_items
WHERE is_available = TRUE
ORDER BY category, item_name;

-- waiter category tab
-- 1 = category
SELECT menu_item_id, item_name, category, price, is_available, description
FROM menu_items
WHERE category = ?
  AND is_available = TRUE
ORDER BY item_name;

-- manager menu editor list
SELECT menu_item_id, item_name, category, price, is_available, description
FROM menu_items
ORDER BY category, item_name;

-- add menu item
-- 1=name 2=category 3=price 4=is_available 5=description
INSERT INTO menu_items (item_name, category, price, is_available, description)
VALUES (?, ?, ?, ?, ?);

-- edit menu item
-- 1=name 2=category 3=price 4=is_available 5=description 6=menu_item_id
UPDATE menu_items
SET item_name = ?,
    category = ?,
    price = ?,
    is_available = ?,
    description = ?
WHERE menu_item_id = ?;

-- quick toggle for sold out / back in stock
-- 1=is_available 2=menu_item_id
UPDATE menu_items
SET is_available = ?
WHERE menu_item_id = ?;


-- =========================
-- orders
-- =========================

-- check if table already has live order
-- 1 = table_id
SELECT order_id, table_id, server_id, order_status, created_at, closed_at,
       subtotal, tax, total_amount
FROM orders
WHERE table_id = ?
  AND order_status <> 'closed'
ORDER BY created_at DESC
LIMIT 1;

-- waiter opens order
-- 1=table_id 2=server_id 3=order_status 4=subtotal 5=tax 6=total_amount
INSERT INTO orders (
    table_id, server_id, order_status, subtotal, tax, total_amount
)
VALUES (?, ?, ?, ?, ?, ?);

-- update totals whenever items change
-- 1=subtotal 2=tax 3=total_amount 4=order_id
UPDATE orders
SET subtotal = ?,
    tax = ?,
    total_amount = ?
WHERE order_id = ?;

-- change overall order status
-- 1=order_status 2=order_id
UPDATE orders
SET order_status = ?
WHERE order_id = ?;

-- close order after payment
-- 1=subtotal 2=tax 3=total_amount 4=order_id
UPDATE orders
SET order_status = 'closed',
    closed_at = CURRENT_TIMESTAMP,
    subtotal = ?,
    tax = ?,
    total_amount = ?
WHERE order_id = ?;

-- waiter order history
-- 1 = waiter_id
SELECT order_id, table_id, server_id, order_status, created_at, closed_at,
       subtotal, tax, total_amount
FROM orders
WHERE server_id = ?
ORDER BY created_at DESC;

-- manager all orders view
SELECT order_id, table_id, server_id, order_status, created_at, closed_at,
       subtotal, tax, total_amount
FROM orders
ORDER BY created_at DESC;


-- =========================
-- order items
-- =========================

-- all items in one ticket
-- 1 = order_id
SELECT oi.order_item_id, oi.order_id, oi.menu_item_id, oi.created_at, oi.item_status,
       oi.special_notes, oi.item_price, oi.quantity, oi.seat_no,
       mi.item_name, mi.category
FROM order_items oi
JOIN menu_items mi ON mi.menu_item_id = oi.menu_item_id
WHERE oi.order_id = ?
ORDER BY oi.created_at, oi.order_item_id;

-- add line to ticket
-- 1=order_id 2=menu_item_id 3=item_status 4=special_notes 5=item_price 6=quantity 7=seat_no
INSERT INTO order_items (
    order_id, menu_item_id, item_status, special_notes, item_price, quantity, seat_no
)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- change item qty
-- 1=quantity 2=order_item_id
UPDATE order_items
SET quantity = ?
WHERE order_item_id = ?;

-- change notes
-- 1=special_notes 2=order_item_id
UPDATE order_items
SET special_notes = ?
WHERE order_item_id = ?;

-- remove one item from ticket
-- 1=order_item_id
DELETE FROM order_items
WHERE order_item_id = ?;

-- item status update
-- 1=item_status 2=order_item_id
UPDATE order_items
SET item_status = ?
WHERE order_item_id = ?;

-- mark all pending items as preparing when order goes to kitchen
-- 1=order_id
UPDATE order_items
SET item_status = 'preparing'
WHERE order_id = ?
  AND item_status = 'pending';

-- kitchen-ish / order status sync
-- 1 = order_id
CALL sync_order_status_for_order(?);


-- =========================
-- refunds
-- =========================

-- waiter submits refund request
-- 1=order_id 2=requested_by 3=approved_by 4=refund_status 5=reason 6=refund_amount
INSERT INTO refund_requests (
    order_id, requested_by, approved_by, refund_status, reason, refund_amount
)
VALUES (?, ?, ?, ?, ?, ?);

-- manager refund queue
SELECT rr.refund_id, rr.order_id, rr.requested_by, rr.approved_by,
       rr.refund_status, rr.reason, rr.refund_amount,
       o.table_id, o.server_id
FROM refund_requests rr
JOIN orders o ON o.order_id = rr.order_id
WHERE rr.refund_status = 'pending'
ORDER BY rr.refund_id ASC;

-- approve or deny refund
-- 1=refund_status 2=approved_by 3=refund_id
UPDATE refund_requests
SET refund_status = ?,
    approved_by = ?
WHERE refund_id = ?;

-- refund history for one order
-- 1 = order_id
SELECT refund_id, order_id, requested_by, approved_by, refund_status, reason, refund_amount
FROM refund_requests
WHERE order_id = ?
ORDER BY refund_id DESC;


-- =========================
-- inventory
-- =========================

-- manager inventory screen
SELECT inventory_id, item_name, category, stock_quantity, unit,
       reorder_level, last_updated, is_active
FROM inventory_items
WHERE is_active = TRUE
ORDER BY category, item_name;

-- low stock stuff
SELECT inventory_id, item_name, category, stock_quantity, unit,
       reorder_level, last_updated, is_active
FROM inventory_items
WHERE is_active = TRUE
  AND stock_quantity <= reorder_level
ORDER BY stock_quantity ASC, item_name ASC;

-- add inventory item
-- 1=name 2=category 3=stock 4=unit 5=reorder_level 6=is_active
INSERT INTO inventory_items (
    item_name, category, stock_quantity, unit, reorder_level, is_active
)
VALUES (?, ?, ?, ?, ?, ?);

-- edit inventory item / stock amount
-- 1=name 2=category 3=stock 4=unit 5=reorder_level 6=is_active 7=inventory_id
UPDATE inventory_items
SET item_name = ?,
    category = ?,
    stock_quantity = ?,
    unit = ?,
    reorder_level = ?,
    is_active = ?,
    last_updated = CURRENT_TIMESTAMP
WHERE inventory_id = ?;


-- =========================
-- simple manager reports
-- =========================

-- sales by waiter for date range
-- 1=start_time 2=end_time
SELECT e.employee_id, e.first_name, e.last_name,
       COUNT(o.order_id) AS closed_orders,
       ROUND(COALESCE(SUM(o.total_amount), 0), 2) AS total_sales
FROM employees e
LEFT JOIN orders o
       ON o.server_id = e.employee_id
      AND o.order_status = 'closed'
      AND o.closed_at >= ?
      AND o.closed_at < ?
WHERE e.role = 'waiter'
GROUP BY e.employee_id, e.first_name, e.last_name
ORDER BY total_sales DESC;

-- menu popularity for date range
-- 1=start_time 2=end_time
SELECT mi.menu_item_id, mi.item_name, mi.category,
       SUM(oi.quantity) AS total_sold,
       ROUND(SUM(oi.quantity * oi.item_price), 2) AS revenue
FROM order_items oi
JOIN orders o ON o.order_id = oi.order_id
JOIN menu_items mi ON mi.menu_item_id = oi.menu_item_id
WHERE o.order_status = 'closed'
  AND o.closed_at >= ?
  AND o.closed_at < ?
GROUP BY mi.menu_item_id, mi.item_name, mi.category
ORDER BY total_sold DESC, revenue DESC;

-- hours worked by employee for date range
-- 1=start_date 2=end_date
SELECT e.employee_id, e.first_name, e.last_name, e.role,
       ROUND(COALESCE(SUM(ce.hours_worked), 0), 2) AS total_hours
FROM employees e
LEFT JOIN clock_events ce
       ON ce.employee_id = e.employee_id
      AND ce.work_date BETWEEN ? AND ?
GROUP BY e.employee_id, e.first_name, e.last_name, e.role
ORDER BY e.role, e.employee_id;
