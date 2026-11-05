package thecook

import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import groovy.toml.TomlSlurper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files
import java.nio.file.LinkOption

class AgentsSubagentsCookTest {
  private static final String REVIEWER = '''\
---
name: reviewer
description: Reviews a staged diff for correctness bugs.
claude:
  tools: [Read, Grep, Glob]
  model: opus
codex:
  model: gpt-5.5
  model_reasoning_effort: high
---
You review diffs. Report only bugs you can show with a failing input.
'''

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  @Test void rendersEachSharedSubagentForEveryHarnessAndLinksThem() {
    def kitchen = seeded()
    write(kitchen, '.agents/agents/reviewer.md', REVIEWER)

    def connected = connect(kitchen, ['claude', 'codex'])

    assert connected.changes.findAll { it.contains('agents/reviewer') || it.endsWith('/agents') } == [
      '.agents/adapters/claude/agents/reviewer.md',
      '.agents/adapters/codex/agents/reviewer.toml',
      '.claude/agents',
      '.codex/agents',
    ]
    connected.commit()
    assert read(kitchen, '.claude/agents/reviewer.md') == '''\
---
name: "reviewer"
description: "Reviews a staged diff for correctness bugs."
tools:
- "Read"
- "Grep"
- "Glob"
model: "opus"
---
You review diffs. Report only bugs you can show with a failing input.
'''
    assert new TomlSlurper().parseText(read(kitchen, '.codex/agents/reviewer.toml')) == [
      name: 'reviewer',
      description: 'Reviews a staged diff for correctness bugs.',
      developer_instructions: 'You review diffs. Report only bugs you can show with a failing input.\n',
      model: 'gpt-5.5',
      model_reasoning_effort: 'high',
    ]
    assert Files.readSymbolicLink(new File(kitchen, '.claude/agents').toPath()).toString() ==
    '../.agents/adapters/claude/agents'
    assert Files.readSymbolicLink(new File(kitchen, '.codex/agents').toPath()).toString() ==
    '../.agents/adapters/codex/agents'
    def ignore = read(kitchen, '.gitignore')
    assert ignore.contains('!/.claude/skills\n!/.claude/agents\n')
    assert ignore.contains('!/.codex/rules\n!/.codex/agents\n')
    assert connect(kitchen, ['claude', 'codex']).changes == []
  }

  @Test void keepsAPromptWithQuotesBackslashesAndBlankLinesIntactForCodex() {
    def kitchen = seeded()
    def prompt = 'Say "hi" and \\"\\"\\" then\\\\path.\n\nSecond paragraph.\n'
    write(kitchen, '.agents/agents/echo.md', "---\nname: echo\ndescription: Echoes.\n---\n${prompt}")

    connect(kitchen, ['codex']).commit()

    assert new TomlSlurper().parseText(read(kitchen, '.codex/agents/echo.toml')).developer_instructions == prompt
  }

  @Test void rendersOnlyForTheHarnessesConnected() {
    def kitchen = seeded()
    write(kitchen, '.agents/agents/reviewer.md', REVIEWER)

    connect(kitchen, ['claude']).commit()

    assert new File(kitchen, '.agents/adapters/claude/agents/reviewer.md').isFile()
    assert !new File(kitchen, '.agents/adapters/codex/agents').exists()
    assert !exists(kitchen, '.codex/agents')
  }

  @Test void removesTheRendersAndLinksOfADeletedSubagent() {
    def kitchen = seeded()
    write(kitchen, '.agents/agents/reviewer.md', REVIEWER)
    connect(kitchen, ['claude', 'codex']).commit()
    assert new File(kitchen, '.agents/agents/reviewer.md').delete()

    def again = connect(kitchen, ['claude', 'codex'])

    assert again.changes.findAll { it.contains('agents/reviewer') || it.endsWith('/agents') } == [
      '.agents/adapters/claude/agents/reviewer.md',
      '.agents/adapters/codex/agents/reviewer.toml',
      '.claude/agents',
      '.codex/agents',
    ]
    again.commit()
    ['.claude/agents', '.codex/agents', '.agents/adapters/claude/agents', '.agents/adapters/codex/agents'].each {
      assert !exists(kitchen, it)
    }
    assert !read(kitchen, '.gitignore').contains('/agents\n')
  }

  @Test void seedsTheSubagentDirectoryAndRendersNothingFromItsKeepFile() {
    def kitchen = seeded()
    assert new File(kitchen, '.agents/agents/.gitkeep').isFile()

    def connected = connect(kitchen, ['claude', 'codex'])

    assert !connected.changes.any { it.contains('/agents/') || it.endsWith('/agents') }
  }

  @Test void refusesASubagentThatBreaksTheSchema() {
    [
      'unknown key': [
        'bad.md',
        "---\nname: bad\ndescription: d\ncursor: {}\n---\nx\n",
        '.agents/agents/bad.md: has unknown keys [cursor]; use name, description, claude and codex'
      ],
      'no description': ['bad.md', "---\nname: bad\n---\nx\n", '.agents/agents/bad.md: needs a description'],
      'no name': ['bad.md', "---\ndescription: d\n---\nx\n", '.agents/agents/bad.md: needs a name'],
      'block not a map': [
        'bad.md',
        "---\nname: bad\ndescription: d\nclaude: opus\n---\nx\n",
        ".agents/agents/bad.md: claude must be a map of that harness's keys"
      ],
      'stem differs': [
        'other.md',
        "---\nname: bad\ndescription: d\n---\nx\n",
        '.agents/agents/other.md: is named after its name: rename it bad.md'
      ],
      'no front matter': [
        'bad.md',
        'just a prompt\n',
        '.agents/agents/bad.md: opens with front matter between two --- lines'
      ],
    ].each { label, entry ->
      def (file, text, message) = entry
      def kitchen = seeded(label.replace(' ', '-'))
      write(kitchen, ".agents/agents/${file}", text)

      def error = shouldFail { connect(kitchen, ['claude']) }

      assert error.message.contains(message), label
    }
  }

  @Test void refusesADirectoryAmongTheSubagents() {
    def kitchen = seeded()
    write(kitchen, '.agents/agents/team/reviewer.md', REVIEWER)

    def error = shouldFail { connect(kitchen, ['claude']) }

    assert error.message.contains(
    '.agents/agents/team is a directory; keep each subagent in a file directly under .agents/agents/')
  }

  private File seeded(String name = 'kitchen') {
    def kitchen = folder.newFolder(name)
    publishDirectory(kitchen, 'agents')
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit()
    kitchen
  }

  private static def connect(File kitchen, List<String> harnesses) {
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: harnesses])
  }

  private static String read(File kitchen, String path) {
    new File(kitchen, path).text
  }

  private static boolean exists(File kitchen, String path) {
    Files.exists(new File(kitchen, path).toPath(), LinkOption.NOFOLLOW_LINKS)
  }

  private static void write(File kitchen, String path, String text) {
    def file = new File(kitchen, path)
    file.parentFile.mkdirs()
    file.text = text
  }

  private static Throwable shouldFail(Closure action) {
    try {
      action()
    } catch (Throwable error) {
      return error
    }

    throw new AssertionError('Expected a failure')
  }
}
