package thecook.cli

import com.sun.net.httpserver.HttpServer
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.security.MessageDigest

class WrapperTest {
  private static final String ARCH = [amd64: 'x64', x86_64: 'x64', aarch64: 'arm64'][System.getProperty('os.arch')]
  private static final String OS = System.getProperty('os.name') == 'Linux' ? 'linux' :
  (System.getProperty('os.name').startsWith('Mac') ? 'macos' : null)
  private static final String TARGET = "${OS}-${ARCH}".toString()
  private static final String IMAGE = OS == 'macos' ? 'cook.app/Contents/MacOS/cook' : 'cook/bin/cook'
  private static final String PACKAGE = "/the-cook-1.2.3-${TARGET}.tar.gz"
  private static final List<String> TOOLS = ['uname', 'mkdir', 'mktemp', 'rm', 'rmdir', 'mv', 'tar', 'gzip', 'cut']

  @Rule public TemporaryFolder folder = new TemporaryFolder()

  // The wrapper runs only the Linux x64 and arm64 and macOS arm64 packages, so it has nothing to download elsewhere.
  @Before void runsOnAPlatformWithAPackage() {
    Assume.assumeTrue(TARGET in ['linux-x64', 'linux-arm64', 'macos-arm64'])
  }

  @Test void downloadsVerifiesCachesAndRunsThePinnedPackageInTheCurrentDirectory() {
    def content = fakePackage()

    serve(content) { String url, List<String> requests ->
      def cook = wrapper(url, sha256(content))
      def kitchen = folder.newFolder('a kitchen')

      def first = run(cook, kitchen, System.getenv('PATH'), 'help', 'two words', '--line=x')

      assert first.status == 0 : first.err
      assert first.out == "${kitchen.canonicalPath}\nhelp\ntwo words\n--line=x\n"
      assert requests == [PACKAGE]
      assert new File(folder.root, "cache/the-cook/1.2.3/${TARGET}/${IMAGE}").canExecute()
      assert new File(folder.root, 'cache/the-cook/1.2.3').list() as List == [TARGET]

      def second = run(cook, kitchen, System.getenv('PATH'), 'help')

      assert second.out == "${kitchen.canonicalPath}\nhelp\n"
      assert requests == [PACKAGE]
    }
  }

  @Test void refusesAPackageWhoseSha256DiffersAndCachesNothing() {
    def content = fakePackage()

    serve(content) { String url, List<String> requests ->
      def actual = sha256(content)
      def pinned = '0' * 64
      def cook = wrapper(url, pinned)

      def result = run(cook, folder.root, System.getenv('PATH'), 'help')

      assert result.status == 2
      assert result.out == ''
      assert result.err.endsWith("cook: ${PACKAGE.substring(1)} has sha256 ${actual}, not the ${pinned} this " +
      'wrapper pins; refusing to run it\n')
      assert requests == [PACKAGE]
      assert !new File(folder.root, 'cache/the-cook').list()
    }
  }

  @Test void keepsAndRunsThePackageAnotherRunCachedDuringTheDownload() {
    def content = fakePackage()
    def cached = new File(folder.root, "cache/the-cook/1.2.3/${TARGET}/${IMAGE}")
    def cacheMeanwhile = {
      cached.parentFile.mkdirs()
      cached.text = '#!/bin/sh\necho cached meanwhile\n'
      cached.setExecutable(true)
    }

    serve(content, { String url, List<String> requests ->
      def result = run(wrapper(url, sha256(content)), folder.root, System.getenv('PATH'), 'help')

      assert result.status == 0 : result.err
      assert result.out == 'cached meanwhile\n'
      assert requests == [PACKAGE]
      assert new File(folder.root, 'cache/the-cook/1.2.3').list() as List == [TARGET]
      assert new File(folder.root, "cache/the-cook/1.2.3/${TARGET}").list() as List == [IMAGE.split('/')[0]]
    }, cacheMeanwhile)
  }

  @Test void downloadsWithWgetAndVerifiesWithShasumWhenCurlAndSha256sumAreAbsent() {
    Assume.assumeTrue(installed('wget') && installed('shasum'))
    def content = fakePackage()

    serve(content) { String url, List<String> requests ->
      def cook = wrapper(url, sha256(content))

      def result = run(cook, folder.root, tools(TOOLS + ['wget', 'shasum']), 'help')

      assert result.status == 0 : result.err
      assert result.out == "${folder.root.canonicalPath}\nhelp\n"
      assert requests == [PACKAGE]
    }
  }

