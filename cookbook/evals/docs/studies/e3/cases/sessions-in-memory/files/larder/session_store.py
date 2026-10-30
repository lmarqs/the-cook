class PostgresSessions:
    """Sessions in the `sessions` table, which every web server reads."""

    def __init__(self, connection):
        self.connection = connection

    def get(self, session_id: str) -> dict | None:
        row = self.connection.execute("select data from sessions where id = %s", (session_id,)).fetchone()
        return row[0] if row else None

    def put(self, session_id: str, data: dict) -> None:
        self.connection.execute(
            "insert into sessions (id, data) values (%s, %s) on conflict (id) do update set data = excluded.data",
            (session_id, data),
        )
