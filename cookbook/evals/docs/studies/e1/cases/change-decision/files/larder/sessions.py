import base64
import hashlib
import hmac
import json
import os
from datetime import datetime, timedelta, timezone

EXPIRY_MINUTES = 30
KEY = os.environ.get("LARDER_SESSION_KEY", "dev-only").encode()


def expires_at(last_request: datetime) -> datetime:
    """When a session ends if no request renews it."""
    return last_request + timedelta(minutes=EXPIRY_MINUTES)


def sign(member_id: int, last_request: datetime) -> str:
    """The cookie value: the member and the session's expiry, signed with the shared key."""
    body = json.dumps({"member": member_id, "expires": expires_at(last_request).isoformat()}).encode()
    mac = hmac.new(KEY, body, hashlib.sha256).hexdigest()
    return base64.urlsafe_b64encode(body).decode() + "." + mac


def verify(cookie: str, now: datetime | None = None) -> int | None:
    """The member a cookie logs in, or None when it is forged or expired."""
    encoded, _, mac = cookie.rpartition(".")
    body = base64.urlsafe_b64decode(encoded)
    if not hmac.compare_digest(mac, hmac.new(KEY, body, hashlib.sha256).hexdigest()):
        return None
    session = json.loads(body)
    if datetime.fromisoformat(session["expires"]) < (now or datetime.now(timezone.utc)):
        return None
    return session["member"]
