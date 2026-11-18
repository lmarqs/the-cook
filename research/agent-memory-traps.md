# Is "trap" clear, and what do primary sources put in agent memory?

Researched 2026-11-17. Every external page was fetched that day; "via summary" marks pages read through a
summarizing fetcher whose quotes are short and may be imprecise, "direct" marks pages downloaded and quoted from
their own text. The note describes the recipe before the change that followed it; line numbers are of commit 6e34b991.

## Verdict

"Trap" is not clear enough to decide cases. The recipe never defines the word on its own: it defines memory by
it, uses it at three scopes (any task, the product, one area) and, in recipe help, with a fourth meaning (a
fault of a third-party tool version). A reader can decide the easy exclusions from the text alone (a person, a
personal preference, session state) but not the cases that matter: the line between a memory trap and a
documentation fact rests on "nobody would look up", and the two worked examples of memory entries are both
routed elsewhere by the table's own rows when those rows are read literally. The exclusions of user preferences
and project state are deliberate and stated; the exclusion of external references is not addressed.

## Part 1: the recipe's own text

Paths under `S/` are `cookbook/src/agents/skills/agents-layout/`.

### Where the word is defined or used

| Place | Text | What it does |
| --- | --- | --- |
| `S/references/memory.md:3-4` | "`.agents/memory/` holds traps: how to work in one area of the repository, learned from a correction or a failure, that a later session needs and no other file owns." | The owner. Defines what memory holds; "trap" is glossed only by the clause after the colon |
| `S/references/memory.md:20` | "`AGENTS.md`: a rule, or one of its five gotchas" | "gotcha" appears, undefined here |
| `S/references/memory.md:22` | "a decision, how a feature behaves, a standard, a trap of the product" goes to documentation | A second kind of trap, "of the product" |
| `S/references/memory.md:23` | "A trap nobody would look up in the documentation" goes to memory | The discriminator between rows 22 and 23 |
| `S/references/memory.md:25-28` | "the traps left for memory are how agents go wrong there: the command that looks right and is not, the file that must change with another, the generated directory that must stay unedited. In a repository without it, memory also holds the decisions and product traps a page would own" | The only examples; also widens memory beyond traps |
| `S/references/memory.md:32-37` | Three entry tests: a fresh session would fall into it; it happened ("a person's correction, a failed check, a command's output"); it names its area | Admission test |
| `S/SKILL.md:60` | "One file per area, holding the traps sessions met there" | Restates |
| `S/SKILL.md:71-73` | "**Memory** holds traps: how to work in one area, learned from a correction or a failure, that no other file owns." | Restates, drops "a later session needs" |
| `S/SKILL.md:82` | "Record a trap or anything else a session learned" | Routes to memory.md |
| `S/references/design.md:14` | "Instructions, skills, hook scripts, traps by area" | Lists it under the Knowledge axis |
| `S/references/agents-md.md:54-55` | "**Gotchas is capped at five.** Every session reads `AGENTS.md`, so it keeps only the traps any task can hit. A trap in one area goes to `.agents/memory/`" | Makes gotcha = trap any task can hit |
| `S/references/review.md:21` | "Gotchas holds at most five traps any task can hit" | Restates |
| `S/references/review.md:24-25` | Each memory entry "has no other owner, holds no session note, secret or personal data, and is still true" | Review test |
| `S/templates/AGENTS.template.md:62` | "A trap in one area: `.agents/memory/`, listed under Deep dive." | Restates |
| `S/templates/AGENTS.template.md:73-77` | "## Gotchas / At most five traps any task can hit. / `[[fill: what goes wrong when you do what, and what to do instead]]`" | The nearest thing to a definition of the bare word, given as a fill hint |
| `S/templates/README.template.md:19`, `.agents/README.md:19` | "The traps sessions met in one area, a file each" | Restates |
| `S/references/glossary.md` | No entry for trap, gotcha or memory entry; line 16 only lists `memory/` as a shared directory | The skill's glossary does not define it |
| `docs/features/glossary.md:33` | "Memory: The traps sessions met in one area of a repository, learned from a correction or a failure, that no other file owns" | Defines Memory by trap; no entry for Trap, Gotcha or Pitfall |
| `docs/ux/generated-content-standards.md:105-107` | "Memory files hold the traps sessions met in one area: how to work there, learned from a correction or a failure, that no other file owns." | Restates |
| `docs/README.md:36` | Pitfalls: "It is a trap: a setup that looks right and is not, or a guarantee a reader assumes Cook gives and it does not" | A different, sharper definition, for this project's docs only |
| `docs/features/pitfalls.md:5` | "# Pitfalls, gaps and traps" | Three words in one title, none distinguished |
| `cookbook/README.md:254`, `docs/ux/messages-and-docs.md:206` | "Traps: Known faults of the versions written for, each with its workaround" | A fourth meaning: recipe-help section |
| `cookbook/src/agents/connect.groovy:208`, `cookbook/src/git/worktrees.groovy:69`, `cookbook/src/git/templates/README.template.md:38`, `cookbook/src/beads/viewer.groovy:59`, `cookbook/src/beads/tracker.groovy:89`, `cookbook/src/ralph/ralph-tui.groovy:233` | `## Traps` headings | Uses of the fourth meaning; entries are tool behaviours, not learned from a session |
| `AGENTS.md:66`, `AGENTS.md:81`, `AGENTS.md:92` | "A trap a correction reveals in one area"; "## Gotchas"; "Known traps by feature: docs/features/pitfalls.md" | This repository's filled file; line 66 drops "or a failure" |

This repository's `.agents/memory/` holds only `.gitkeep`, so there is no real entry to read as a precedent.

### Do the definitions agree?

Partly. The memory sentence is repeated consistently (memory.md:3, SKILL.md:71, glossary.md:33,
generated-content-standards.md:105), with small drift: `AGENTS.md:66` keeps only "a correction",
`memory.md:35` adds "a command's output", which need be neither a correction nor a failure.

