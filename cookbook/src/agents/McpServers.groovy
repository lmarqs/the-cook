package io.github.lmarqs.cookbook.agents

import groovy.yaml.YamlSlurper

/** Translates the shared MCP server definitions into the project configuration each client reads. */
class McpServers {
  static final String PATH = '.agents/mcp/servers.yaml'
  private static final List<String> HTTP = ['type', 'url', 'headers', 'bearerTokenEnv', 'oauth']
  private static final List<String> STDIO = ['type', 'command', 'args', 'env', 'envFrom']

  static Map parse(String text) {
    def renders = [claude: [:], codex: [:]]

    if (text == null) {
      return renders
    }

    def servers

    try {
      servers = new YamlSlurper().parseText(text)
    } catch (RuntimeException ignored) {
      fail('', 'expected a YAML map of server names to definitions')
    }

    if (!(servers instanceof Map)) {
      fail('', 'expected a YAML map of server names to definitions')
    }

    servers.each { name, server ->
      if (!name.trim() || !(server instanceof Map)) {
        fail(name, 'expected a named server definition')
      }

      def type = server.containsKey('type') ? server.type : server.containsKey('url') ? 'http' : 'stdio'
      Map claude = [type: type]
      Map codex = [:]

      switch (type) {
        case 'http':
          fields(name, server, HTTP)
          http(name, server, claude, codex)
          break
        case 'stdio':
          fields(name, server, STDIO)
          stdio(name, server, claude, codex)
          break
        default:
          fail("${name}.type", 'expected http or stdio')
      }

      renders.claude[name] = claude
      renders.codex[name] = codex
    }

    renders
  }

  private static void http(String name, Map server, Map claude, Map codex) {
    String url = requiredString(name, server, 'url')
    def uri

    try {
      uri = new URI(url)
    } catch (URISyntaxException ignored) {
      fail("${name}.url", 'expected an HTTP or HTTPS URL')
    }

    if (!(uri.scheme?.toLowerCase(Locale.ROOT) in ['http', 'https']) || !uri.host) {
      fail("${name}.url", 'expected an HTTP or HTTPS URL')
    }

    claude.url = codex.url = url

    if (server.containsKey('headers')) {
      def headers = stringMap(name, server, 'headers')
      claude.headers = new LinkedHashMap(headers)
      codex.http_headers = headers
    }

    if (server.containsKey('bearerTokenEnv')) {
      def variable = variableName("${name}.bearerTokenEnv", server.bearerTokenEnv)

      if (claude.headers?.keySet()?.any { it.equalsIgnoreCase('Authorization') }) {
        fail("${name}.bearerTokenEnv", 'already supplied by headers.Authorization')
      }

      claude.headers = (claude.headers ?: [:]) + [Authorization: 'Bearer ${' + variable + '}']
      codex.bearer_token_env_var = variable
    }

    if (server.containsKey('oauth')) {
      oauth("${name}.oauth", server.oauth, claude, codex)
    }
  }

  private static void oauth(String path, Object options, Map claude, Map codex) {
    if (!(options instanceof Map)) {
      fail(path, 'expected a map of OAuth settings')
    }

    fields(path, options, ['clientId', 'callbackPort', 'scopes'])
    def nativeOptions = [:]

    if (options.containsKey('clientId')) {
      nativeOptions.client_id = requiredString(path, options, 'clientId')
    }

    if (options.containsKey('callbackPort')) {
      def port = options.callbackPort

      if (!(port instanceof Integer) || !(port in 1..65535)) {
        fail("${path}.callbackPort", 'expected an integer between 1 and 65535')
      }

      nativeOptions.callback_port = port
    }

    if (nativeOptions) {
      codex.oauth = nativeOptions
    }

    claude.oauth = new LinkedHashMap(options)

    if (options.containsKey('scopes')) {
      def scopes = strings(path, options, 'scopes')

      if (!scopes || scopes.any { !(it ==~ /[^\s]+/) }) {
        fail("${path}.scopes", 'expected a list of nonempty scope names without whitespace')
      }

      claude.oauth.scopes = scopes.join(' ')
      codex.scopes = scopes
    }
  }

  private static void stdio(String name, Map server, Map claude, Map codex) {
    claude.command = codex.command = requiredString(name, server, 'command')

    if (server.containsKey('args')) {
      claude.args = codex.args = strings(name, server, 'args')
    }

    if (server.containsKey('env')) {
      def env = stringMap(name, server, 'env')
      claude.env = new LinkedHashMap(env)
      codex.env = env
    }

    if (server.containsKey('envFrom')) {
      def variables = strings(name, server, 'envFrom')
      def env = claude.env ?: [:]
      variables.each { variable ->
        variableName("${name}.envFrom", variable)

        if (env.containsKey(variable)) {
          fail("${name}.envFrom", "${variable} is already defined")
        }

        env[variable] = '${' + variable + '}'
      }

      claude.env = env
      codex.env_vars = variables
    }
  }

  private static void fields(String path, Map data, List<String> allowed) {
    def unknown = data.keySet() - allowed

    if (unknown) {
      fail("${path}.${unknown.first()}", "expected one of ${allowed.join(', ')}")
    }
  }

  private static String requiredString(String path, Map data, String key) {
    def value = data[key]

    if (!(value instanceof String) || !value.trim()) {
      fail("${path}.${key}", 'expected a nonempty string')
    }

    value
  }

  private static List strings(String path, Map data, String key) {
    def value = data[key]

    if (!(value instanceof List) || !value.every { it instanceof String }) {
      fail("${path}.${key}", 'expected a list of strings')
    }

    value
  }

  private static Map stringMap(String path, Map data, String key) {
    def value = data[key]

    if (!(value instanceof Map) || !value.every { k, v -> k && v instanceof String }) {
      fail("${path}.${key}", 'expected a map of names to strings')
    }

    value
  }

  private static String variableName(String path, Object value) {
    if (!(value instanceof String) || !(value ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
      fail(path, 'expected an environment variable name')
    }

    value
  }

  private static void fail(String field, String message) {
    throw new IllegalArgumentException("${PATH}: ${field ? field + ': ' : ''}${message}")
  }
}
