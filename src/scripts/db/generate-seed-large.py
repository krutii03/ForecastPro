#!/usr/bin/env python3
"""Generate scripts/db/seed-large.sql with realistic demo data."""

from __future__ import annotations

import math
import random
from calendar import monthrange
from datetime import date, datetime, timedelta
from pathlib import Path

random.seed(42)

OUT = Path(__file__).resolve().parent / "seed-large.sql"

# Ideal row counts for demo dataset
INVENTORY_COUNT = 30
STOCK_REQUEST_COUNT = 20
MESSAGE_COUNT = 15
FORECAST_MONTHS_COUNT = 3  # × 15 products = 45 forecasts (target 30–60)
SALES_HISTORY_MONTHS = 18


def last_n_month_range(months: int, as_of: date | None = None) -> tuple[date, date]:
    """Inclusive range: first day of month (months-1) ago → last day of as_of month."""
    today = as_of or date.today()
    end = date(today.year, today.month, monthrange(today.year, today.month)[1])
    y, m = today.year, today.month
    m -= months - 1
    while m <= 0:
        m += 12
        y -= 1
    start = date(y, m, 1)
    return start, end


def vendor_for_product(product_id: int) -> int:
    """Map product to specialist vendor by category (ids 1–3); else general vendor 4."""
    category_id = PRODUCTS[product_id - 1][1]
    if category_id == 1:
        return 1
    if category_id == 2:
        return 2
    if category_id == 3:
        return 3
    return 4


def vendor_name_for_product(product_id: int) -> str:
    vendor_id = vendor_for_product(product_id)
    return VENDORS[vendor_id - 1][0]


START, END = last_n_month_range(SALES_HISTORY_MONTHS)

CATEGORIES = ["Furniture", "Home Appliances", "Electronics"]

PRODUCTS = [
    # Furniture (category 1)
    ("Oak Desk", 1, 1299.00, 120, "furniture_slow"),
    ("Chair", 1, 349.50, 280, "chair_high"),
    ("Dining Table", 1, 899.00, 90, "dining_stable"),
    ("Sofa Set", 1, 2499.00, 55, "sofa_summer"),
    ("Bed Frame", 1, 1199.00, 75, "furniture_slow"),
    # Home Appliances (category 2)
    ("Refrigerator", 2, 899.00, 110, "fridge_moderate"),
    ("Microwave", 2, 159.00, 6, "microwave_steady"),
    ("Air Conditioner", 2, 3299.00, 65, "ac_summer"),
    ("Ceiling Fan", 2, 249.00, 140, "fan_summer"),
    ("Water Heater", 2, 449.00, 95, "heater_winter"),
    # Electronics (category 3)
    ("Laptop", 3, 1199.00, 5, "laptop_steady"),
    ("Smartphone", 3, 799.00, 12, "phone_fast"),
    ("Smart Watch", 3, 299.00, 8, "watch_fast"),
    ("Bluetooth Speaker", 3, 149.00, 7, "speaker_seasonal"),
    ("Gaming Console", 3, 499.00, 3, "console_holiday"),
]

# Latest-month forecast targets for demo inventory alerts (stock set low above)
ALERT_FORECAST_TARGETS = {
    "Laptop": 35.0,
    "Smartphone": 50.0,
    "Gaming Console": 20.0,
    "Bluetooth Speaker": 30.0,
    "Microwave": 18.0,
}

VENDOR_HASH = "$2a$10$DOEkYkpLRDPVrlcY9bjJCuuYq6yuSTFmb0prI/Ccl7POO2wzYQ/3q"
ADMIN_HASH = "$2a$10$0oRlWEw2lqIIKrFM9LrB4.WHx5Dj9XtnEm8mydnmPuWjD5Ww0Sb2K"
MANAGER_HASH = "$2a$10$WpmOaDE8COuLZR5u195q5ewMhSMg4o5qjTmaW.B0HEhAcrE6XeGQG"
EMPLOYEE_HASH = "$2a$10$m1r2gXS.t5kjGtAAshaFr.9vcDAjN3bUVXaysx91SbxjiF3PN12oO"

# vendor user_id → supplies category_id (1=Furniture, 2=Home Appliances, 3=Electronics)
VENDOR_CATEGORY = {4: 1, 5: 2, 6: 3, 7: 4}  # user_id → category (4 = general supply)