They disagree in kind:

- The memory gloss is "how to work in one area" (memory.md:3), which describes knowledge. The template hint is
  "what goes wrong when you do what, and what to do instead" (AGENTS.template.md:77) and docs/README.md:36 is "a
  setup that looks right and is not", which describe a hazard. memory.md:25-26 borrows the hazard phrasing ("the
  command that looks right and is not") only as an example.
- The `## Traps` sections of recipe help are "known faults of the versions written for" (cookbook/README.md:254):
  known in advance by the recipe author, not learned from a correction. An adopter reads both `cook help` and the
  skill and meets one word with two origins.
- "Holds traps" (memory.md:3) is contradicted five lines later: without documentation "memory also holds the
  decisions and product traps a page would own" (memory.md:26-28). A decision is not a trap and is not "learned
  from a correction or a failure". SKILL.md:60, SKILL.md:71, README.template.md:19 and glossary.md:33 carry the
  "traps" summary without that fallback.

### Trap, gotcha, pitfall

Implicitly one genus with three locations, never stated as such:

- gotcha = a trap any task can hit, in `AGENTS.md` (agents-md.md:54, AGENTS.template.md:75).
- pitfall = a trap of the product, in documentation (memory.md:22; docs/README.md:36 for this project).
- memory trap = a trap in one area that documentation does not own (memory.md:23).

No file says "gotcha" and "pitfall" are kinds of trap, and neither glossary lists any of the three words. The
reading above is an inference from agents-md.md:54 and memory.md:20-23.

### Six candidate facts, decided from the text alone

