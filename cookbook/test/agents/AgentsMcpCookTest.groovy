package thecook

import static org.junit.Assert.assertThrows
import static thecook.Cookbook.publishDirectory
import static thecook.Kitchens.cook

import groovy.json.JsonSlurper
import groovy.toml.TomlSlurper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import thecook.cookbook.CookLock
import thecook.cookbook.KeyTarget

class AgentsMcpCookTest {
  private static final String CONNECT = 'io.github.lmarqs.cookbook:agents#connect'
  private static final String SOURCE = '.agents/mcp/servers.yaml'
  private static final String CONFIG = '.agents/adapters/codex/config.toml'

  @Rule public TemporaryFolder folder = new TemporaryFolder()
  File kitchen

  @Before void publish() {
    kitchen = folder.newFolder()
    publishDirectory(kitchen, 'agents')
  }

  @Test void seedsAnEmptyMapAndPreservesExistingDefinitions() {
    def seed = { cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#seed', [:]).commit() }
    seed()
    assert file(SOURCE).text.trim() == '{}'
    file(SOURCE).text = 'local: { command: example }\n'
    seed()
    assert file(SOURCE).text == 'local: { command: example }\n'
  }

  @Test void adoptsSharedServersAndRemovesOnlyTheirOwnedEntries() {
    def plain = ['datadog', 'github', 'n8n', 'supabase'].collect {
      "${it}: { url: 'https://${it}.example/mcp' }\n"
    }.join()
    write(SOURCE, plain + '''\
slack:
  type: http
  url: https://slack.example/mcp
  oauth: { clientId: project-app, callbackPort: 3118, scopes: [search, read] }
''')
    def manual = [type: 'http', url: 'https://manual.example/mcp']
    write('.mcp.json', '{"mcpServers":{"manual":{"type":"http","url":"https://manual.example/mcp"},' +
        '"github":{"type":"http","url":"https://github.example/mcp"}}}')
    write(CONFIG, '[mcp_servers.manual]\nurl = "https://manual.example/mcp"\n')

    connect().commit()
    def claude = claudeServers()
    def codex = codexServers()
    def owned = CookLock.parse(file('cook.lock').text).owned(CONNECT)
    ['datadog', 'github', 'n8n', 'supabase', 'slack'].each { name ->
      assert claude[name].url == "https://${name}.example/mcp"
      assert codex[name].url == claude[name].url
      assert new KeyTarget('.mcp.json', ['mcpServers', name]) in owned
      assert new KeyTarget(CONFIG, ['mcp_servers', name]) in owned
    }
    assert claude.slack.oauth == [clientId: 'project-app', callbackPort: 3118, scopes: 'search read']
    assert codex.slack.oauth == [client_id: 'project-app', callback_port: 3118]
    assert codex.slack.scopes == ['search', 'read']
    assert !new TomlSlurper().parse(file(CONFIG)).containsKey('mcp_oauth_callback_port')
    assert connect().changes == []

    file(SOURCE).text = plain
    connect().commit()
    assert claudeServers() == claude - [slack: claude.slack]
    assert codexServers() == codex - [slack: codex.slack]

    connect(['codex']).commit()
    assert claudeServers() == [manual: manual]
    assert codexServers().keySet() == ['datadog', 'github', 'n8n', 'supabase', 'manual'] as Set
    assert file(SOURCE).delete()
    connect().commit()
    assert codexServers() == [manual: [url: manual.url]]
    assert claudeServers() == [manual: manual]
  }

  @Test void rendersLocalArgumentsAndCredentialReferencesForEachClient() {
    write(SOURCE, '''\
local:
  command: example
  args: ['with space', '--switch']
  env: { MODE: test }
  envFrom: [EXAMPLE_KEY]
remote:
  url: https://remote.example/mcp
  bearerTokenEnv: REMOTE_TOKEN
  headers: { X-Mode: test }
''')
    connect().commit()
    assert claudeServers().local == [type: 'stdio', command: 'example', args: ['with space', '--switch'],
      env: [MODE: 'test', EXAMPLE_KEY: '${EXAMPLE_KEY}']]
    assert codexServers().local == [command: 'example', args: ['with space', '--switch'],
      env: [MODE: 'test'], env_vars: ['EXAMPLE_KEY']]
    assert claudeServers().remote.headers == ['X-Mode': 'test', Authorization: 'Bearer ${REMOTE_TOKEN}']
    assert codexServers().remote == [url: 'https://remote.example/mcp', http_headers: ['X-Mode': 'test'],
      bearer_token_env_var: 'REMOTE_TOKEN']
  }

  @Test void rejectsInvalidFieldsBeforeWritingEitherClient() {
    [
      '{ url: https://example.org, unexpected: true }': 'unexpected',
      '{ url: https://example.org, command: example }': 'command',
      '{ url: https://example.org, headers: { authorization: fixed }, bearerTokenEnv: TOKEN }': 'uthorization',
      '{ url: https://example.org, oauth: { callbackPort: 65536 } }': 'callbackPort',
    ].each { spec, field ->
      write(SOURCE, "broken: ${spec}\n")
      def error = assertThrows(IllegalArgumentException) { connect() }
      assert error.message.contains('broken')
      assert error.message.contains(field)
      assert !file('.mcp.json').exists()
      assert !file(CONFIG).exists()
    }
  }

  private def connect(List<String> harnesses = ['claude', 'codex']) {
    cook(kitchen, 'io.github.lmarqs.cookbook:agents:0.1.0#connect', [harness: harnesses])
  }

  private Map claudeServers() {
    new JsonSlurper().parse(file('.mcp.json')).mcpServers
  }

  private Map codexServers() {
    new TomlSlurper().parse(file(CONFIG)).mcp_servers
  }

  private File file(String path) {
    new File(kitchen, path)
  }

  private void write(String path, String text) {
    file(path).parentFile.mkdirs()
    file(path).text = text
  }
}
