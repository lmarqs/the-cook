package thecook.cli.terminal

import picocli.CommandLine
import thecook.cookbook.Argument
import thecook.cookbook.CookbookCook
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookDiscard
import thecook.cookbook.CookbookReplicate
import thecook.cookbook.CookbookRepositories
import thecook.cookbook.CookbookUpdate
import thecook.cookbook.Coordinates
import thecook.cookbook.ExistingSeed
import thecook.cookbook.Proposal
import thecook.cookbook.RecipeLoader
import thecook.cookbook.RecipeRequest
import thecook.cookbook.RecipeSource
import thecook.model.Diagnostic
import thecook.model.RecipeTexts

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.function.Function

/**
 * Cook's command line. `stdin` answers the approval question; without it, writing needs `--batch-approve`. Coordinates
 * resolve from the repositories that `repositories` reads for a kitchen. It exits 1 when it declines to write or a
 * check finds a difference, and 2 on any error, so a caller can tell a difference from a failure.
 */
final class CookCommand {
  private static final Set<String> BEFORE_RECIPE =
  ['-C', '--kitchen', '--dry-run', '--batch-approve', '--force', '--skip'] as Set
  private static final Map<String, String> STATUS =
  [create: 'Created', update: 'Updated', delete: 'Deleted', replace: 'Replaced', keep: 'Kept']
  private static final String VERSION = CookCommand.getResourceAsStream('version.txt').text.trim()

  private final PrintWriter stdout
  private final PrintWriter stderr
  private final Function<File, CookbookRepositories> repositories
  private final BufferedReader stdin
  // The command word once the invocation names one; null while cooking or before parsing.
  private String commandName

  CookCommand(PrintWriter stdout, PrintWriter stderr, Function<File, CookbookRepositories> repositories,
  BufferedReader stdin = null) {
    this.stdout = stdout
    this.stderr = stderr
    this.repositories = repositories
    this.stdin = stdin
  }

  int execute(String[] args) {
    Closure<Boolean> command
    commandName = null

    try {
      command = parse(args)
    } catch (UsageError error) {
      diagnose(error.message)
      return 2
    } catch (IllegalArgumentException error) {
      diagnose(error.message)
      return 2
    }

    try {
      if (!command()) {
        return 1
      }

      stdout.flush()
      return 0
    } catch (Throwable error) {
      // An Error, such as a recipe's failed assert, would otherwise end the JVM with 1, which reads as a difference.
      diagnose(error.message ?: error.class.simpleName)
      return 2
    }
  }

  // A diagnostic on stderr names its source, as the message standard asks: `cook: `, then the command under one.
  private void diagnose(String message) {
    stderr.println("cook: ${commandName ? commandName + ': ' : ''}${message}")
    stderr.flush()
  }

  // The command the invocation names, as an action that returns false when it declines to write.
  private Closure<Boolean> parse(String[] invocation) {
    if (invocation.toList() == ['--help']) {
      return usage()
    }

    if (invocation.any { it.split('=', 2)[0] == '--help' }) {
      throw new UsageError('--help takes no other arguments', null, 'use ./cook help <recipe> to read a recipe')
    }

    def line = Grammar.commandLine()
    CommandLine.ParseResult parsed

    try {
      parsed = line.parseArgs(invocation)
    } catch (CommandLine.ParameterException error) {
      def name = error.commandLine.commandSpec.name()
      commandName = name == 'cook' ? null : name
      throw new UsageError(error.message ?: 'Invalid arguments')
    }

    RootArgs root = line.command

    if (root.version) {
      if (invocation.length > 1) {
        throw new UsageError('--version takes no other arguments')
      }

      return { -> stdout.println("cook ${VERSION}"); true }
    }

    def chosen = parsed.subcommand()
    def command = chosen?.commandSpec()?.userObject()
    commandName = chosen?.commandSpec()?.name()

    if (command != null && (root.dryRun || root.batchApprove || root.force || root.skip)) {
      throw new UsageError('--dry-run, --batch-approve, --force and --skip go before a recipe, not before a command')
    }

    if (root.force && root.skip) {
      throw new UsageError('--force and --skip exclude each other')
    }

    def commandKitchen = command instanceof KitchenScoped ? command.kitchen.kitchen : null
    def kitchens = [root.kitchen.kitchen, commandKitchen].findAll { it != null }

    if (kitchens.size() > 1) {
      throw new UsageError('Option may only be supplied once: --kitchen')
    }

    def kitchen = kitchen(kitchens ? kitchens.first() : null)
        .toAbsolutePath()
        .toFile()

    switch (command) {
      case null:
        return recipes(kitchen, root)
      case HelpArgs:
        return help(kitchen, ((HelpArgs) command).recipes)
      case CheckArgs:
        return check(kitchen, kitchens ? kitchens.first() : null, ((CheckArgs) command).recipes)
      case DiscardArgs:
        return discard(kitchen, (DiscardArgs) command)
      case ReplicateArgs:
        return replicate(kitchen, (ReplicateArgs) command)
      case UpdateArgs:
        return update(kitchen, (UpdateArgs) command)
      default:
        throw new IllegalStateException("Unhandled command: ${command}")
    }
  }