| Fact | What the text gives | Decidable? |
| --- | --- | --- |
| "The user wants terse answers" (one person's style) | Row "One person's preference → that person's harness settings" (memory.md:17). The earlier row "A secret, or about a person → Nowhere in the repository" (memory.md:15) also fits, and the table says "Take the first row that fits" (memory.md:10-11) | Yes for "not memory". The two rows give different destinations |
| "The user is a senior Go dev" | "about a person → Nowhere in the repository" (memory.md:15) | Yes |
| "The release freeze is on the 5th" (deadline) | Nearest row: "What this session did, what is left to do, or a plan → the tracker, or the commit message" (memory.md:16). A deadline is none of the three. No row for a fact that is not a trap and fits nothing | By inference only |
| "We chose Postgres over Dynamo because…" (past decision) | "a decision → the repository's documentation, where it keeps some" (memory.md:22); without documentation, memory (memory.md:26-28). Whether the entry tests apply ("Did it happen: a person's correction, a failed check, a command's output?", memory.md:35) is not said; a decision passes none | Destination yes; admission no |
| "`IntegrationTest` fails one run in five; rerun it" (flaky test) | Fits "Something a check, a hook or the policy can enforce" (memory.md:19, quarantine it), "Needed by every task" if the Done check runs it (memory.md:20), "What a person changing the code needs too" (memory.md:22), and "A trap nobody would look up" (memory.md:23). Whether a test suite's fault is "a trap of the product" is undefined | No |
| "`make db-migrate` needs `--env=test` here" (command needs a flag) | "Shown by the code, a config file, the README or the history → Nowhere" (memory.md:18) fits if the Makefile shows it; "the policy can enforce" (memory.md:19) may fit; "Needed by every task" (memory.md:20) fits if the command is in Quick reference; else memory | No: turns on what "shown" means |

Two further probes:

- An external pointer ("bugs live in tracker project X", "the dashboard is at …"): no row. Not a trap, not a
  plan, not shown by the repository.
- The recipe's own example entries (memory.md:50-53). "`make db-reset` drops the local database without asking"
  is shown by the Makefile (row memory.md:18) and is something the policy can enforce with an `ask` or deny entry
  (row memory.md:19). "Number a new migration from `main`" is something a check can enforce (row memory.md:19).
  Read literally, the table routes both examples away from memory before its last row is reached.

### What the text leaves undecided

1. What a trap is, apart from where it is stored. The only generic wording is a fill hint
   (AGENTS.template.md:77).
2. "Shown by the code" (memory.md:18): derivable by reading, or evident to a session that does not go looking. A
   trap is nearly always derivable; that it is not evident is what makes it a trap.
3. "Can enforce" (memory.md:19) against "comes to enforce it" (memory.md:64). The exit rule implies an entry
   lives in memory while enforcement is possible but not built; the routing row says it never enters.
4. Row 22 against row 23. "What a person changing the code needs too" (memory.md:22) covers all three examples of
   memory traps in memory.md:25-26; a person also must not edit the generated directory. The working
   discriminator is "nobody would look up in the documentation" (memory.md:23) and "how agents go wrong there"
   (memory.md:25), a claim about readers' behaviour that the text gives no test for.
5. "Area" (memory.md:3, 37): a directory, a module, a topic or a tool. One example, `db/migrations/`
   (memory.md:48).
6. Whether memory's fallback content (decisions, product traps; memory.md:26-28) must pass the entry tests.
7. A team convention a person states while correcting an agent: a rule in `AGENTS.md` if every task needs it
   (memory.md:20), but one-area conventions that are not hazards ("handlers in this package return `Result`")
   have no row; "how to work in one area" admits them, "trap" does not.
8. Facts that fit no row and are not traps (external pointers, deadlines).

## Part 2: what primary sources put in agent memory

### Per source

| Source | Memory is for | Put in | Keep out | Who writes | When loaded | Pruning |
| --- | --- | --- | --- | --- | --- | --- |
| Claude Code, `CLAUDE.md` [C1][C2][C3] (direct) | "persistent instructions for a project, your personal workflow, or your entire organization" | "build commands, conventions, project layout, 'always do X' rules"; add "when Claude makes the same mistake a second time", a review catches something, you repeat a correction, "a new teammate would need the same context"; "Places where the code is misleading", "Decisions that aren't in the repo", "Anything you've had to correct twice already"; sample file has a `# Gotchas` heading | What Claude can find by reading the code and "would actually go and look" for; multi-step procedures and one-area entries (to a skill or path-scoped rule); rules with no exceptions (to a hook); personal preferences (to `CLAUDE.local.md`) | The person; `/init` drafts | Every session, whole file | "target under 200 lines"; periodic review; `/doctor` "keeps pitfalls, rationale, and conventions that differ from tool defaults" and cuts what is derivable |
| Claude Code, `.claude/rules/` [C1][C2] (direct) | Instructions scoped to part of the codebase | "If a rule only matters in one part of the codebase, put it in `.claude/rules/` with a `paths:` filter" | Task-specific content (to skills) | The person | Every session, or "only … when Claude works with matching files" | Same review as `CLAUDE.md` |
| Claude Code, auto memory [C1] (direct) | "notes Claude writes itself based on your corrections and preferences" | Four types: `user` "your role, expertise, and working preferences"; `feedback` "corrections you give Claude and approaches you confirm"; `project` "ongoing work, deadlines, and decisions that Claude can't derive from the code or git history"; `reference` "where to find information outside the project" | "anything it can derive from the codebase, such as architecture, file paths, or debugging fixes"; anything `CLAUDE.md` already says | Claude | `MEMORY.md` index, first 200 lines or 25KB, every session; topic files on demand | Claude merges or drops stale entries when the index nears its limit; person edits via `/memory`. "machine-local … not shared across machines" |
| Anthropic API memory tool [A1] (direct) | "store and retrieve information across conversations"; "just-in-time context retrieval" | Use cases: "Maintain project context across multiple agent sessions", "Apply lessons from past interactions, decisions, and feedback", "Build up a knowledge base". Injected protocol: "record status / progress / thoughts etc in your memory" | Sensitive information ("Claude usually refuses"; add validation) | The agent | Agent views the directory "BEFORE DOING ANYTHING ELSE", then reads files on demand | "rename or delete files that are no longer relevant"; application expires files "that haven't been accessed in a long time" and caps size |
| Anthropic, context engineering post [A2] (via summary) | "Structured note-taking, or agentic memory": notes "persisted to memory outside of the context window" | Progress and objectives (to-do list, `NOTES.md`, the Pokémon step counts and maps); compaction keeps "architectural decisions, unresolved bugs, and implementation details" | "redundant tool outputs or messages" | The agent | Pulled back "at later times", after context resets | Not stated |
| Anthropic, long-running harness post [A3] (via summary) | Carry state between sessions of one long task | Feature list file with a `passes` status, `claude-progress.txt` log, `init.sh`, git commits | Editing the feature list beyond the status field | Initializer agent, then each coding agent | Start of every session: git log, progress file, feature list | Not stated |
| AGENTS.md specification [G1] (via summary) | "a dedicated, predictable place to provide the context and instructions to help AI coding agents" | Project overview, build and test commands, code style, testing instructions, security considerations; "anything you'd tell a new teammate"; lists "security gotchas" | Nothing specified; "No" required fields | People; agents can scaffold | Nearest file in the tree; closest wins | "Treat AGENTS.md as living documentation" |
| OpenAI Codex, `AGENTS.md` [O1] (via summary) | Guidance read "before doing any work" | Global working agreements; repository norms; code-review rules near the code they govern | Not stated | People | Once per run, root-to-leaf chain, stops at `project_doc_max_bytes` (32 KiB default) | Not stated |
| OpenAI Codex, memories [O2] (via summary) | "carry useful context from earlier work into future work" | Generated summaries of eligible prior chats | "Don't store secrets"; rules that must always apply: "Keep required team guidance in `AGENTS.md` or checked-in documentation" | Codex, in the background after a chat is idle; "Treat these files as generated state" | Injected into later sessions when enabled; off by default | Delete-all control; per-chat `/memories` |
| Cursor, rules [U1] (via summary) | "persistent, reusable context at the prompt level" | "focused, actionable, and scoped" rules; "Add rules only when you notice Agent making the same mistake repeatedly" | "Copying entire style guides" (use a linter); "Documenting every possible command"; "Duplicating what's already in your codebase" | People; `/create-rule` has the agent write one | Always, by description, by file pattern, or by @-mention | "Keep rules under 500 lines" |
| Cursor, memories [U2] (via summary; changelog only) | "remember facts from conversations and reference them in the future" | Facts from conversations | Not stated | Cursor | Not stated | "managed from Settings"; "stored per project on an individual level" |
| GitHub Copilot, custom instructions [H1] (via summary) | "repository-specific guidance and preferences" | What the repository does, how to build and validate "so the agent does not need to search", layout and architecture facts | Conflicting instructions; "Instructions must not be task specific" | People; cloud agent can draft via pull request | Repository-wide always; `applyTo` files by path | Generated file "no longer than 2 pages" |
| GitHub Copilot Memory [H2] (direct) | "durable facts about a repository, and details of your own personal preferences" | Repository facts ("this repository uses Vitest for unit tests"), with "citations pointing to the code that supports the fact"; user-level preferences ("Explain your changes before you apply them") | Not stated | "Copilot creates memories itself, as a byproduct of working with you" | Repository facts in work on that repository; preferences across repositories | "expire after a period without reinforcement … The default expiry period is 90 days"; citations checked against the branch (via summary); delete in settings |
| Windsurf / Devin Desktop, memories and rules [W1][W2] (via summary) | Memories: "sharing and persisting context across conversations". Rules: "persistent instructions that shape how the agent behaves" | Memories: context Cascade "believes is useful to remember". Rules: short, specific instructions | Rules: generic rules; "Prefer a skill for anything the agent needs only sometimes" | Memories: Cascade, or on request. Rules: people | Memories: per workspace. Rules: `always_on`, `glob`, `model_decision`, `manual` | Rules files "limited to 12000 characters each" |
| Devin, Knowledge [D1] (via summary; page says deprecated, moving to Skills) | "tips, advice, and instructions that Devin can reference in all sessions" | "common bugs and their associated solutions, code conformance practices, deployment workflows, testing workflows, how to interact with proprietary tools" | Broad items: "targeted at one workflow or action" | People; "Devin will automatically suggest Knowledge to remember based on your feedback in chat" | "when its current work is related to the specified triggers"; pinned to a repository, always | Devin suggests updates |
| Gemini CLI, `GEMINI.md` and memory files [M1][M2] (M1 via summary, M2 direct) | "persists durable facts, user preferences, and project details" | "shared project instructions go in repository `GEMINI.md` files, private project notes go in the per-project private memory folder, and cross-project personal preferences go in the global `~/.gemini/GEMINI.md`"; examples: a user preference, "project-wide architectural decisions", aliases | "avoids duplicating the same fact across multiple memory tiers" | People and the agent, by editing the files | Concatenated and sent "with every prompt"; subdirectory files just in time | `/memory show`, `/memory reload`; no expiry stated |
| Cline, Memory Bank [L1][L2] (direct, from the docs source on GitHub) | "structured documentation system that helps Cline maintain context across sessions" | Six files: project brief, product context, `activeContext.md` ("Current focus, recent changes, next steps"), system patterns, tech context, `progress.md` ("What works, what's left, known issues") | Nothing | Cline, with the person | "I MUST read ALL memory bank files at the start of EVERY task" | "update memory bank" reviews every file |
| Amp, `AGENTS.md` [P1] (via summary) | "guidance on codebase structure, build/test commands, and conventions" | Architecture, build and test commands, internal API overviews, review and release steps; personal preferences in the user-level file | Not stated | People; Amp offers to generate and can update "based on what I told you in this thread" | Working directory and parents always; subtree files when a file there is read; mentioned files with `globs` only after a matching read | Not stated. No memory feature on the page |
| CoALA, Sumers et al. [R1] (direct) | Taxonomy | Working: "active and readily available information … for the current decision cycle". Episodic: "experience from earlier decision cycles". Semantic: "an agent's knowledge about the world and itself". Procedural: "implicit knowledge stored in the LLM weights, and explicit knowledge written in the agent's code" | Writing to procedural memory "is significantly riskier … as it can easily introduce bugs or allow an agent to subvert its designers' intentions" | Agent (learning actions); designer initializes procedural | Retrieval actions in the decision cycle | Not prescribed |
| Reflexion, Shinn et al. [R2] (direct) | Improve across trials without weight updates | "reflective text in an episodic memory buffer"; evaluation signals "amplified to natural language experience summaries" | Not stated | The agent's self-reflection model, after a failed trial | Every later trial of the same task | "bound mem by a maximum number of stored experiences, (usually set to 1-3)" |
| Generative Agents, Park et al. [R3] (direct) | Believable long-run behaviour | "memory stream … a comprehensive list of the agent's experiences"; reflections, "higher-level, more abstract thoughts" | Nothing; retrieval selects | The agent | Retrieval by "relevance, recency, and importance" | Recency decay in retrieval, no deletion |
| MemGPT, Packer et al. [R4] (direct) and Letta docs [R5][R6] (via summary) | Manage a bounded context like virtual memory | Working context / memory blocks: "key facts, preferences, and other important information about the user and the persona". Archival: "arbitrary length text objects". Recall: message history | Not stated | The agent, by function calls; developers by API | Blocks always in context; archival and recall "queried on-demand via tools" | Block character limit; queue eviction |
| LangGraph and LangMem [N1][N2] (via summary) | Long-term memory across threads | Semantic "Facts" (user profile or collection); episodic "Experiences" (few-shot examples, "successful interactions"); procedural "Instructions" (the system prompt, refined by reflection on feedback) | Not stated | The agent, "in the hot path" or "in the background" | By namespace and key, or search | Profile overwritten; collection updated |

Sources:

- [C1] https://code.claude.com/docs/en/memory
- [C2] https://code.claude.com/docs/en/best-practices
- [C3] https://code.claude.com/docs/en/features-overview
- [A1] https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool
- [A2] https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
- [A3] https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
- [G1] https://agents.md
- [O1] https://developers.openai.com/codex/guides/agents-md, which redirected to
  https://learn.chatgpt.com/docs/agent-configuration/agents-md
- [O2] https://developers.openai.com/codex/memories, which redirected to
  https://learn.chatgpt.com/docs/customization/memories?surface=app
- [U1] https://cursor.com/docs/context/rules
- [U2] https://cursor.com/changelog/1-0
- [H1] https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions
- [H2] https://docs.github.com/en/copilot/concepts/agents/copilot-memory
- [W1] https://docs.windsurf.com/windsurf/cascade/memories, which redirected to
  https://docs.devin.ai/desktop/cascade/memories
- [W2] https://docs.windsurf.com/plugins/cascade/memories, which redirected to
  https://docs.devin.ai/windsurf/plugins/cascade/memories
- [D1] https://docs.devin.ai/product-guides/knowledge
- [M1] https://geminicli.com/docs/cli/gemini-md/
- [M2] https://geminicli.com/docs/tools/memory/
- [L1] https://raw.githubusercontent.com/cline/cline/main/docs/best-practices/memory-bank.mdx
- [L2] https://raw.githubusercontent.com/cline/prompts/main/.clinerules/memory-bank.md
- [P1] https://ampcode.com/docs/customize/agents-md
- [R1] https://arxiv.org/html/2309.02427 (Sumers, Yao, Narasimhan, Griffiths, "Cognitive Architectures for
  Language Agents")
- [R2] https://arxiv.org/html/2303.11366 ("Reflexion: Language Agents with Verbal Reinforcement Learning")
- [R3] https://arxiv.org/html/2304.03442 ("Generative Agents: Interactive Simulacra of Human Behavior")
- [R4] https://arxiv.org/html/2310.08560 ("MemGPT: Towards LLMs as Operating Systems")
- [R5] https://docs.letta.com/guides/agents/memory-blocks
- [R6] https://docs.letta.com/guides/agents/archival-memory
- [N1] https://docs.langchain.com/oss/python/concepts/memory
- [N2] https://langchain-ai.github.io/langmem/concepts/conceptual_guide/

### Notes

- Claude Code splits the subject in two, and the split matters here. Its instruction files hold what the recipe
  calls gotchas and traps: "Places where the code is misleading: an old pattern that's everywhere but that new
  code must not copy, or a directory that looks unused but isn't" [C2]. Its auto memory explicitly skips
  "debugging fixes" and holds the person, their feedback, project state and references [C1].
- `S/references/memory.md:78-80` says Claude's auto memory is written by Claude under
  `~/.claude/projects/<project>/memory/` and stays on one machine, marked "(Claude's docs; not tested)". [C1]
  confirms both: "Each project gets its own memory directory at `~/.claude/projects/<project>/memory/`" and
  "Auto memory is machine-local … Files are not shared across machines or cloud environments."
- [C1] adds a `modified` timestamp to memory files to show "how current the fact is"; the recipe forbids dates
  in entries (memory.md:56-57) and relies on git history.
- Only Copilot Memory [H2] expires entries by default and ties a fact to code citations. Every other source
  prunes by hand or by size cap.
- Codex [O2] and Windsurf [W1] both tell the reader to put what the team requires in checked-in files, not in
  generated memory. That is the same separation `memory.md:78-80` draws.

### Synthesis

Content categories across the sources:

| Category | Sources that store it | CoALA type |
| --- | --- | --- |
| A. Standing instructions: commands, conventions, layout | `CLAUDE.md`, AGENTS.md, Codex, Cursor rules, Copilot instructions, Windsurf rules, `GEMINI.md`, Amp | Procedural |
| B. Guidance scoped to a path, topic or trigger, loaded on demand | `.claude/rules` with `paths`, Cursor file-pattern and description rules, Copilot `applyTo`, nested AGENTS.md, Amp `globs`, Devin triggers | Procedural |
| C. Lessons from a correction or a failure | `CLAUDE.md` ("same mistake a second time"), Claude auto memory `feedback`, Cursor ("same mistake repeatedly"), Devin ("common bugs and their associated solutions", suggestions from feedback), memory tool ("lessons from past interactions"), Reflexion, LangMem procedural reflection | Procedural, written from episodic |
| D. The person: role, expertise, preferences | Claude auto memory `user`, Copilot user-level preferences, Gemini global file, MemGPT working context, Letta `human` block, LangGraph profile, Codex global AGENTS.md, Cursor user rules | Semantic |
| E. Project state: progress, next steps, deadlines | Memory tool protocol, long-running harness files, Cline `activeContext.md` and `progress.md`, Claude auto memory `project`, context-engineering notes | Working memory persisted; episodic |
| F. Decisions and facts not derivable from code | Claude auto memory `project`, `CLAUDE.md` ("Decisions that aren't in the repo"), Copilot repository facts, Gemini, Cline `systemPatterns.md` | Semantic |
| G. Pointers to outside systems | Claude auto memory `reference` | Semantic |
| H. Raw experience: transcripts, trajectories, chat summaries | Generative Agents memory stream, Letta recall and archival, Codex memories, LangMem episodic | Episodic |

Where they agree:

- Do not store what the agent can read from the code ([C1], [C2], [U1]); do not store secrets ([A1], [O2]).
- Keep always-loaded text short and move scoped content to something loaded on demand ([C1], [C3], [U1], [W1],
  [P1]).
- A repeated mistake is the trigger for writing something down ([C1], [C3], [U1], [D1]).
- What a team requires goes in checked-in files, not in agent-generated stores ([O2], [W1], [C1]).

Where they differ:

- Who writes: people for instruction files; the agent for everything named "memory". No product source has the
  recipe's arrangement of agent-written, person-reviewed, committed entries; Copilot repository facts are the
  nearest (agent-written, shared with collaborators, deletable by write-access users) but unreviewed.
- Scope of "memory": products and the API use the word for D, E, G and H. Research uses it for all of A to H.
  The instruction-file sources hold A, B, C and F and mostly do not call them memory (Claude Code's page title
  is the exception, covering both).
- State: the memory tool, the harness post and Cline make progress tracking the main use; Claude Code and
  Copilot instruction files exclude task-specific content.
- Expiry: Copilot only.

How "trap" maps:

- The recipe's memory is category C restricted to B's scope: a lesson from a correction or failure, in one
  area, loaded on demand through a pointer in `AGENTS.md`, with A, F and anything enforceable routed elsewhere.
  In CoALA's terms it is procedural memory written from an episode, with a person's review as the control for
  the risk CoALA names for agent-written procedural memory [R1]. In product terms it is what Claude Code calls a
  path-scoped rule and Cursor a file-scoped rule, not what any fetched product calls "memory".
- D, the person: excluded, deliberately. "A secret, or about a person → Nowhere in the repository"
  (memory.md:15); "One person's preference → That person's harness settings, outside the repository"
  (memory.md:17); "never secrets or personal data" (SKILL.md:60); the Person axis and its test, "if two people
  in the team could reasonably want different values, it is not shared" (design.md:21-24).
- E, project state: excluded, deliberately. "What this session did, what is left to do, or a plan → The tracker,
  or the commit message" (memory.md:16); "holds no session note" (review.md:24-25); a benchmark run that "wrote
  its session note into a versioned memory file" is reported as a fault (agents-md.md:73).
- F, decisions: excluded where documentation exists, deliberately (memory.md:22), admitted where it does not
  (memory.md:26-28). The admission is what breaks "memory holds traps".
- G, external pointers: not addressed. No row routes them, and nothing says they are excluded.
- H, raw experience: excluded by form, deliberately: an entry "states what is true now, with no dates, authors
  or log of changes: the history holds those" (memory.md:56-57).
- A harness's own memory is acknowledged as a separate thing and left alone (memory.md:78-80), which is where D,
  E and G go in Claude Code [C1].

The reason given for all the exclusions is one sentence: "Most of what a session learns has an owner already,
and a copy in memory drifts from it" (memory.md:10).

## Categorizing traps

Two further sources, both fetched directly on 2026-11-17 and quoted from their own text. Both are third-party
skills, primary only for themselves:

- [X1] https://raw.githubusercontent.com/mattpocock/skills/main/skills/engineering/retro/SKILL.md, a
  retrospective skill. It sorts a session's findings into seven categories, each with a "_Use when_" trigger
  tied to an observed symptom, and proposes "improvements to the coding agent's **environment**".
- [X2] https://raw.githubusercontent.com/mindmuxai/brain.md/main/skills/brain-page/SKILL.md, a project knowledge
  base. "Each page's `category` must be one of" six, chosen by subject.

### The two sources against the routing table

Rows are those of `S/references/memory.md:13-23`.

| Source category | Its trigger or boundary | Row that already takes it | Covered? |
| --- | --- | --- | --- |
| [X1] Navigation | "Are there hidden dependencies between files? Would a **navigation pointer** make it easier? _Use when_ the session took a long time to find a piece of information" | Hidden dependency: named as a memory trap, "the file that must change with another" (memory.md:26). Pointer: "Nowhere; name the file where a reader needs it" (memory.md:18), and `AGENTS.md` Where things go and Deep dive (AGENTS.template.md:55-62, 79-81) | Half. A slow search is a cost, not a correction or a failure, so it fails "Did it happen" (memory.md:35) and no row sends a pointer anywhere specific |
| [X1] Automated checks | "_Use when_ the agent made a mistake an automated check could have caught, or the repo has no guardrail at all" | "Something a check, a hook or the policy can enforce → The check, the hook or `policy.yaml`" (memory.md:19) | Yes |
| [X1] Coding standards | Mechanical violation "gets a deterministic check"; `CODING_STANDARDS.md` only for "genuine **judgement calls**"; enforced by the reviewer agent. "_Use when_ the reviewer agent failed to catch a mistake" | Mechanical: memory.md:19. Judgement: "a standard" → documentation (memory.md:22), or a rule in `AGENTS.md` (memory.md:20) | Yes for the destination. The implementer/reviewer split has no counterpart in the table |
| [X1] Global AGENTS.md | Move steering instructions out. "_Use when_ the AGENTS.md file is particularly large" | Not a kind of fact. The recipe handles it as upkeep: the five-gotcha cap (agents-md.md:54-55) and the reverse move in memory.md:66 | As upkeep, not as a row |
| [X1] Tool economy | "did the agent make expensive tool calls that could be streamlined? … _Use when_ the agent made an expensive tool call" | None. Nearest: "A procedure an agent repeats → A skill" (memory.md:21) | No |
| [X1] No-ops | "instructions in steering files that don't modify the agent's behavior" | Not a kind of fact. The entry test "Would a fresh session fall into it again without the entry?" (memory.md:34) and "is still true" (review.md:24-25) are the same test at admission and at review | As upkeep, not as a row |
| [X1] Information access | "Teeing dev server logs, readonly access to third-party services. _Use when_ a crucial piece of information was not available to the agent" | None. The remedy is a hook, a policy allow or a harness setting; memory.md:19 is about enforcing, not granting, and the Environment axis is "Per machine, or by the organization" (design.md:20) | No |
| [X2] `project` | "A concrete piece of work with a goal, scope, and an end … dated progress belongs in the timeline" | "What this session did, what is left to do, or a plan → The tracker, or the commit message" (memory.md:16) | Yes, sent away |
| [X2] `concept` | "A concept, term, mechanism, or unfinished idea/question; also the deliberate home for durable knowledge that does not fit another type" | "how a feature behaves" → documentation (memory.md:22) | Yes for mechanisms. The recipe has no catch-all; memory is the narrow last row, not the default |
| [X2] `decision` | "An established judgment and its reasoning (the most common)" | "a decision" → documentation (memory.md:22); memory without documentation (memory.md:26-28) | Yes |
| [X2] `person` | "A relevant person / role, their preferences and responsibilities" | "A secret, or about a person → Nowhere in the repository" (memory.md:15); "One person's preference" (memory.md:17) | Yes, and opposite: [X2] commits the page, the recipe forbids it |
| [X2] `organization` | "A company, school, foundation, government body, standards organization, or internal team relevant to the work" | None | No |
| [X2] `reference` | "An external resource / object of analysis worth keeping" | None | No |

Neither the table nor the recipe's examples cover: navigation pointers for information that was slow to find,
tool economy, information access, organizations and external references. The first three share a property: the
session did not go wrong, it went slowly or blind, so "learned from a correction or a failure" (memory.md:3)
excludes them by definition. Whether that is intended is not said.

[X2] has no category for a lesson learned from a failure; it would land in `concept`. [X1] has no category for
"write the fact down for next time" at all: every category ends in a change to the environment (a pointer, a
check, a rule for the reviewer, a tool, an access grant). On that point [X1] and the recipe agree: memory.md:10
says "Most of what a session learns has an owner already", and the table's first eight rows are destinations
other than memory.

The three schemes sort by different keys. [X2] sorts by subject. [X1] sorts by remedy and recognizes each by a
symptom. The recipe sorts by owner and has no symptoms: its rows start from "The fact is" (memory.md:13), which
assumes the session already knows it has a fact worth routing.

### Kinds of trap the recipe already names

Collected from memory.md:25-26 (three kinds), its two example entries (memory.md:50-53) and the 48 entries under
`## Traps` in `cookbook/src` (15 in `agents/connect.groovy`, 22 in `ralph/ralph-tui.groovy`, 5 in
`beads/tracker.groovy`, 3 in `beads/viewer.groovy`, 2 in `git/worktrees.groovy`, 1 in
`git/templates/README.template.md`). The grouping is this note's; an entry can fit two groups. Prefixes:
`connect` = `cookbook/src/agents/connect.groovy`, `ralph` = `cookbook/src/ralph/ralph-tui.groovy`, `tracker` =
`cookbook/src/beads/tracker.groovy`, `viewer` = `cookbook/src/beads/viewer.groovy`, `worktrees` =
`cookbook/src/git/worktrees.groovy`.

| How the agent goes wrong | Named in memory.md | Entries in recipe help |
| --- | --- | --- |
| Runs a plausible command that does something else or something destructive | "the command that looks right and is not" (memory.md:25-26); "`make db-reset` drops the local database without asking" (memory.md:52) | "`bd init` and `--stealth` can add unmanaged excludes or stage/commit source files" (tracker:104) |
| Changes one thing and misses what is coupled to it, or shares state it took for private | "the file that must change with another" (memory.md:26); "Number a new migration from `main`, not from the branch" (memory.md:50) | "Branches, the stash and anything else kept in the clone are shared by every worktree" (`cookbook/src/git/templates/README.template.md:40`); a cook that changes a hook entry "turns it off for everyone" (connect:212-216); Codex runs the main checkout's hooks, not the run worktree's (ralph:279) |
| Edits what something else owns | "the generated directory that must stay unedited" (memory.md:26) | "a hook the team writes there is replaced on the next cook" (connect:247-249) |
| Takes a success signal for success: exit 0, "done", or no message, with the work not done | None | "exit 0 with stale data after export failure" (viewer:63); "Push/pull can exit 0 because they skipped synchronization" (tracker:96); completes a task on `<promise>COMPLETE</promise>` "even in a sentence saying it is not done" (ralph:238); "reports a failed commit nowhere" (ralph:246); `--model` dropped, "saying nowhere" (ralph:257); "stop as `no_tasks`, with no error" (ralph:260); "Codex prints no warning when it skips them" (connect:215) |
| Relies on a setting that is present but not in effect: untrusted, shadowed or overridden | None | Allow rules apply "only in a workspace someone has trusted" (connect:210, ralph:235); a `CLAUDE.md` "makes Claude Code skip that `AGENTS.md`" (connect:217); `.beads/config.local.yaml` "can override repository policy" (tracker:95); a worktree-local config "can shadow main-store discovery" (viewer:61); `.worktreeinclude` no longer copied (connect:245) |
| Relies on a guard that is narrower than it reads | None | "Deny rules are prefix rules: `git -C . push` gets past `git push`" (ralph:283); a `git worktree add` through Bash "bypasses them" (connect:221); "Codex has no roles" (connect:219, ralph:271); no permission checks for an unmatched `--agent` (ralph:242) |
| Uses a tool whose reach is wider than expected | None | Linters "see every worktree's copy of the sources" (worktrees:71); `docker build` sends every worktree (worktrees:73); "ralph-tui commits with `git add -A`" (ralph:244) |
| Hits a limit of the environment and reads it as a fault in the work | None | A Codex `workspace-write` worker "has no network, localhost included" (ralph:274); a fetch "slower than ten seconds … is stopped every time" (connect:240); checks that print rate-limit words to stderr "look rate-limited" (ralph:285) |
| Leaves or inherits stale state | None | "A reused worktree keeps its old tip" (connect:238); worktrees of subagents are never removed (connect:223); `bin/clean` "leaves its workers' worktrees and branches" (ralph:287); completion marks left uncommitted (ralph:289) |

Findings:

- memory.md names three kinds, all about an action the agent takes. The recipe's own help sections are mostly
  about three kinds memory.md does not name: a success signal that is false, a setting not in effect, and a guard
  with a hole.
- Those three are what `docs/README.md:36` already defines: "a setup that looks right and is not, or a guarantee
  a reader assumes Cook gives and it does not". The project's documentation definition and the skill's memory
  examples describe different halves of the same set, and an adopter sees only the skill's half.
- The recipe-help traps were written by the recipe's author from testing, not by a session after a correction.
  They show what kinds exist; they are not evidence of what adopters' sessions record. This repository has no
  `.agents/memory/` to check against.

### Do the Part 2 sources categorize entries by type?

Some do, none by how the agent went wrong.

| Source | Types | Key | Recorded as |
| --- | --- | --- | --- |
| Claude Code auto memory [C1] | `user`, `feedback`, `project`, `reference` | Subject and origin | "a `type` field in the memory file's frontmatter"; the docs' example file names carry it too (`user_role.md`, `feedback_testing.md`) |
| CoALA [R1] | Working, episodic, semantic, procedural | Cognitive function | Separate memory modules |
| LangGraph and LangMem [N1][N2] | Semantic, episodic, procedural | Cognitive function, after CoALA's terms | Namespaces; profile or collection |
| Copilot Memory [H2] | Repository facts, user-level preferences | Scope | Separate stores with different sharing and deletion rules |
| Gemini CLI [M2] | Shared project instructions, private project notes, cross-project personal preferences | Scope and visibility | Three file locations |
| Cline [L1] | Six files: brief, product, active context, system patterns, tech context, progress | Subject | File names |
| MemGPT and Letta [R4][R5] | Blocks by label (`persona`, `human`), archival, recall | Subject, and whether in context | Labelled blocks and stores |
| [X2] | Six page categories; timeline entries have a second axis, `--kind <decision\|evidence\|reversal\|note>` | Subject; kind of change | Frontmatter `category`, timeline `kind` |
| [X1] | Seven categories | Remedy, recognized by symptom | Not stored; a checklist for the retrospective |

Comparison:

- Claude's `user`, `project` and `reference` match [X2]'s `person`, `project` and `reference`, and all three are
  what the recipe sends away or does not address (Synthesis, categories D, E, G). Claude's `feedback` is the
  only type that contains the recipe's trap, and it is wider: it includes "approaches you confirm" [C1], which
  are not hazards.
- In CoALA's terms every trap is procedural. The taxonomy does not divide procedural memory further.
- No fetched source subdivides lessons from failures by failure mode. [X1] is the only one keyed on an observed
  symptom, and its categories name remedies, not kinds of entry.
- Where a type is stored (Claude, [X2], Copilot, Gemini), it decides something mechanical: which file, who sees
  it, how it loads or expires. No source stores a type only as a label.

### Options for a trap taxonomy

Options only.

1. Kinds as recognition aids. Extend the three kinds in memory.md:25-26 to a short list such as the table above,
   each with a symptom trigger in [X1]'s form ("the command exited 0 and the work was not done"). Nothing is
   stored per entry; the list only helps a session notice it has met a trap. Costs one paragraph; adds no
   upkeep.
2. Triggers on the routing table. Give each row of memory.md:13-23 a "you saw" symptom beside "The fact is", so
   the table can be entered from an observation, as [X1] is, and not only from a fact already recognized.
3. A tag per entry. A fixed word at the start of each bullet, or frontmatter as [C1] does. Lets `check.sh` or a
   review count and query kinds. Costs upkeep, invites misfiling, and no fetched source stores a type that
   decides nothing.
4. File structure by kind, as `memory/<kind>/<area>.md` or headings inside an area file. Conflicts with "One file
   per area … read only when a task touches the area" (memory.md:4-5): a task finds a file by area, not by kind.
5. Kind as the exit route. Pair each kind with the owner it should end at under memory.md:61-67: a false success
   signal with a check, a do-not-edit path with a policy deny, a coupled change with a test, a setting not in
   effect with the documentation or `S/references/investigate.md`. The taxonomy then serves pruning, not filing.
6. No taxonomy; widen the examples. Add one example each of the three unnamed kinds to memory.md:25-26 and stop
   there.
7. Decide the non-failure categories first. Navigation cost, tool economy and information access fail "learned
   from a correction or a failure". Either state that they are out and name their owners (`AGENTS.md` Deep dive,
   a skill, the policy or a hook), or widen the definition; a taxonomy built before that choice inherits the
   gap.

