from dataclasses import dataclass


@dataclass(frozen=True)
class Member:
    id: str
    name: str
    address: str
