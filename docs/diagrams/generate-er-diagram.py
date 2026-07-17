#!/usr/bin/env python3
"""Generate ForecastPro Chen ER diagram DOT — symmetrical portrait layout."""

from pathlib import Path

OUT_DOT = Path(__file__).parent / "er-diagram.dot"
OUT_PUML = Path(__file__).parent / "er-diagram.puml"

# A4 portrait canvas (inches, neato coords, y-up). Center x = 4.14
CX = 4.14
ROW1_Y = 10.05
ROW2_Y = 8.35
ROW3_Y = 6.05
ROW4_Y = 3.55
COL_L = 1.55
COL_C = CX
COL_R = 6.73

# Entity centers — strict 4-row grid
ENTITIES = {
    "Users": (COL_L, ROW1_Y),
    "Vendors": (COL_R, ROW1_Y),
    "Messages": (COL_L, ROW2_Y),
    "Categories": (COL_C, ROW2_Y),
    "Products": (COL_R, ROW2_Y),
    "Sales": (COL_L, ROW3_Y),
    "Forecasts": (COL_C, ROW3_Y),
    "Inventory": (COL_R, ROW3_Y),
    "StockRequests": (COL_C, ROW4_Y),
}

# Relationship diamonds — centered on connector paths
RELS = {
    "r_owns": (CX, ROW1_Y),
    "r_sends": (COL_L + 0.55, 9.2),
    "r_receives": (COL_L + 1.25, 8.75),
    "r_contains": (COL_C + 1.3, ROW2_Y),
    "r_generates": (COL_L, ROW2_Y - 1.15),
    "r_hasfc": (5.35, ROW2_Y - 1.15),
    "r_hasinv": (COL_R, ROW2_Y - 1.15),
    "r_reqin": (5.35, ROW3_Y - 0.55),
    "r_handles": (6.05, 7.85),
}

REL_LABELS = {
    "r_contains": "CONTAINS",
    "r_generates": "GENERATES",
    "r_hasfc": "HAS\\nFORECAST",
    "r_hasinv": "HAS\\nINVENTORY",
    "r_handles": "HANDLES",
    "r_reqin": "REQUESTED\\nIN",
    "r_owns": "OWNS",
    "r_sends": "SENDS",
    "r_receives": "RECEIVES",
}

EDGES = [
    ("Users", "r_owns", "1"),
    ("r_owns", "Vendors", "0..1"),
    ("Users", "r_sends", "1"),
    ("r_sends", "Messages", "M"),
    ("Users", "r_receives", "1"),
    ("r_receives", "Messages", "M"),
    ("Categories", "r_contains", "1"),
    ("r_contains", "Products", "M"),
    ("Products", "r_generates", "1"),
    ("r_generates", "Sales", "M"),
    ("Products", "r_hasfc", "1"),
    ("r_hasfc", "Forecasts", "M"),
    ("Products", "r_hasinv", "1"),
    ("r_hasinv", "Inventory", "M"),
    ("Products", "r_reqin", "1"),
    ("r_reqin", "StockRequests", "M"),
    ("Vendors", "r_handles", "1"),
    ("r_handles", "StockRequests", "M"),
]

# Attribute placement: top / left / right / bottom lists per entity
ATTR_SPEC = {
    "Users": {
        "top": [("u_id", "id", True)],
        "left": [("u_un", "username"), ("u_pw", "password")],
        "right": [("u_ro", "role"), ("u_en", "enabled")],
    },
    "Categories": {
        "top": [("c_id", "id", True)],
        "right": [("c_name", "name")],
    },
    "Messages": {
        "top": [("m_id", "id", True)],
        "left": [("m_snd", "sender_id (FK)"), ("m_rcv", "receiver_id (FK)")],
        "right": [("m_sub", "subject"), ("m_body", "message"), ("m_st", "status")],
        "bottom": [("m_ca", "created_at")],
    },
    "Products": {
        "top": [("p_id", "id", True)],
        "left": [("p_cfk", "category_id (FK)")],
        "right": [("p_name", "name"), ("p_pri", "price")],
        "bottom": [("p_stk", "stock_quantity")],
    },
    "Vendors": {
        "top": [("v_id", "id", True)],
        "left": [("v_ufk", "user_id (FK)")],
        "right": [("v_name", "name"), ("v_con", "contact")],
        "bottom": [("v_st", "status")],
    },
    "Sales": {
        "top": [("s_id", "id", True)],
        "left": [("s_pfk", "product_id (FK)")],
        "right": [("s_qty", "quantity_sold")],
        "bottom": [("s_dt", "sale_date")],
    },
    "Forecasts": {
        "top": [("f_id", "id", True)],
        "left": [
            ("f_pfk", "product_id (FK)"),
            ("f_ma", "moving_avg_value"),
            ("f_reg", "ml_regression_value"),
        ],
        "right": [("f_ps", "predicted_sales"), ("f_pr", "predicted_revenue")],
        "bottom": [
            ("f_fm", "forecast_month"),
            ("f_lb", "lower_bound"),
            ("f_ub", "upper_bound"),
            ("f_ca", "created_at"),
        ],
    },
    "Inventory": {
        "top": [("i_id", "id", True)],
        "left": [("i_pfk", "product_id (FK)")],
        "right": [("i_qty", "quantity_added"), ("i_dt", "date_added")],
        "bottom": [("i_src", "source"), ("i_ca", "created_at")],
    },
    "StockRequests": {
        "top": [("sr_id", "id", True)],
        "left": [("sr_pfk", "product_id (FK)"), ("sr_vfk", "vendor_id (FK)")],
        "right": [("sr_qty", "requested_quantity"), ("sr_st", "status")],
        "bottom": [("sr_dt", "request_date")],
    },
}

