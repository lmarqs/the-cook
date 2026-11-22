package io.github.lmarqs.cookbook.agents

import groovy.yaml.YamlSlurper

/** One shared MCP server from `.agents/mcp/servers.yaml`, reached over HTTP or by running a stdio command. */
class McpServer {
  static final String PATH = '.agents/mcp/servers.yaml'
  private static final List<String> HTTP = ['type', 'url', 'headers', 'bearerTokenEnv', 'oauth']
  private static final List<String> STDIO = ['type', 'command', 'args', 'env', 'envFrom']

  /** The name each client lists the server by. */
  String name

  /** `http` or `stdio`; `http` when `type` is absent and `url` is set. */
  String type

  /** The HTTP endpoint, with the literal request headers, the variable holding a bearer token, and OAuth settings. */
  String url
  Map<String, String> headers
  String bearerTokenEnv
  McpOAuth oauth

  /** The stdio executable and its arguments, literal environment values, and variables forwarded from the developer. */
  String command
  List<String> args
  Map<String, String> env
  List<String> envFrom

  /** The servers `text` defines, in file order; none for absent, empty or comment-only text. */
  static List<McpServer> parse(String text) {
    if (text == null || text.readLines().every { !it.trim() || it.trim().startsWith('#') }) {
      return []
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

    servers.collect { name, server ->
      if (!(name instanceof String) || !(name ==~ /[A-Za-z0-9_-]+/) || !(server instanceof Map)) {
        fail(name.toString(), 'expected a server name of letters, digits, _ or - with a definition')
      }

      def type = server.containsKey('type') ? server.type : server.containsKey('url') ? 'http' : 'stdio'

      switch (type) {
        case 'http':
          fields(name, server, HTTP)
          http(name, server)
          break
        case 'stdio':
          fields(name, server, STDIO)
          stdio(name, server)
          break
        default:
          fail("${name}.type", 'expected http or stdio')
      }

      new McpServer(server.findAll { key, value -> key != 'oauth' } + [name: name, type: type,
        oauth: server.containsKey('oauth') ? new McpOAuth(server.oauth) : null])
    }
  }

  private static void http(String name, Map server) {
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

    if (server.containsKey('headers')) {
      stringMap(name, server, 'headers')
    }

    if (server.containsKey('bearerTokenEnv')) {
      variableName("${name}.bearerTokenEnv", server.bearerTokenEnv)

      if (server.headers?.keySet()?.any { it.equalsIgnoreCase('Authorization') }) {
        fail("${name}.bearerTokenEnv", 'already supplied by headers.Authorization')
      }
    }

    if (server.containsKey('oauth')) {
      oauth("${name}.oauth", server.oauth)
    }
  }

  private static void oauth(String path, Object options) {
    if (!(options instanceof Map)) {
      fail(path, 'expected a map of OAuth settings')
    }

    fields(path, options, ['clientId', 'callbackPort', 'scopes'])

    if (options.containsKey('clientId')) {
      requiredString(path, options, 'clientId')
    }

    if (options.containsKey('callbackPort')) {
      def port = options.callbackPort

      if (!(port instanceof Integer) || !(port in 1..65535)) {
        fail("${path}.callbackPort", 'expected an integer between 1 and 65535')
      }
    }

    if (options.containsKey('scopes')) {
      def scopes = strings(path, options, 'scopes')

      if (!scopes || scopes.any { !(it ==~ /[^\s]+/) }) {
        fail("${path}.scopes", 'expected a list of nonempty scope names without whitespace')
      }
    }
  }

  private static void stdio(String name, Map server) {
    requiredString(name, server, 'command')

    if (server.containsKey('args')) {
      strings(name, server, 'args')
    }

    if (server.containsKey('env')) {
      stringMap(name, server, 'env')
    }

    if (server.containsKey('envFrom')) {
      def variables = strings(name, server, 'envFrom')
      def names = (server.env ?: [:]).keySet().toSet()
      variables.each { variable ->
        variableName("${name}.envFrom", variable)

        if (!names.add(variable)) {
          fail("${name}.envFrom", "${variable} is already defined")
        }
      }
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

/** The OAuth settings of an HTTP server: a public client ID, its fixed callback port and the requested scopes. */
class McpOAuth {
  String clientId
  Integer callbackPort
  List<String> scopes
}
