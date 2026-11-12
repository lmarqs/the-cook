package thecook.cli.terminal

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

/**
 * Cook's own grammar: its commands and the options that precede a recipe. Parsing stops at the first operand, so
 * everything after a recipe is that recipe's arguments and is never read as a Cook option.
 */
final class Grammar {
  /** The release the build wrote, which `--version` prints and the usage names recipes at. */
  static final String VERSION = Grammar.getResourceAsStream('version.txt').text.trim()

  private static final String COOKBOOK = 'io.github.lmarqs.cookbook'
  private static final String DOCS = 'https://lmarqs.github.io/the-cook/'

  private Grammar() {
  }

  static CommandLine commandLine() {
    def line = new CommandLine(new RootArgs())
        .setStopAtPositional(true)
        .setOverwrittenOptionsAllowed(false)
    def usage = line.commandSpec.usageMessage()
    usage.footer((usage.footer() + startHere()) as String[])
    line
  }

  // The recipes are named at this release, so a person or an agent can run them as printed.
  private static List<String> startHere() {
    [
      '',
      "Recipes come from the repositories the kitchen's cook.toml lists, from Maven Central without one, or from " +
      ".groovy files inside the kitchen, so a team cooks its own. The official cookbook is ${COOKBOOK}, with the " +
      "artifacts agents, docs, git, hello and ralph; help lists an artifact's recipes:",
      "  cook help ${COOKBOOK}:agents:${VERSION}",
      'Start here: a person cooks hello#world, and a coding agent reads hello#agent:',
      "  cook ${COOKBOOK}:hello:${VERSION}#world",
      "  cook help ${COOKBOOK}:hello:${VERSION}#agent",
      "Docs: ${DOCS}",
      "CLI reference: ${DOCS}docs/features/cli.html",
    ]*.toString()
  }
}

/** The directory Cook cooks in, accepted by every command. */
class KitchenOption {
  @Option(names = ['-C', '--kitchen'], paramLabel = 'DIR',
  description = ['Cook in DIR, as if started there; relative paths resolve against it.'])
  String kitchen
}

/** A command that accepts `-C`/`--kitchen`. */
abstract class KitchenScoped {
  @Mixin KitchenOption kitchen = new KitchenOption()
}

@Command(
name = 'cook',
subcommands = [HelpArgs, CheckArgs, ReplicateArgs, UpdateArgs, DiscardArgs],
description = ["Cooks recipes: Cook's options come first, then the recipe, then the recipe's own arguments."],
footer = [
  '',
  "A recipe is coordinates, group:artifact:version#recipe, resolved from the repositories listed in the kitchen's " +
  'cook.toml, or a .groovy path inside the kitchen. Arguments follow their recipe as --name=value or --name value, ' +
  'and a yes-or-no argument as --name or --name=false. A word that contains / or : or ends with .groovy starts the ' +
  'next recipe:',
  '  cook a:b:1.0.0#x --name value ./setup.groovy',
  '--help takes no other arguments; read a recipe with help <recipe>.',
]
)
class RootArgs extends KitchenScoped {
  @Option(names = ['--dry-run'], description = ['Report the proposed changes without writing.'])
  boolean dryRun

  @Option(names = ['--batch-approve'],
  description = ["Show the recipes' confirm texts and the diff, then write without asking."])
  boolean batchApprove

  @Option(names = ['--version'], description = ["Print Cook's version; takes no other arguments."])
  boolean version

  @Option(names = ['--force'], description = [
    'Replace a file that a recipe writes from a template the first time it is cooked, instead of refusing.'
  ])
  boolean force

  @Option(names = ['--skip'], description = [
    'Keep a file that a recipe writes from a template the first time it is cooked, and cook the rest.'
  ])
  boolean skip

  @Parameters(paramLabel = 'RECIPE [ARGUMENT]', description = ['The recipe, then its arguments.'])
  List<String> operands = []
}

@Command(name = 'help', description = ["Print each recipe's help in order, without running it; alone, this usage."])
class HelpArgs extends KitchenScoped {
  @Parameters(paramLabel = 'RECIPE', description = ['A recipe: coordinates or a .groovy path.'])
  List<String> recipes = []
}

@Command(name = 'discard', description = ["Remove what cook.lock lists for each recipe, without running it."])
class DiscardArgs extends KitchenScoped {
  @Option(names = ['--dry-run'], description = ['Report the proposed changes without writing.'])
  boolean dryRun

  @Option(names = ['--batch-approve'], description = ['Show the diff, then write without asking.'])
  boolean batchApprove

  @Parameters(paramLabel = 'NAME', description = ['A recipe by the name cook.lock writes.'])
  List<String> recipes = []
}

@Command(name = 'replicate', description = ['Cook recipes again from cook.lock.'])
class ReplicateArgs extends KitchenScoped {
  @Option(names = ['--dry-run'], description = ['Report the proposed changes without writing.'])
  boolean dryRun

  @Option(names = ['--batch-approve'],
  description = ["Show the recipes' confirm texts and the diff, then write without asking."])
  boolean batchApprove

  @Parameters(paramLabel = 'NAME', description = ['A recipe by the name cook.lock writes; none is every one.'])
  List<String> recipes = []
}

@Command(name = 'update', description = ['Cook recipes from cook.lock again at their newest release.'])
class UpdateArgs extends KitchenScoped {
  @Option(names = ['--dry-run'], description = ['Report the proposed changes without writing.'])
  boolean dryRun

  @Option(names = ['--batch-approve'],
  description = ["Show the recipes' confirm texts and the diff, then write without asking."])
  boolean batchApprove

  @Option(names = ['--major'], description = ['Allow updates outside the compatible release range.'])
  boolean major

  @Parameters(paramLabel = 'NAME', description = ['A recipe by the name cook.lock writes; none is every one.'])
  List<String> recipes = []
}

@Command(name = 'check', description = [
  'Compare the kitchen with what cook.lock records, without writing; exit 1 when they differ.'
])
class CheckArgs extends KitchenScoped {
  @Parameters(paramLabel = 'NAME', description = ['A recipe by the name cook.lock writes; none is every one.'])
  List<String> recipes = []
}
