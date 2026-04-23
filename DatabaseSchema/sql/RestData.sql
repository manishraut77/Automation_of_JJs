INSERT INTO inventory_items (item_name, category, stock_quantity, unit, reorder_level, is_active)
VALUES
('Chicken Breast', 'Proteins', 25, 'lbs', 10, TRUE),
('Pulled Pork', 'Proteins', 18, 'lbs', 8, TRUE),
('Catfish', 'Proteins', 12, 'lbs', 5, TRUE),
('Shrimp', 'Proteins', 15, 'lbs', 6, TRUE),
('Grouper', 'Proteins', 10, 'lbs', 4, TRUE),
('Ahi Tuna', 'Proteins', 8, 'lbs', 3, TRUE),
('Ground Beef', 'Proteins', 20, 'lbs', 8, TRUE),

('Romaine Lettuce', 'Produce', 12, 'heads', 4, TRUE),
('Mixed Greens', 'Produce', 10, 'bags', 4, TRUE),
('Tomatoes', 'Produce', 30, 'pcs', 10, TRUE),
('Red Onions', 'Produce', 18, 'pcs', 6, TRUE),
('Scallions', 'Produce', 10, 'bunches', 3, TRUE),
('Cilantro', 'Produce', 8, 'bunches', 3, TRUE),
('Avocados', 'Produce', 16, 'pcs', 6, TRUE),
('Mango Salsa', 'Produce', 6, 'containers', 2, TRUE),

('Cheddar Cheese', 'Dairy', 12, 'lbs', 4, TRUE),
('Pepper Jack Cheese', 'Dairy', 10, 'lbs', 4, TRUE),
('Provolone Cheese', 'Dairy', 8, 'lbs', 3, TRUE),
('Swiss Cheese', 'Dairy', 8, 'lbs', 3, TRUE),
('Blue Cheese Crumbles', 'Dairy', 6, 'lbs', 2, TRUE),

('Grits', 'Dry Goods', 20, 'bags', 6, TRUE),
('Burger Buns', 'Bread', 30, 'pcs', 10, TRUE),
('Hoagie Rolls', 'Bread', 20, 'pcs', 8, TRUE),
('Pretzel Buns', 'Bread', 20, 'pcs', 8, TRUE),
('Tea Syrup', 'Beverage', 5, 'jugs', 2, TRUE),
('Coke Syrup', 'Beverage', 4, 'boxes', 2, TRUE);

-- =========================================
-- ORDERS
-- =========================================
INSERT INTO orders (table_id, server_id, order_status, created_at, closed_at, subtotal, tax, total_amount)
VALUES
('A3', 'WTR101', 'preparing', '2026-04-20 12:05:00', NULL, 21.00, 1.47, 22.47),
('B4', 'WTR102', 'completed', '2026-04-20 12:18:00', NULL, 24.50, 1.72, 26.22),
('E1', 'WTR104', 'closed', '2026-04-20 11:40:00', '2026-04-20 12:35:00', 17.50, 1.23, 18.73),
('F3', 'WTR105', 'open', '2026-04-20 12:25:00', NULL, 15.50, 1.09, 16.59);

-- =========================================
-- ORDER ITEMS
-- =========================================
INSERT INTO order_items (order_id, menu_item_id, created_at, item_status, special_notes, item_price, quantity, seat_no)
VALUES
-- Order 1: A3
(1, (SELECT menu_item_id FROM menu_items WHERE item_name = 'House Salad'), '2026-04-20 12:06:00', 'preparing', NULL, 7.50, 1, 1),
(1, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Shrimp & Grits'), '2026-04-20 12:06:20', 'preparing', 'No extra salt', 13.50, 1, 2),

-- Order 2: B4
(2, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Chicken BLT&A'), '2026-04-20 12:19:00', 'completed', NULL, 10.00, 1, 1),
(2, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Sweet Tea'), '2026-04-20 12:19:10', 'completed', 'Less ice', 2.00, 1, 1),
(2, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Bacon Cheeseburger'), '2026-04-20 12:19:30', 'completed', 'American cheese', 11.00, 1, 2),
(2, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Curly Fries'), '2026-04-20 12:19:45', 'completed', NULL, 2.50, 1, 2),

-- Order 3: E1
(3, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Sweet Tea Fried Chicken'), '2026-04-20 11:42:00', 'completed', NULL, 11.50, 1, 1),
(3, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Sweet Tea'), '2026-04-20 11:42:20', 'completed', NULL, 2.00, 1, 1),
(3, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Curly Fries'), '2026-04-20 11:42:30', 'completed', NULL, 2.50, 1, 1),
(3, (SELECT menu_item_id FROM menu_items WHERE item_name = 'House Salad'), '2026-04-20 11:43:00', 'completed', 'Dressing on side', 7.50, 1, 2),

-- Order 4: F3
(4, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Carolina Burger'), '2026-04-20 12:26:00', 'pending', NULL, 11.00, 1, 1),
(4, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Sprite'), '2026-04-20 12:26:10', 'pending', NULL, 2.00, 1, 1),
(4, (SELECT menu_item_id FROM menu_items WHERE item_name = 'Baked Beans'), '2026-04-20 12:26:20', 'pending', NULL, 2.50, 1, 1);

-- =========================================
-- REFUND REQUESTS
-- =========================================
INSERT INTO refund_requests (order_id, requested_by, approved_by, refund_status, reason, refund_amount)
VALUES
(3, 'WTR104', 'M3N451', 'approved', 'Customer was charged for wrong item', 2.50);