  @Test void namesTheToolItNeedsAndAPlatformWithoutAPackage() {
    def cook = wrapper('http://127.0.0.1:9', '0' * 64)
    def uname = new File(folder.newFolder('uname'), 'uname')
    uname.text = '#!/bin/sh\n[ "$1" = -s ] && echo Darwin || echo x86_64\n'
    uname.setExecutable(true)
    def hasher = installed('sha256sum') ? 'sha256sum' : 'shasum'

    [
      [tools(TOOLS + [hasher]), 'cook: downloading the-cook needs curl or wget\n'],
      [tools(TOOLS + ['curl']), 'cook: verifying the-cook needs sha256sum or shasum\n'],
      [tools(TOOLS - ['tar'] + ['curl', hasher]), 'cook: unpacking the-cook needs tar\n'],
      [
        "${uname.parent}:${tools(TOOLS)}".toString(),
        'cook: the-cook has no package for Darwin x86_64; it runs on Linux x64 and arm64 and macOS arm64\n'
      ],
    ].each { path, message ->
      def result = run(cook, folder.root, path, 'help')
      assert result.status == 2
      assert result.err == message
    }
  }

  // A package whose launcher prints its working directory and then each argument on its own line.
  private byte[] fakePackage() {
    def root = folder.newFolder()
    def launcher = new File(root, IMAGE)
    launcher.parentFile.mkdirs()
    launcher.text = '#!/bin/sh\npwd -P\nfor argument in "$@"; do printf \'%s\\n\' "$argument"; done\n'
    launcher.setExecutable(true)
    def archive = new File(root, 'package.tar.gz')
    def tar = new ProcessBuilder('tar', '-czf', archive.path, '-C', root.path, IMAGE.split('/')[0]).start()
    assert tar.waitFor() == 0 : tar.errorStream.text
    archive.bytes
  }

  private File wrapper(String url, String sha256) {
    def cook = new File(folder.root, 'cook')
    cook.text = new File(System.getProperty('cook.wrapper')).text
        .replace('@VERSION@', '1.2.3')
        .replace('@BASE_URL@', url)
        .replace('@SHA256_LINUX_X64@', sha256)
        .replace('@SHA256_LINUX_ARM64@', sha256)
        .replace('@SHA256_MACOS_ARM64@', sha256)
    cook
  }

  // `onRequest` runs as each request arrives, before the response.
  private static void serve(byte[] content, Closure body, Closure onRequest = { }) {
    List<String> requests = [].asSynchronized()
    def server = HttpServer.create(new InetSocketAddress(InetAddress.loopbackAddress, 0), 0)
    server.createContext('/') { exchange ->
      requests << exchange.requestURI.path
      onRequest()
      def found = exchange.requestURI.path == PACKAGE
      exchange.sendResponseHeaders(found ? 200 : 404, found ? content.length : -1)
      exchange.responseBody.withCloseable { it.write(found ? content : new byte[0]) }
    }
    server.start()

    try {
      body("http://127.0.0.1:${server.address.port}", requests)
    } finally {
      server.stop(0)
    }
  }

  private Map run(File cook, File directory, String path, String... arguments) {
    def builder = new ProcessBuilder(['/bin/sh', cook.path] + arguments.toList()).directory(directory)
    builder.environment().putAll(PATH: path, HOME: folder.root.path,
    XDG_CACHE_HOME: new File(folder.root, 'cache').path)
    def process = builder.start()
    def out = new StringBuilder()
    def err = new StringBuilder()
    process.waitForProcessOutput(out, err)
    [status: process.exitValue(), out: out.toString(), err: err.toString()]
  }

  // A PATH holding only `names`, linked from the machine's own.
  private String tools(List<String> names) {
    def bin = folder.newFolder()
    names.each { name ->
      def target = System.getenv('PATH')
          .split(File.pathSeparator)
          .collect { new File(it, name) }
          .find { it.canExecute() }
      assert target : "${name} is not installed"
      java.nio.file.Files.createSymbolicLink(new File(bin, name).toPath(), target.toPath())
    }
    bin.path
  }

  private static boolean installed(String name) {
    System.getenv('PATH')
        .split(File.pathSeparator)
        .any { new File(it, name).canExecute() }
  }

  private static String sha256(byte[] bytes) {
    MessageDigest.getInstance('SHA-256')
        .digest(bytes)
        .encodeHex()
        .toString()
  }
}
