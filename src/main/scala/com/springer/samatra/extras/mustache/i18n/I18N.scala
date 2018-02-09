package com.springer.samatra.extras.mustache.i18n

import java.io.{StringReader, Writer}

import com.samskivert.mustache.Mustache.Lambda
import com.samskivert.mustache.Template
import com.springer.samatra.extras.responses.MustacheRenderer

import scala.collection.JavaConverters._

class I18N(translator: Translator, tr: => MustacheRenderer) extends Lambda {

  override def execute(frag: Template#Fragment, out: Writer): Unit = {
    val translationKey = frag.execute()

    (for {
      text <- translator.translate(translationKey)
      rendered <- {
        frag.context() match {
          case m: java.util.Map[_, _] if text.indexOf("{{") > -1 =>
            tr.rendered(new StringReader(text), m.asInstanceOf[java.util.Map[String, Any]].asScala.toMap)
          case _ => Right(text)
        }
      }
    } yield rendered) match {
      case Right(text) => out.write(text)
      case Left(err) => throw new IllegalArgumentException(s"${err.toString} for $translationKey in ${translator.language}")
    }
  }
}
