package com.springer.samatra.extras.mustache

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import com.springer.samatra.extras.core.templating.{NonLeafFormattingError, ViewRenderingError}
import org.scalatest.FunSpec
import org.scalatest.Matchers._

import com.springer.samatra.extras.mustache.MustacheRenderer.zonedDateTimeRenderer

class MustacheRendererTest extends FunSpec {

  it("renders a String") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("foo", Map("foo" -> "bar")) shouldBe Right("<html>This is a test. Foo: bar</html>")
  }

  class MyString(val str: String)

  it("uses custom leaf renderers") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false,
      zonedDateTimeRenderer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss")),
      { case s: MyString => s"MyString: ${s.str}" }
    )
    renderer.rendered("foo", Map("foo" -> ZonedDateTime.of(2007, 1, 1, 1, 1, 1, 1, ZoneId.of("Z")))) shouldBe Right("<html>This is a test. Foo: 2007-01-01T01:01:01</html>")
    renderer.rendered("foo", Map("foo" -> new MyString("hello"))) shouldBe Right("<html>This is a test. Foo: MyString: hello</html>")
  }

  it("renders an Int") {
    val renderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)
    renderer.rendered("foo", Map("foo" -> -1)) shouldBe Right("<html>This is a test. Foo: -1</html>")
  }

  it("fails to render when the model doesn't have a matching key") {
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
        t shouldBe a[NonLeafFormattingError]
        t.getMessage shouldBe "Only leaves can be formatted, but we got '{foo=bar}'"
      case otherwise => fail("Should have failed with a ViewRenderingError, but we got:" + otherwise)
    }
  }
}