VENDORS = [
    ("CraftWood Furniture", "contact@craftwood.com", 4),       # vendor1 — Furniture
    ("HomeComfort Appliances", "sales@homecomfort.com", 5),    # vendor2 — Home Appliances
    ("ElectroLink Distributors", "orders@electrolink.com", 6),  # vendor3 — Electronics
    ("Summit General Supply", "info@summitsupply.com", 7),     # vendor4 — mixed / overflow
]

INVENTORY_SOURCES = [
    "Initial Stock",
    "Vendor Delivery",
    "Warehouse Update",
    "Manual Correction",
    "Emergency Purchase",
]

STOCK_STATUSES = [
    "PENDING",
    "APPROVED",
    "READY_PACKED",
    "OUT_FOR_DELIVERY",
    "DELIVERED",
    "COMPLETED",
    "REJECTED",
]

MESSAGE_SUBJECTS = [
    "Inventory Issue",
    "Forecast Review",
    "Monthly Report",
    "Stock Shortage",
    "Vendor Complaint",
    "Delivery Delay",
    "Forecast Accuracy",
    "System Feedback",
]

MESSAGE_BODIES = {
    "Inventory Issue": [
        "Laptop stock is critically low. Please approve an urgent restock request.",
        "Microwave units in warehouse do not match system count. Please investigate.",
        "Several furniture items show negative movement in the last audit.",
    ],
    "Forecast Review": [
        "Smartphone demand forecast for next month looks high. Can we review assumptions?",
        "Please review the updated demand forecast before the monthly planning meeting.",
        "Forecast for Gaming Console needs adjustment after last quarter sales dip.",
    ],
    "Monthly Report": [
        "Attached summary of May sales performance for your review.",
        "Monthly revenue report is ready. Electronics category led growth this month.",
        "Please find the monthly KPI summary for admin review.",
    ],
    "Stock Shortage": [
        "Bluetooth Speaker stock will run out within two weeks at current sales rate.",
        "We are out of stock on Gaming Console. Customers are asking for back-order dates.",
        "Smart Watch inventory is below safety level. Requesting approval to reorder.",
    ],
    "Vendor Complaint": [
        "ElectroLink Distributors delivered damaged Laptop units in the last shipment.",
        "Vendor delivery was incomplete — 15 Smartphone units missing from invoice.",
        "Please follow up with CraftWood Furniture regarding repeated late deliveries.",
    ],
    "Delivery Delay": [
        "Air Conditioner shipment from HomeComfort Appliances is delayed by one week.",
        "Vendor confirmed Water Heater delivery pushed to next Friday.",
        "Ceiling Fan order is stuck in transit. Warehouse needs an update.",
    ],
    "Forecast Accuracy": [
        "Last month forecast for Smartphone was 12% above actual sales.",
        "Forecast accuracy improved for Laptop but Microwave still needs tuning.",
        "Can we discuss why Refrigerator forecast missed actual demand?",
    ],
    "System Feedback": [
        "Reports page is much faster after the last update. Good work.",
        "Suggestion: add export option on inventory alerts table.",
        "The new forecast chart on demand page is very helpful for planning.",
    ],
}

MESSAGE_PAIRS = [
    (3, 1, "Employee → Admin"),
    (2, 1, "Manager → Admin"),
    (1, 3, "Admin → Employee"),
    (1, 4, "Admin → Vendor"),
    (4, 1, "Vendor → Admin"),
    (5, 1, "Vendor → Admin"),
    (2, 3, "Manager → Employee"),
    (1, 2, "Admin → Manager"),
]


def month_starts(start: date, end: date) -> list[date]:
    months = []
    y, m = start.year, start.month
    while date(y, m, 1) <= end.replace(day=1):
        months.append(date(y, m, 1))
        m += 1
        if m > 12:
            m = 1
            y += 1
    return months