## Options for tightening the definition

Options only; nothing was changed.

1. Define "trap" once, on its own, in `S/references/glossary.md`, in hazard terms the text already uses: what
   goes wrong when you do what, and what to do instead (AGENTS.template.md:77), where the wrong action looks
   right (memory.md:25-26). State there that a gotcha is a trap any task can hit and that a product trap belongs
   to documentation.
2. Replace "Shown by the code" (memory.md:18) with a two-part test such as [C2]'s: could a session find it by
   reading, and would it go and look. A trap is what fails the second part.
3. Change "can enforce" (memory.md:19) to match the exit rule (memory.md:64): enforce it now if that is cheap;
   otherwise the entry stays until a check enforces it. Or change the two examples (memory.md:50-53) to ones no
   check or policy entry could carry.
4. Give rows 22 and 23 a test that does not depend on guessing what people look up: for example, the trap exists
   only because of how an agent works (fresh context, no memory of the last run), or the fact concerns the
   repository's tooling, not the product's behaviour.
5. Either drop the no-documentation fallback (memory.md:26-28) or say in the first sentence that memory holds
   traps and, where no documentation exists, the decisions a page would own; then say whether the entry tests
   apply to those.
6. Add rows for what fits nothing: an external pointer (to "Where things go" in `AGENTS.md` or the README), a
   deadline (the tracker), and a one-area convention that is not a hazard (documentation, or admit it under "how
   to work in one area").
7. Say what an area is: a directory, a module or a tool, named so `AGENTS.md` can state the question the file
   answers.
8. Align the trigger wording in all places: "a correction or a failure" (memory.md:3), "a person's correction, a
   failed check, a command's output" (memory.md:35), "a correction" (`AGENTS.md:66`).
9. Rename the recipe-help section or the memory term so "trap" has one meaning for an adopter: the `## Traps`
   sections are "known faults of the versions written for" (cookbook/README.md:254).
10. Say in one line that the layout's memory is not what harnesses call memory: it holds no profile of the
    person, no project state and no pointers, which `memory.md:78-80` implies but states only for Claude Code.

## Unverified

- Pages marked "via summary" were read through a summarizing fetcher; quotes from them are short and were not
  checked against page source: [A2], [A3], [G1], [O1], [O2], [U1], [U2], [H1], [W1], [W2], [D1], [M1], [P1],
  [R5], [R6], [N1], [N2].
- Cursor memories: no current documentation page was found (`cursor.com/docs/context/memories` served the rules
  page, `cursor.com/docs/agent/memories` returned 404). Only the 1.0 changelog [U2] was read. Whether the
  feature still exists, who approves a memory and how it is stored are unverified.
- Windsurf: where auto-generated memories are stored and whether they are committed are not on the fetched
  pages. A search-result summary gave `~/.codeium/windsurf/memories/` and "not committed"; unverified.
- Gemini CLI: [M2] does not mention a `save_memory` tool; it describes the agent editing Markdown memory files
  directly. Whether `save_memory` was removed or renamed is unverified. The summary of [M1] reported a notice
  that Gemini CLI was replaced by Antigravity CLI on 2026-06-18; not checked further.
- Cline: `docs.cline.bot` did not resolve. [L1] is the documentation source file in the `cline/cline`
  repository on `main`, assumed to be what the site publishes.
- Devin Knowledge: the summary reported the page as deprecated in favour of Skills; ingestion of repository
  files such as `AGENTS.md` into Knowledge is not on the page.
- OpenAI: both Codex URLs redirected to `learn.chatgpt.com`; the content was read there.
- Copilot Memory: the claim that citations are checked against the current branch is via summary; the expiry,
  creation and example quotes are direct.
- The reading of gotcha and pitfall as kinds of trap (Part 1) is inference from agents-md.md:54 and
  memory.md:20-23; no file states it.
- CoALA's mapping onto the recipe (procedural memory written from an episode) is this note's inference, not a
  claim of the paper.