  private Closure<Boolean> usage() {
    return { -> stdout.print(Grammar.commandLine().usageMessage); true }
  }

  private Closure<Boolean> recipes(File kitchen, RootArgs root) {
    def first = root.operands ? root.operands.first() : null

    if (first == null) {
      throw new UsageError('A recipe is required')
    }

    if (!isOperand(first)) {
      throw new UsageError("Not a recipe: ${first}", 'a recipe is coordinates or a .groovy path')
    }

    List<String> operands = []
    List<List<Argument>> arguments = []
    String bare = null
    root.operands.each { token ->
      if (token.startsWith('-')) {
        def argument = recipeArgument(token)
        arguments.last().add(argument)
        bare = argument.form == Argument.Form.BARE ? argument.name : null
      } else if (isOperand(token)) {
        operands.add(recipeOperand(token, bare))
        arguments.add([])
        bare = null
      } else {
        if (bare == null) {
          throw new UsageError("Not a recipe: ${token}", 'recipe arguments start with --')
        }

        arguments.last()[-1] = Argument.separate(bare, token)
        bare = null
      }
    }
    def requests = [operands, arguments].transpose()
    .collect { new RecipeRequest((String) it[0], (List<Argument>) it[1]) }
    def existing = root.force ? ExistingSeed.REPLACE : root.skip ? ExistingSeed.KEEP : ExistingSeed.REFUSE
    return {
      ->
      def proposal = new CookbookCook(kitchen, repositories.apply(kitchen), existing).propose(requests)
      write(proposal, root.dryRun, root.batchApprove)
    }
  }

  private Closure<Boolean> discard(File kitchen, DiscardArgs args) {
    if (!args.recipes) {
      throw new UsageError('A recipe is required')
    }

    lockNames(args.recipes)
    return { -> write(new CookbookDiscard(kitchen).propose(args.recipes), args.dryRun, args.batchApprove) }
  }

  private Closure<Boolean> replicate(File kitchen, ReplicateArgs args) {
    lockNames(args.recipes)
    return {
      ->
      def proposal = new CookbookReplicate(kitchen, repositories.apply(kitchen)).propose(args.recipes)
      write(proposal, args.dryRun, args.batchApprove)
    }
  }

  private Closure<Boolean> update(File kitchen, UpdateArgs args) {
    lockNames(args.recipes)
    return {
      ->
      def updater = new CookbookUpdate(kitchen, repositories.apply(kitchen))
      def plan = updater.plan(args.recipes, args.major)
      plan.each { recipe ->
        if (!recipe.known) {
          diagnose(Diagnostic.text("warning: skipping ${recipe.name}; releases are unknown",
              "repository ${recipe.unlisted.join(', ')} cannot list releases",
              "cook the release you want by its coordinates, such as ./cook ${recipe.source}"))
        } else if (recipe.to != null) {
          stdout.println("Update ${recipe.name} ${recipe.from} -> ${recipe.to}")
        } else if (recipe.newestOverall != null) {
          stdout.println("Holding ${recipe.name} ${recipe.from}; ${recipe.newestOverall} needs --major")
        }
      }

      if (!plan.any { it.to != null }) {
        stdout.println('Already at the newest releases.')
        return true
      }

      def written = write(updater.propose(plan), args.dryRun, args.batchApprove)
      !args.dryRun && written
    }
  }

  // Checking cooks the recipes again as replicate does, and keeps the proposal as the report.
  // `given` is the kitchen as the person wrote it, so the suggested fix runs where they ran check.
  private Closure<Boolean> check(File kitchen, String given, List<String> names) {
    lockNames(names)
    return {
      ->
      def proposal = new CookbookReplicate(kitchen, repositories.apply(kitchen)).propose(names)
      def actions = proposal.actions

      if (actions.isEmpty()) {
        stdout.println('Already cooked.')
        return true
      }

      review(proposal, proposal.changes, false)
      actions.each { path, action -> stdout.println('Would ' + action + ' ' + path) }
      def files = actions.size() == 1 ? '1 file differs' : "${actions.size()} files differ"
      def fix = (['./cook replicate'] + (given == null ? [] : ['-C', given]) + names).join(' ')
      diagnose(Diagnostic.text("${files} from what cook.lock records; nothing was written",
          'check cooks each recipe again from the source and arguments cook.lock records',
          "run ${fix}, review the diff and commit it"))
      false
    }
  }

