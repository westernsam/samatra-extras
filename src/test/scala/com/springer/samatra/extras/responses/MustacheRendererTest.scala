package com.springer.samatra.extras.responses

import org.scalatest.FunSpec
import org.scalatest.Matchers._

class MustacheRendererTest extends FunSpec {
  it("renders a String") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("foo", Map("foo" -> "bar")) shouldBe Right("<html>This is a test. Foo: bar</html>")
  }

  it("renders an Int") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("foo", Map("foo" -> -1)) shouldBe Right("<html>This is a test. Foo: -1</html>")
  }

  it("fails to render when the model doesn't have a matching key"){
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("foo", Map.empty[String, Any]) match {
      case Left(ViewRenderingError(t)) =>
        t shouldBe a[com.samskivert.mustache.MustacheException.Context]
        t.getMessage shouldBe "No method or field with name 'foo' on line 1"
      case otherwise => fail("Should have failed with a ViewRenderingError, but we got:" + otherwise)
    }
  }

  it("refuses to render a map") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("dot", Map("foo" -> "bar")) match {
      case Left(ViewRenderingError(t)) =>
        t shouldBe a[NonLeaveFormattingError]
        t.getMessage shouldBe "Only leaves can be formatted, but we got '{foo=bar}'"
      case otherwise => fail("Should have failed with a ViewRenderingError, but we got:" + otherwise)
    }
  }
}
