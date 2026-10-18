package thecook.cli.run

import thecook.cli.terminal.CookCommand
import thecook.infra.CookToml

/** Cook's entry point: it wires the kitchen's repositories into the command line. */
final class Main {
  private Main() {
  }

  static void main(String[] args) {
    def stdin = System.console() ? new BufferedReader(new InputStreamReader(System.in)) : null
    def home = new File(System.getProperty('user.home'))
    def command = new CookCommand(new PrintWriter(System.out, true), new PrintWriter(System.err, true),
        { File kitchen -> CookToml.repositories(kitchen, home) }, stdin)
    System.exit(command.execute(args))
  }
}
