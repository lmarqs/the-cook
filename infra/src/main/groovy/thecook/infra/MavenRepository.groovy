package thecook.infra

import org.w3c.dom.NodeList
import thecook.cookbook.Coordinates
import thecook.cookbook.CookbookArtifact
import thecook.cookbook.CookbookRepository

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/** A Maven-layout repository at an `https://` or `file:` URI, holding one jar per artifact. */
final class MavenRepository implements CookbookRepository {
  private static final int TIMEOUT_MILLIS = 30_000

  final URI uri

  MavenRepository(URI uri) {
    this.uri = uri
  }

  CookbookArtifact artifact(Coordinates coordinates) {
    def jar = read(uri.resolve(coordinates.jarPath))
    jar == null ? null : CookbookArtifact.fromZip(coordinates, jar, coordinates.packagePath)
  }

  List<String> versions(String group, String artifact) {
    def path = "${group.replace('.', '/')}/${artifact}/"
    def metadata = read(uri.resolve(path + 'maven-metadata.xml'))

    if (metadata == null) {
      metadata = read(uri.resolve(path + 'maven-metadata-local.xml'))
    }

    metadata == null ? [] : parseVersions(metadata)
  }

  @Override String toString() {
    uri.toString()
  }

  private static List<String> parseVersions(byte[] metadata) {
    def factory = DocumentBuilderFactory.newInstance()
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature('http://xml.org/sax/features/external-general-entities', false)
    factory.setFeature('http://xml.org/sax/features/external-parameter-entities', false)
    factory.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, '')
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, '')
    factory.setXIncludeAware(false)
    factory.setExpandEntityReferences(false)
    def document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(metadata))
    def xpath = XPathFactory.newInstance().newXPath()
    def nodes = xpath.evaluate('/metadata/versioning/versions/version', document, XPathConstants.NODESET) as NodeList
    (0..<nodes.length).collect { nodes.item(it).textContent }
  }

  private static byte[] read(URI resource) {
    try {
      def connection = resource.toURL().openConnection()
      connection.connectTimeout = TIMEOUT_MILLIS
      connection.readTimeout = TIMEOUT_MILLIS
      return connection.inputStream.withCloseable { it.readAllBytes() }
    } catch (FileNotFoundException ignored) {
      return null
    } catch (IOException error) {
      throw new IllegalArgumentException("Cannot read ${resource}: ${error.message}", error)
    }
  }
}
