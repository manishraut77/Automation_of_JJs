package com.jjcorner.app.service;

import com.jjcorner.app.model.MenuCategory;
import com.jjcorner.app.model.MenuItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MenuService {
    private final List<MenuItem> items = List.of(
            // Appetizers
            new MenuItem("APP-001", "Chicken Nachos", MenuCategory.APPETIZERS, new BigDecimal("8.50"),
                    "Pulled chicken, spicy white cheese sauce, & cheddar cheese topped with red onions & cilantro. Add BBQ sauce (+$0.50)"),
            new MenuItem("APP-002", "Pork Nachos", MenuCategory.APPETIZERS, new BigDecimal("8.50"),
                    "Pulled pork, spicy white cheese sauce, & pepper jack cheese topped with tomato, scallions & cilantro. Add BBQ sauce (+$0.50)"),
            new MenuItem("APP-003", "Pork or Chicken Sliders (3)", MenuCategory.APPETIZERS, new BigDecimal("5.00"),
                    "Sauces: Chipotle, Jim Beam, or Carolina Gold BBQ"),
            new MenuItem("APP-004", "Catfish Bites", MenuCategory.APPETIZERS, new BigDecimal("6.50"),
                    "Catfish pieces cornmeal-battered & fried. Served with lemon & spicy cocktail sauce"),
            new MenuItem("APP-005", "Fried Veggies", MenuCategory.APPETIZERS, new BigDecimal("6.50"),
                    "Choice of okra, zucchini, squash, or Mix & Match. Served with a side of spicy ranch"),

            // Salads
            new MenuItem("SAL-001", "House Salad", MenuCategory.SALADS, new BigDecimal("7.50"),
                    "Mixed greens, topped with bacon, tomato & blue cheese crumbles"),
            new MenuItem("SAL-002", "Wedge Salad", MenuCategory.SALADS, new BigDecimal("7.50"),
                    "Iceberg lettuce wedge topped with bacon, tomato & blue cheese crumbles"),
            new MenuItem("SAL-003", "Caesar Salad", MenuCategory.SALADS, new BigDecimal("7.50"),
                    "Romaine lettuce, shredded Parmesan cheese & croutons tossed in Caesar dressing"),
            new MenuItem("SAL-004", "Sweet Potato Chicken Salad", MenuCategory.SALADS, new BigDecimal("11.50"),
                    "Mixed greens, red onion, dried cranberries & goat cheese crumbles topped with chilled sweet potato crusted chicken"),

            // Entrees (served with 2 sides)
            new MenuItem("ENT-001", "Shrimp & Grits", MenuCategory.ENTREES, new BigDecimal("13.50"),
                    "Sautéed shrimp with garlic served on top of cheese grits, topped with sautéed peppers & onions"),
            new MenuItem("ENT-002", "Sweet Tea Fried Chicken", MenuCategory.ENTREES, new BigDecimal("11.50"),
                    "Fried chicken breast marinated in sweet tea & spices, topped with a sweet tea reduction"),
            new MenuItem("ENT-003", "Caribbean Chicken", MenuCategory.ENTREES, new BigDecimal("11.50"),
                    "Grilled chicken marinated in spicy Caribbean seasoning topped with mango salsa & avocado"),
            new MenuItem("ENT-004", "Grilled Pork Chops", MenuCategory.ENTREES, new BigDecimal("11.00"),
                    "Two bone-in grilled pork chops"),
            new MenuItem("ENT-005", "New York Strip Steak", MenuCategory.ENTREES, new BigDecimal("17.00"),
                    "Cut in-house. Cooked to your desired temperature"),
            new MenuItem("ENT-006", "Seared Tuna", MenuCategory.ENTREES, new BigDecimal("15.00"),
                    "Seared ahi tuna cooked to your desired temperature, topped with mango salsa & a honey lime vinaigrette drizzle"),
            new MenuItem("ENT-007", "Captain Crunch Chicken Tenders", MenuCategory.ENTREES, new BigDecimal("11.50"),
                    "Fried chicken tenders coated in Captain Crunch with a dipping sauce"),
            new MenuItem("ENT-008", "Shock Top Grouper Fingers", MenuCategory.ENTREES, new BigDecimal("11.50"),
                    "Shock Top beer-battered grouper served with tartar sauce & a lemon extra"),
            new MenuItem("ENT-009", "Mac & Cheese Bar", MenuCategory.ENTREES, new BigDecimal("8.50"),
                    "Cast iron skillet of mac & cheese (regular or spicy cheese) with choice of two toppings. Toppings: Pepper Jack, Cheddar, Swiss, Mozzarella, Goat Cheese, Bacon, Broccoli, Mushrooms, Grilled Onions, Jalapeños, Spinach, Tomatoes"),

            // Sides (ordered separately $2.50)
            new MenuItem("SID-001", "Curly Fries", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-002", "Wing Chips", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-003", "Sweet Potato Fries", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-004", "Creamy Cabbage Slaw", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-005", "Adluh Cheese Grits", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-006", "Mashed Potatoes", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-007", "Mac & Cheese", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-008", "Seasonal Vegetables", MenuCategory.SIDES, new BigDecimal("2.50")),
            new MenuItem("SID-009", "Baked Beans", MenuCategory.SIDES, new BigDecimal("2.50")),

            // Sandwiches
            new MenuItem("SND-001", "Grilled Cheese", MenuCategory.SANDWICHES, new BigDecimal("5.50"),
                    "American cheese served on multigrain or white bread"),
            new MenuItem("SND-002", "Chicken BLT&A", MenuCategory.SANDWICHES, new BigDecimal("10.00"),
                    "Grilled chicken, bacon, lettuce, tomato & avocado on a pretzel bun"),
            new MenuItem("SND-003", "Philly", MenuCategory.SANDWICHES, new BigDecimal("13.50"),
                    "Choice of shaved New York Strip steak or grilled chicken topped with mushrooms, peppers, onions & provolone cheese on a hoagie"),
            new MenuItem("SND-004", "Club", MenuCategory.SANDWICHES, new BigDecimal("10.00"),
                    "Ham, turkey, Swiss cheddar, lettuce, tomato, mayo & bacon on multigrain bread"),
            new MenuItem("SND-005", "Meatball Sub", MenuCategory.SANDWICHES, new BigDecimal("10.00"),
                    "House-made meatballs topped with marinara & mozzarella cheese. Sautéed pepper & onions on request"),

            // Burgers
            new MenuItem("BRG-001", "Bacon Cheeseburger", MenuCategory.BURGERS, new BigDecimal("11.00"),
                    "8-ounce burger topped with bacon & your choice of cheddar, American, Swiss, provolone, pepper jack, blue cheese, or pimento cheese on a brioche bun"),
            new MenuItem("BRG-002", "Carolina Burger", MenuCategory.BURGERS, new BigDecimal("11.00"),
                    "8-ounce burger topped with chili, diced onions & slaw on a brioche bun"),
            new MenuItem("BRG-003", "Portobello Burger (V)", MenuCategory.BURGERS, new BigDecimal("8.50"),
                    "Marinated Portobello mushroom cap topped with mango salsa, lettuce, tomato & onion on a telera bun"),
            new MenuItem("BRG-004", "Vegan Boca Burger (V)", MenuCategory.BURGERS, new BigDecimal("10.50"),
                    "Vegan Boca Burger topped with lettuce, tomato & onion on a telera bun"),

            // Beverages (all $2.00)
            new MenuItem("BEV-001", "Sweet / Unsweetened Tea", MenuCategory.BEVERAGES, new BigDecimal("2.00")),
            new MenuItem("BEV-002", "Coke / Diet Coke", MenuCategory.BEVERAGES, new BigDecimal("2.00")),
            new MenuItem("BEV-003", "Sprite", MenuCategory.BEVERAGES, new BigDecimal("2.00")),
            new MenuItem("BEV-004", "Bottled Water", MenuCategory.BEVERAGES, new BigDecimal("2.00")),
            new MenuItem("BEV-005", "Lemonade", MenuCategory.BEVERAGES, new BigDecimal("2.00")),
            new MenuItem("BEV-006", "Orange Juice", MenuCategory.BEVERAGES, new BigDecimal("2.00"))
    );

    public List<MenuItem> allItems() {
        return items;
    }

    public List<MenuItem> byCategory(MenuCategory category) {
        return items.stream().filter(i -> i.category() == category).collect(Collectors.toList());
    }

    public Optional<MenuItem> itemById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return items.stream().filter(i -> i.id().equalsIgnoreCase(id)).findFirst();
    }
}

