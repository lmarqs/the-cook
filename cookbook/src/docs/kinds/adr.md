## Architecture decisions, `adr/`

Holds why the system is built as it is: each foundation decision, the options weighed and why the choice won.
Diátaxis type: explanation.

What makes a good decision record:

- Holds one decision other work is built on, so that reversing it changes more than one feature or module. Such a
  decision shapes structure, qualities, dependencies or interfaces, is hard to reverse, surprises a reader new to its
  context and comes from a real trade-off. How one feature is implemented, a choice one change can reverse and a
  convention a check such as a linter enforces are not decisions of this kind: every record present reads as
  binding, and a record of a detail freezes what should stay easy to change.
- Is named `NNNN-<slug>.md`, numbered in sequence, with the decision taken as its title. A number is used once,
  since an old reference may still name it.
- Opens with a lead that says whom the decision concerns, the decision, and in a clause what moved it.
- Tells, in `Context`, what happened: the situation that raised the decision, what it cost and what was tried, then the
  forces the options are judged by. A reader tests whether the decision still holds against that story.
- Gives, in `Options`, each alternative at its best and why it lost; in `Decision`, the choice and why it won, with the
  product that implements it, if any, and why that one; in `Consequences`, what becomes easier and what it costs.
- Fits on about one page.
- Stays current. A decision that changes is edited in place: it keeps its number, its title and slug follow the new
  choice, and `Options` keeps the previous choice and why it changed. A decision reversed outright leaves with its
  record; version history keeps it.