TOP_DY = 0.78
BOT_DY = -0.78
SIDE_DX = 1.08
SIDE_SP = 0.42
BOT_SP = 0.52


def pk_label() -> str:
    return "< <U>id</U><BR/>(PK) >"


def pos(x: float, y: float) -> str:
    return f'pos="{x:.2f},{y:.2f}!"'


def build_attrs(ename: str, cx: float, cy: float) -> list[tuple[str, str, float, float, bool, int]]:
    spec = ATTR_SPEC[ename]
    out: list[tuple[str, str, float, float, bool, int]] = []
    fs = 9 if ename in ("Forecasts", "StockRequests") else 10
    if ename in ("Users", "Categories", "Products", "Vendors"):
        fs = 11

    for aid, label, is_pk in spec.get("top", []):
        out.append((aid, label, cx, cy + TOP_DY, is_pk, fs))

    left = spec.get("left", [])
    for i, (aid, label) in enumerate(left):
        y = cy + 0.22 - i * SIDE_SP
        out.append((aid, label, cx - SIDE_DX, y, False, fs - (1 if len(label) > 18 else 0)))

    right = spec.get("right", [])
    for i, (aid, label) in enumerate(right):
        y = cy + 0.22 - i * SIDE_SP
        out.append((aid, label, cx + SIDE_DX, y, False, fs - (1 if len(label) > 16 else 0)))

    bottom = spec.get("bottom", [])
    n = len(bottom)
    for i, (aid, label) in enumerate(bottom):
        x_off = (i - (n - 1) / 2) * BOT_SP
        out.append((aid, label, cx + x_off, cy + BOT_DY, False, fs - (1 if len(label) > 14 else 0)))

    return out


def main() -> None:
    lines = [
        "digraph ForecastPro_Chen_ER {",
        "  graph [",
        '    label="ForecastPro Entity Relationship Diagram"',
        "    labelloc=t",
        "    fontsize=26",
        '    fontname="Arial Bold"',
        "    dpi=300",
        "    bgcolor=white",
        "    layout=neato",
        "    overlap=false",
        "    splines=line",
        '    sep="+0.2"',
        "    pad=0.5",
        '    size="8.27,11.69!"',
        "    ratio=fill",
        "  ];",
        '  node [fontname="Arial", margin="0.07,0.05"];',
        '  edge [fontname="Arial Bold", fontsize=11, penwidth=1.2, color="#1E293B"];',
        "",
    ]

    for name, (x, y) in ENTITIES.items():
        fs = 14 if name in ("Users", "Vendors", "Categories", "Products") else 12
        w = 1.4 if len(name) > 8 else 1.2
        lines.append(
            f'  {name} [shape=box, style="rounded,filled,bold", fillcolor="#DAE8FC",'
            f' fontsize={fs}, label={name}, width={w}, height=0.44, {pos(x, y)}];'
        )

    for rid, (x, y) in RELS.items():
        lines.append(
            f'  {rid} [shape=diamond, style="filled,bold", fillcolor="#FEF3C7",'
            f' fontsize=11, label="{REL_LABELS[rid]}", width=1.05, height=0.75, {pos(x, y)}];'
        )

    for ename, (cx, cy) in ENTITIES.items():
        for aid, label, ax, ay, is_pk, fs in build_attrs(ename, cx, cy):
            lbl = pk_label() if is_pk else f'"{label}"'
            lines.append(
                f'  {aid} [shape=ellipse, style=filled, fillcolor=white, fontsize={fs},'
                f" label={lbl}, {pos(ax, ay)}];"
            )
            lines.append(f"  {aid} -> {ename} [dir=none, color=\"#64748B\", penwidth=0.9];")

    for src, dst, card in EDGES:
        lines.append(f'  {src} -> {dst} [label="  {card}  "];')

    lines.append("}")
    dot_body = "\n".join(lines) + "\n"
    OUT_DOT.write_text(dot_body, encoding="utf-8")

    puml = (
        "' ForecastPro — Chen ER Diagram (symmetrical portrait layout)\n"
        "' Regenerate: python3 generate-er-diagram.py\n"
        "' Export:\n"
        "'   cd /Users/xyz/Desktop/ForecastPro/docs/diagrams\n"
        "'   python3 generate-er-diagram.py\n"
        "'   neato -Tsvg -Gdpi=300 er-diagram.dot -o er-diagram.svg\n"
        "'   rsvg-convert -w 2480 er-diagram.svg -o er-diagram.png\n"
        "'   sips -s dpiWidth 300 -s dpiHeight 300 er-diagram.png\n"
        "\n@startdot\n"
        + dot_body
        + "@enddot\n"
    )
    OUT_PUML.write_text(puml, encoding="utf-8")
    print(f"Wrote {OUT_DOT}")
    print(f"Wrote {OUT_PUML}")


if __name__ == "__main__":
    main()