def seasonal_multiplier(profile: str, month: int) -> float:
    summer = {4, 5, 6, 7, 8}
    winter = {11, 12, 1}
    holiday = {11, 12}

    if profile == "phone_fast":
        base = 1.0 + (month in holiday) * 1.8
        return base
    if profile == "laptop_steady":
        return 1.0 + (month in holiday) * 0.9
    if profile == "watch_fast":
        return 1.0 + (month in holiday) * 1.2 + (month in {1, 2}) * 0.3
    if profile == "speaker_seasonal":
        return 1.0 + (month in holiday) * 1.5 + (month in summer) * 0.6
    if profile == "console_holiday":
        return 1.0 + (month in holiday) * 2.0
    if profile == "furniture_slow":
        return 1.0 + (month in {3, 4, 9}) * 0.2
    if profile == "chair_high":
        return 1.0 + (month in {2, 3, 8, 9}) * 0.25
    if profile == "dining_stable":
        return 1.0 + (month in {10, 11}) * 0.15
    if profile == "sofa_summer":
        return 1.0 + (month in summer) * 0.9
    if profile == "fridge_moderate":
        return 1.0 + (month in {3, 4, 5}) * 0.35
    if profile == "microwave_steady":
        return 1.0 + (month in {10, 11, 12}) * 0.2
    if profile == "ac_summer":
        return 1.0 + (month in {4, 5, 6}) * 2.5
    if profile == "fan_summer":
        return 1.0 + (month in {3, 4, 5, 6, 7}) * 1.4
    if profile == "heater_winter":
        return 1.0 + (month in {11, 12, 1, 2}) * 1.8
    return 1.0


def growth_factor(profile: str, month_index: int) -> float:
    rates = {
        "phone_fast": 0.045,
        "laptop_steady": 0.022,
        "watch_fast": 0.038,
        "speaker_seasonal": 0.028,
        "console_holiday": 0.025,
        "furniture_slow": 0.012,
        "chair_high": 0.018,
        "dining_stable": 0.008,
        "sofa_summer": 0.015,
        "fridge_moderate": 0.02,
        "microwave_steady": 0.014,
        "ac_summer": 0.016,
        "fan_summer": 0.017,
        "heater_winter": 0.013,
    }
    rate = rates.get(profile, 0.015)
    wave = math.sin(month_index / 2.3) * 0.08
    return 1.0 + month_index * rate + wave


