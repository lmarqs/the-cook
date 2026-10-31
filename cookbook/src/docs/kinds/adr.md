## Architecture decisions, `adr/`

Holds why the system is built as it is: each foundation decision, the options weighed and why the choice won.
Diátaxis type: explanation.

Good records:

- Hold one decision other work is built on, so that reversing it changes more than one feature or module. Such a
  decision shapes structure, qualities, dependencies or interfaces, is hard to reverse, surprises a reader new to its
  context and comes from a real trade-off. How one feature is implemented, a choice one change can reverse and a
  convention a check such as a linter enforces are not decisions of this kind: every record present reads as
  binding, and a record of a detail freezes what should stay easy to change.
- Are named `NNNN-<slug>.md`, numbered in sequence, with the decision taken as their title. A number is used once,
  since an old reference may still name it.
- Open with a lead that says whom the decision concerns, the decision, and in a clause what moved it.
- Tell, in `Context`, what happened: the situation that raised the decision, what it cost and what was tried, then the
  forces the options are judged by. A reader tests whether the decision still holds against that story.
- Give, in `Options`, each alternative at its best and why it lost; in `Decision`, the choice and why it won, with the
  product that implements it, if any, and why that one; in `Consequences`, what becomes easier and what it costs.
- Fit on about one page.
- Stay current. A decision that changes is edited in place: it keeps its number, its title and slug follow the new
  choice, and `Options` keeps the previous choice and why it changed. A decision reversed outright leaves with its
  record; version history keeps it.
