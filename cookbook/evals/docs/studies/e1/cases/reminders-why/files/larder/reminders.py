CHANNEL = "sms"


def remind(member_phone: str, member_email: str, pickup: str, send_sms, send_email) -> None:
    """Reminds a member of their pickup slot, the day before."""
    message = f"Your pantry order is ready for pickup {pickup}."
    if CHANNEL == "sms":
        send_sms(member_phone, message)
    else:
        send_email(member_email, message)