  // The commands that read cook.lock take recipe names only: no recipe arguments, and Cook's options go first.
  private void lockNames(List<String> names) {
    names.findAll { it.startsWith('-') }.each { operand ->
      def name = operand.split('=', 2)[0]
      throw new UsageError(name in BEFORE_RECIPE ?
      "${name} goes before the first recipe" :
      "${commandName} takes no recipe arguments: ${operand}")
    }
  }

  private static boolean isOperand(String token) {
    token.contains('/') || token.contains(':') || token.endsWith('.groovy')
  }

  private static String recipeOperand(String token, String bare) {
    if (!Coordinates.written(token)) {
      return token
    }

    try {
      Coordinates.parse(token)
    } catch (IllegalArgumentException error) {
      if (bare != null) {
        throw new UsageError("Not a recipe: ${token}", null, "to give it to --${bare}, write --${bare}=${token}")
      }

      throw error
    }

    token
  }

  private static Argument recipeArgument(String token) {
    def name = token.split('=', 2)[0]

    if (name in BEFORE_RECIPE) {
      throw new UsageError("${name} goes before the first recipe")
    }

    if (!(name ==~ /--[a-z][a-z0-9-]*/)) {
      throw new UsageError("Unknown option for the recipe: ${token}")
    }

    if (!token.contains('=')) {
      return Argument.bare(name.substring(2))
    }

    def value = token.substring(token.indexOf('=') + 1)

    if (value.isEmpty()) {
      throw new UsageError("Missing value for ${name}")
    }

    Argument.inline(name.substring(2), value)
  }

  private Closure<Boolean> help(File kitchen, List<String> operands) {
    if (operands.isEmpty()) {
      return usage()
    }

    // Each page takes the kitchen's repositories on demand, so help on paths alone never reads them.
    List<Closure<String>> pages = operands.collect { String operand ->
      if (operand.startsWith('-')) {
        throw new UsageError("help takes recipes only: ${operand}")
      }

      if (Coordinates.written(operand)) {
        def coordinates = Coordinates.parse(operand)
        return { Closure<CookbookRepositories> repositories -> addressHelp(coordinates, repositories()) }
      }

      if (!isOperand(operand)) {
        throw new UsageError("Not a recipe: ${operand}", 'a recipe is coordinates or a .groovy path')
      }

      RecipeLoader.name(kitchen.toPath()
          .toAbsolutePath()
          .normalize(), operand)
      def script = path(kitchen.toPath(), operand, 'recipe')

      if (!Files.isRegularFile(script)) {
        throw new IllegalArgumentException("Recipe file not found: ${operand}")
      }

      return { Closure<CookbookRepositories> repositories -> recipeHelp(script, operand) }
    }
    return {
      ->
      CookbookRepositories resolved = null
      def resolve = { -> resolved = resolved ?: repositories.apply(kitchen) }
      stdout.println(pages.collect { it(resolve) }.join('\n\n'))
      true
    }
  }

  private static String addressHelp(Coordinates coordinates, CookbookRepositories repositories) {
    def artifact = repositories.artifact(coordinates)
    def recipe = coordinates.recipe

    if (recipe == null) {
      return frontPage(artifact)
    }

    def texts = RecipeSource.texts(artifact.recipeSource(recipe), "${recipe}.groovy")

    if (texts == null) {
      throw new IllegalArgumentException(
      Diagnostic.text("${coordinates} has no help", 'its first import carries no @help'))
    }

    render(coordinates.toString(), texts)
  }

  private static String frontPage(CookbookArtifact artifact) {
    def coordinates = artifact.coordinates
    [
      coordinates.artifactAddress,
      artifact.readme?.trim(),
      section('Recipes', artifact.recipes.collect { coordinates.recipeAddress(it) }),
      section('Models', artifact.models.collect { coordinates.group + '.' + coordinates.artifact + '.' + it }),
    ].findAll { it != null }.join('\n\n')
  }

  // A titled list of indented lines, or nothing when there are none.
  private static String section(String title, List<String> lines) {
    lines ? ([title] + lines.collect { '  ' + it }).join('\n') : null
  }

  private static String recipeHelp(Path script, String operand) {
    def texts = RecipeSource.texts(Files.readString(script), script.fileName.toString())

    if (texts == null) {
      throw new IllegalArgumentException(Diagnostic.text("${operand} has no help", 'its first import carries no @help'))
    }

    render(operand, texts)
  }

