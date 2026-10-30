from larder.pantry import Line, PantryOrder, amount_cents


def test_the_amount_uses_the_prices_the_member_confirmed():
    order = PantryOrder("o1", (Line("rice", 2, 300), Line("beans", 1, 450)))
    catalog = {"rice": 350, "beans": 500}

    assert amount_cents(order, catalog) == 2 * 300 + 450


def test_an_item_gone_from_the_catalog_is_still_charged_its_line_price():
    order = PantryOrder("o1", (Line("rice", 1, 300), Line("oil", 1, 900)))

    assert amount_cents(order, {"rice": 350}) == 1200
