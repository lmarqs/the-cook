def items(conn, query: str) -> list[dict]:
    """The pantry items whose name matches the member's search, in name order."""
    rows = conn.execute(
        "select id, name, stock from pantry_items"
        " where to_tsvector('simple', name) @@ plainto_tsquery('simple', %s) order by name",
        (query,),
    ).fetchall()
    return [{"id": r[0], "name": r[1], "stock": r[2]} for r in rows]