  private static String render(String name, RecipeTexts texts) {
    def params = texts.params.collect { param ->
      def facts = [
        param.defaultValue == null ? 'required' : param.defaultValue ? "default: ${param.defaultValue}" : 'optional',
        param.choices ? "choices: ${param.choices.join(', ')}" : null,
        param.many ? 'repeatable' : null,
      ].findAll { it != null }
      '  ' + param.names.collect { '--' + it + '=<' + param.type + '>' }.join(', ') +
      '\n      ' + (param.doc ?: '') + '; ' + facts.join('; ')
    }.join('\n')
    [
      name,
      texts.help,
      params ? "Parameters\n${params}" : null,
      section('Ingredients', texts.ingredients),
      texts.confirm != null ? "Confirm\n${indent(texts.confirm)}" : null,
      texts.yieldText != null ? "After cooking\n${indent(texts.yieldText)}" : null,
    ].findAll { it != null }.join('\n\n')
  }

  // Reviews the proposal, asks unless approved in advance, writes it and reports each path; false when declined.
  private boolean write(Proposal proposal, boolean dryRun, boolean batchApprove) {
    def actions = proposal.actions

    if (actions.isEmpty()) {
      stdout.println('Already cooked.')
      return true
    }

    review(proposal, proposal.changes)

    if (!dryRun && !batchApprove && !approved()) {
      return false
    }

    if (!dryRun) {
      proposal.commit()
    }

    actions.each { path, action -> stdout.println((dryRun ? 'Would ' + action : STATUS[action]) + ' ' + path) }

    if (!dryRun) {
      proposal.recipes.findAll { it.yieldText != null }.each {
        stdout.println()
        stdout.println("After cooking ${it.address}")
        stdout.println(indent(it.yieldText))
      }
    }

    true
  }

  // Each changed source, each recipe's consequence the diff cannot show when there is an approval to give, then
  // the diff itself: what one approval covers.
  private void review(Proposal proposal, Collection<String> paths, boolean confirm = true) {
    proposal.changedSources.each {
      def release = Coordinates.written(it.address)
      def source = release ? 'release' : 'source'
      def context = release ? ' under the same version' : ''
      stdout.println("Changed source ${it.address}")
      stdout.println("  cook.lock recorded sha256 ${it.recorded}; the ${source} now has sha256 ${it.current}")
      stdout.println("  its sources changed${context}; review the diff${confirm ? ' before approving' : ''}")
      stdout.println()
    }
    proposal.recipes.findAll { confirm && it.confirm != null }.each {
      stdout.println("Confirm ${it.address}")
      stdout.println(indent(it.confirm))
      stdout.println()
    }
    paths.each { path ->
      proposal.patch(path).each {
        stdout.println(it)
      }
    }
    stdout.println()
    stdout.flush()
  }

  // Whether the person approved the writes; a missing terminal or another answer declines them.
  private boolean approved() {
    if (stdin == null) {
      diagnose(Diagnostic.text('approval required; nothing was written',
          'there is no terminal to ask in, and --batch-approve was not given',
          'run interactively to review the diff, or run again with --batch-approve before the first recipe'))
      return false
    }

    stdout.print('Write these changes? [y/N] ')
    stdout.flush()
    def yes = stdin.readLine()
        ?.trim()
        ?.toLowerCase() in ['y', 'yes']

    if (!yes) {
      diagnose(Diagnostic.text('nothing was written', 'the answer was not y or yes'))
    }

    yes
  }

  private static String indent(String text) {
    text.split(/\r\n|\r|\n/, -1)
        .collect { it.isEmpty() ? it : "  ${it}" }
        .join('\n')
  }

  private static Path path(Path kitchen, String value, String option) {
    try {
      return kitchen.resolve(value)
    } catch (InvalidPathException error) {
      throw new UsageError("Invalid path for ${option}: ${error.reason}")
    }
  }

  private static Path kitchen(String value) {
    if (value == null) {
      return Path.of('')
    }

    if (value.isEmpty()) {
      throw new UsageError('Missing value for --kitchen')
    }

    def directory = path(Path.of(''), value, '--kitchen')

    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException("Kitchen directory not found: ${value}")
    }

    directory
  }

  // A mistake in the command line; unless the message names a next step, the next step is reading the usage.
  private static final class UsageError extends IllegalArgumentException {
    UsageError(String what, String why = null, String next = 'run: ./cook help') {
      super(Diagnostic.text(what, why, next))
    }
  }
}
