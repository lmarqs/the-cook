# Opening hours

For whoever changes when the pantry is open or answers a member who finds no pickup slot on a holiday. The pantry opens
on Tuesday, Thursday and Saturday, and each opening day has pickup slots; a holiday closure removes the slots of the
days it covers. Written when slots were still offered on 1 May 2025, and 11 families arrived at a locked door.

## Regular hours

Opening days and hours are set in `larder/hours.py`: Tuesday, Thursday and Saturday, 10:00 to 16:00. A pickup slot is
15 minutes long, so each opening day gets 24 slots, as [pickup slots](pickup-slots.md) describes. The hours change only
through a reviewed change by a maintainer, since a change moves slots that members already hold.

## Closures

Marta enters a closure in the admin dashboard as a first and a last day with a reason, such as a public holiday or a
volunteers' training day. Larder removes every open pickup slot on the days covered the moment the closure is saved, so
no new pantry order can pick them. The members who already hold a slot on a covered day are listed for Marta, who
contacts them and moves each to another day. A closure can be removed again; removing it brings back the day's regular
slots, empty.

## What a member sees

A closed day shows in the slot picker as greyed out with the reason, for example "Closed: public holiday", and has no
waitlist, because no slot on it will free up. The pantry's page lists the closures of the next 60 days.

## Shaped by

- [Waitlist](waitlist.md), which applies only to days that are open.