def monthly_target_qty(profile: str, base_stock_factor: int, month_index: int, month: int) -> int:
    if profile == "laptop_steady":
        qty = 10 + month_index * 0.45 + random.uniform(-0.8, 0.8)
        return max(8, int(round(qty)))
    base = max(3, base_stock_factor // 25)
    qty = base * growth_factor(profile, month_index) * seasonal_multiplier(profile, month)
    if profile == "console_holiday" and 9 <= month_index <= 11:
        qty *= 0.45  # temporary 3-month decline
    if profile == "watch_fast" and month_index == 13:
        qty *= 2.8  # promotion spike
    noise = random.uniform(0.82, 1.22)
    return max(2, int(round(qty * noise)))


def compute_predicted(ma: float, reg: float) -> tuple[float, float, float]:
    """Match ForecastService: clamp regression ±20% of MA; 70% MA + 30% reg."""
    clamped = max(ma * 0.8, min(ma * 1.2, reg))
    predicted = round(ma * 0.7 + clamped * 0.3, 2)
    return ma, clamped, predicted


def random_sale_dates(year: int, month: int, count: int) -> list[date]:
    last = monthrange(year, month)[1]
    days = list(range(1, last + 1))
    random.shuffle(days)
    return [date(year, month, d) for d in days[:count]]


def std_dev(values: list[float]) -> float:
    if len(values) < 2:
        return 0.0
    mean = sum(values) / len(values)
    var = sum((v - mean) ** 2 for v in values) / len(values)
    return math.sqrt(var)


def linreg_predict(xs: list[float], ys: list[float], next_x: float) -> float:
    if not ys:
        return 0.0
    if len(ys) == 1:
        return ys[0]
    n = len(xs)
    sx = sum(xs)
    sy = sum(ys)
    sxx = sum(x * x for x in xs)
    sxy = sum(x * y for x, y in zip(xs, ys))
    denom = n * sxx - sx * sx
    if abs(denom) < 1e-9:
        return ys[-1]
    slope = (n * sxy - sx * sy) / denom
    intercept = (sy - slope * sx) / n
    return max(0.0, intercept + slope * next_x)


def sql_escape(s: str) -> str:
    return s.replace("'", "''")


def append_create_index_if_missing(lines: list[str], index_name: str, table: str, columns: str) -> None:
    append = lines.append
    append(f"-- Index {index_name} on {table}")
    append("SET @idx_exists = (")
    append("  SELECT COUNT(*) FROM information_schema.STATISTICS")
    append("  WHERE TABLE_SCHEMA = DATABASE()")
    append(f"    AND TABLE_NAME = '{table}'")
    append(f"    AND INDEX_NAME = '{index_name}'")
    append(");")
    append("SET @idx_sql = IF(@idx_exists = 0,")
    append(f"  'CREATE INDEX {index_name} ON {table} ({columns})',")
    append("  'SELECT 1');")
    append("PREPARE idx_stmt FROM @idx_sql;")
    append("EXECUTE idx_stmt;")
    append("DEALLOCATE PREPARE idx_stmt;")
    append("")


def main() -> None:
    lines: list[str] = []
    append = lines.append

    append("-- =============================================================================")
    append("-- ForecastPro — demo seed dataset (analytics, forecasts, reports, vendors)")
    append("-- =============================================================================")
    append("-- Run:")
    append("--   mysql -u root -p forecastpro < scripts/db/seed-large.sql")
    append("--")
    append("-- Logins:")
    append("--   admin / Admin@123")
    append("--   manager / Manager@123")
    append("--   employee / Employee@123")
    append("--   craftwood, homecomfort, electrolink, summit / Vendor@123")
    append("--     craftwood   = CraftWood Furniture (Furniture)")
    append("--     homecomfort = HomeComfort Appliances (Home Appliances)")
    append("--     electrolink = ElectroLink Distributors (Electronics)")
    append("--     summit      = Summit General Supply (general)")
    append(f"-- Date range: {START.isoformat()} → {END.isoformat()} (last {SALES_HISTORY_MONTHS} months)")
    append("-- =============================================================================")
    append("")
    append("USE forecastpro;")
    append("")
    append("SET NAMES utf8mb4;")
    append("SET FOREIGN_KEY_CHECKS = 0;")
    append("")
    append("DELETE FROM forecasts;")
    append("DELETE FROM sales;")
    append("DELETE FROM inventory;")
    append("DELETE FROM stock_requests;")
    append("DELETE FROM messages;")
    append("DELETE FROM products;")
    append("DELETE FROM categories;")
    append("DELETE FROM vendors;")
    append("DELETE FROM users;")
    append("")
    for table in [
        "users",
        "categories",
        "products",
        "sales",
        "forecasts",
        "inventory",
        "stock_requests",
        "messages",
        "vendors",
    ]:
        append(f"ALTER TABLE {table} AUTO_INCREMENT = 1;")
    append("")
    append("SET FOREIGN_KEY_CHECKS = 1;")
    append("")

    # Users
    append("-- USERS")
    append("INSERT INTO users (username, password, role, enabled) VALUES")
    user_rows = [
        ("admin", ADMIN_HASH, "ADMIN"),
        ("manager", MANAGER_HASH, "SALES_MANAGER"),
        ("employee", EMPLOYEE_HASH, "EMPLOYEE"),
        ("craftwood", VENDOR_HASH, "VENDOR"),
        ("homecomfort", VENDOR_HASH, "VENDOR"),
        ("electrolink", VENDOR_HASH, "VENDOR"),
        ("summit", VENDOR_HASH, "VENDOR"),
    ]
    append(",\n".join(f"('{u}', '{p}', '{r}', 1)" for u, p, r in user_rows) + ";")
    append("")

    # Categories
    append("-- CATEGORIES")
    append("INSERT INTO categories (name) VALUES")
    append(",\n".join(f"('{c}')" for c in CATEGORIES) + ";")
    append("")

    # Products
    append("-- PRODUCTS")
    append("INSERT INTO products (name, category_id, price, stock_quantity) VALUES")
    prod_vals = []
    for name, cat, price, stock, _ in PRODUCTS:
        prod_vals.append(f"('{sql_escape(name)}', {cat}, {price:.2f}, {stock})")
    append(",\n".join(prod_vals) + ";")
    append("")

    # Vendors (user_id 4-7 for craftwood, homecomfort, electrolink, summit)
    append("-- VENDORS")
    append("INSERT INTO vendors (name, contact, status, user_id) VALUES")
    append(",\n".join(f"('{n}', '{c}', 'ACTIVE', {uid})" for n, c, uid in VENDORS) + ";")
    append("")

    # Sales — 1 record per product per month across last 18 months
    append(f"-- SALES ({START.isoformat()} → {END.isoformat()}, 1 sale per product per month)")
    sales_rows: list[str] = []
    monthly_totals: dict[int, dict[date, int]] = {
        pid: {} for pid in range(1, len(PRODUCTS) + 1)
    }

    months = month_starts(START, END)
    for mi, month_start in enumerate(months):
        y, m = month_start.year, month_start.month
        for pid, (_, _, _, stock_hint, profile) in enumerate(PRODUCTS, start=1):
            target = monthly_target_qty(profile, stock_hint, mi, m)
            # Single sale per product-month; occasional second record keeps total in 150–300
            records = 2 if random.random() < 0.08 else 1
            if records == 1:
                parts = [target]
            else:
                split = max(1, target // 2)
                parts = [split, max(1, target - split)]
            for sale_date, qty in zip(random_sale_dates(y, m, records), parts):
                sales_rows.append(f"({pid}, {qty}, '{sale_date.isoformat()}')")
                monthly_totals[pid][month_start] = monthly_totals[pid].get(month_start, 0) + qty

    append("INSERT INTO sales (product_id, quantity_sold, sale_date) VALUES")
    append(",\n".join(sales_rows) + ";")
    append("")

    # Forecasts — recent months only (30–60 rows total)
    append(f"-- FORECASTS (last {FORECAST_MONTHS_COUNT} months per product)")
    forecast_months = months[-FORECAST_MONTHS_COUNT:]
    forecast_rows: list[str] = []

    for pid, (name, _, price, stock_hint, profile) in enumerate(PRODUCTS, start=1):
        history: list[tuple[date, int]] = sorted(monthly_totals[pid].items())
        base = max(3, stock_hint // 25)
        for fm in forecast_months:
            prior = [(ms, q) for ms, q in history if ms < fm]
            if prior:
                qtys = [float(q) for _, q in prior[-12:]]
                xs = [float(i) for i, _ in enumerate(prior[-12:])]
                ma_raw = sum(qtys[-3:]) / min(3, len(qtys))
                reg_raw = linreg_predict(xs, qtys, float(len(qtys)))
            else:
                ma_raw = float(base)
                reg_raw = float(base) * 1.05
            ma, reg, predicted = compute_predicted(ma_raw, reg_raw)
            if fm == forecast_months[-1] and name in ALERT_FORECAST_TARGETS:
                predicted = ALERT_FORECAST_TARGETS[name]
                ma = predicted * 0.92
                reg = predicted * 1.05
            elif fm == forecast_months[0] and prior:
                # Slight forecast miss vs prior month actual for accuracy demos
                actual_last = prior[-1][1]
                predicted = round(actual_last * (1.08 if pid % 2 == 0 else 0.92), 2)
                ma, reg, _ = compute_predicted(ma_raw, reg_raw)
            sigma = std_dev(qtys) if prior else float(base) * 0.2
            lower = max(0.0, predicted - sigma)
            upper = predicted + sigma
            revenue = predicted * price
            created = datetime(fm.year, fm.month, 5, 9, 15, 0) + timedelta(days=pid)
            forecast_rows.append(
                "("
                f"{pid}, {ma:.4f}, {reg:.4f}, {predicted:.4f}, {revenue:.4f}, "
                f"'{fm.isoformat()}', {lower:.4f}, {upper:.4f}, "
                f"'{created.strftime('%Y-%m-%d %H:%M:%S')}.000000'"
                ")"
            )

    append(
        "INSERT INTO forecasts (product_id, moving_avg_value, ml_regression_value, "
        "predicted_sales, predicted_revenue, forecast_month, lower_bound, upper_bound, created_at) VALUES"
    )
    append(",\n".join(forecast_rows) + ";")
    append("")

    # Inventory
    append("-- INVENTORY")
    inv_rows: list[str] = []
    inv_id_date = START
    for _ in range(INVENTORY_COUNT):
        pid = random.randint(1, len(PRODUCTS))
        qty = random.randint(5, 80)
        # Store vendor name (matches live stock-request receipts and report display).
        source = sql_escape(vendor_name_for_product(pid))
        inv_id_date += timedelta(days=random.randint(3, 14))
        if inv_id_date > END:
            inv_id_date = END - timedelta(days=random.randint(0, 30))
        created = datetime.combine(inv_id_date, datetime.min.time()) + timedelta(hours=random.randint(8, 17))
        inv_rows.append(
            f"({pid}, {qty}, '{inv_id_date.isoformat()}', '{source}', "
            f"'{created.strftime('%Y-%m-%d %H:%M:%S')}.000000')"
        )
    append("INSERT INTO inventory (product_id, quantity_added, date_added, source, created_at) VALUES")
    append(",\n".join(inv_rows) + ";")
    append("")

    # Stock requests
    append("-- STOCK REQUESTS")
    sr_rows: list[str] = []
    req_date = START + timedelta(days=14)
    for i in range(STOCK_REQUEST_COUNT):
        pid = random.randint(1, len(PRODUCTS))
        vendor_id = vendor_for_product(pid)
        qty = random.randint(10, 120)
        status = STOCK_STATUSES[i % len(STOCK_STATUSES)]
        req_date += timedelta(days=random.randint(5, 18))
        if req_date > END:
            req_date = END - timedelta(days=random.randint(0, min(25, (END - START).days)))
        sr_rows.append(f"({pid}, {qty}, '{status}', {vendor_id}, '{req_date.isoformat()}')")
    append("INSERT INTO stock_requests (product_id, requested_quantity, status, vendor_id, request_date) VALUES")
    append(",\n".join(sr_rows) + ";")
    append("")

    # Messages
    append("-- MESSAGES")
    msg_rows: list[str] = []
    for i in range(MESSAGE_COUNT):
        sender, recipient, _ = MESSAGE_PAIRS[i % len(MESSAGE_PAIRS)]
        subject = MESSAGE_SUBJECTS[i % len(MESSAGE_SUBJECTS)]
        status = "OPEN" if i % 3 else "CLOSED"
        created = datetime.combine(START, datetime.min.time()) + timedelta(days=i * 14, hours=i % 8)
        message_text = MESSAGE_BODIES[subject][i % len(MESSAGE_BODIES[subject])]
        msg_rows.append(
            f"({sender}, {recipient}, '{sql_escape(subject)}', '{sql_escape(message_text)}', "
            f"'{status}', '{created.strftime('%Y-%m-%d %H:%M:%S')}')"
        )
    append(
        "INSERT INTO messages (sender_id, receiver_id, subject, message, status, created_at) VALUES"
    )
    append(",\n".join(msg_rows) + ";")
    append("")

    # Indexes + unique constraint guard (MySQL versions without CREATE INDEX IF NOT EXISTS)
    append("-- INDEXES (idempotent)")
    append_create_index_if_missing(lines, "idx_sales_product_date", "sales", "product_id, sale_date")
    append_create_index_if_missing(lines, "idx_forecasts_month", "forecasts", "forecast_month")
    append_create_index_if_missing(lines, "idx_inventory_product", "inventory", "product_id")
    append_create_index_if_missing(lines, "idx_stock_requests_status", "stock_requests", "status")
    append_create_index_if_missing(lines, "idx_messages_created_at", "messages", "created_at")
    append("-- Ensure unique (product_id, forecast_month)")
    append("SET @uk_exists = (")
    append("  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS")
    append("  WHERE CONSTRAINT_SCHEMA = DATABASE()")
    append("    AND TABLE_NAME = 'forecasts'")
    append("    AND CONSTRAINT_NAME = 'uk_forecasts_product_month'")
    append(");")
    append("SET @uk_sql = IF(@uk_exists = 0,")
    append("  'ALTER TABLE forecasts ADD CONSTRAINT uk_forecasts_product_month UNIQUE (product_id, forecast_month)',")
    append("  'SELECT 1');")
    append("PREPARE uk_stmt FROM @uk_sql;")
    append("EXECUTE uk_stmt;")
    append("DEALLOCATE PREPARE uk_stmt;")
    append("")
    append(f"-- STATS: sales={len(sales_rows)}, forecasts={len(forecast_rows)}, inventory={len(inv_rows)}, "
           f"stock_requests={len(sr_rows)}, messages={len(msg_rows)}")
    append("-- Done.")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT}")
    print(f"sales={len(sales_rows)}, forecasts={len(forecast_rows)}")


if __name__ == "__main__":
    main()
