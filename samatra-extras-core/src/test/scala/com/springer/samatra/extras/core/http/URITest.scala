package com.springer.samatra.extras.core.http

import org.scalatest.{FunSpec, Matchers}

class URITest extends FunSpec with Matchers {
  describe("URI") {
    it("handles with multiple query parameters") {
      val uri: URI = URI.parse("http://link.springer.com/search?query=dkasfjd&foo=bar")
      uri.format shouldBe "http://link.springer.com/search?query=dkasfjd&foo=bar"
    }

    it("parses addresses starting with // as without scheme") {
      val uri = URI.parse("//link.springer.com/search?query=dkasfjd&foo=bar")

      uri.scheme shouldBe None
      uri.authority shouldBe Some("link.springer.com")
      uri.path shouldBe "/search"
      uri.queryString shouldBe Some("query=dkasfjd&foo=bar")
      uri.format shouldBe "//link.springer.com/search?query=dkasfjd&foo=bar"
    }

    it("can produce relative URIs") {

      val uri = URI.parse("http://link.springer.com/search?query=dkasfjd&foo=bar").toRelative

      uri.scheme shouldBe None
      uri.authority shouldBe None
      uri.format shouldBe "/search?query=dkasfjd&foo=bar"
    }

    it("can parse relative URIs") {
      val uri = URI.parse("/search?query=dkasfjd&foo=bar")

      uri.scheme shouldBe None
      uri.authority shouldBe None
      uri.format shouldBe "/search?query=dkasfjd&foo=bar"
    }

    it("can remove params") {
      val uri = URI.parse("http://link.springer.com/search?query=dkasfjd&foo=bar").toRelative.removeParam("foo")
      uri.format shouldBe "/search?query=dkasfjd"

      val uri2 = URI.parse("http://link.springer.com/search?query=dkasfjd&foo=bar").toRelative.removeParam("query")
      uri2.format shouldBe "/search?foo=bar"
    }

    it("can add params") {
      val uri1 = URI.parse("http://link.springer.com/search").toRelative.addParam(("foo", "apple"))
      uri1.format shouldBe "/search?foo=apple"

      val uri2 = URI.parse("http://link.springer.com/search?query=dkasfjd").toRelative.addParam(("foo", "apple"))
      uri2.format shouldBe "/search?query=dkasfjd&foo=apple"
    }

    it("can replace params") {
      val uri1 = URI.parse("http://link.springer.com/search?query=abc").toRelative.removeParam("query").addParam(("query", "123"))
      uri1.format shouldBe "/search?query=123"
    }

  }
}
