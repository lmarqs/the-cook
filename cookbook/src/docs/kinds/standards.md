## Standards, `standards/`

Holds the practices every artifact of one sort follows across the repository, such as every log record, every
command's output or every persisted entity, where no automated check enforces them. Diátaxis type: reference.

Good standards:

- Cover one sort of artifact each and are named after it, as `log-records.md` or `cli-output.md`. A subject whose
  practices fall into groups a reader applies separately becomes a directory of that name, with its guide.
- Sit with the artifact a practice constrains: the thing a person is writing or changing when it applies, where that
  person looks. A standard about a quality, such as `sensitive-data.md`, holds what is about the quality itself, such
  as the classes of data, and links the practices that serve it.
- Open with a lead that states the scope, which artifacts and where they live, and what went wrong before the
  standard existed.
- Give each practice its reason in a sentence, or link the record that holds it, so a reader can apply its intent to a
  case its words leave open.
- Make unmistakable which practices must not vary, as the BCP 14 keywords MUST, SHOULD and MAY do (RFC 2119,
  RFC 8174), and name the case that justifies departing from a practice that may vary.
- Show a compliant and a non-compliant example where the practice is about the shape of output.
- Hold what no check enforces. A practice a linter, type check, test or schema enforces is stated by that check, with
  its reason as a comment there, and leaves the standard in the change that adds the check.
- Hold what every artifact of the sort follows. One interface's contract, one feature's behavior, a target the running
  system meets and how people or agents work are facts of other kinds.
