package com.springer.samatra.extras.mustache.i18n

import java.io.{StringReader, Writer}
import com.samskivert.mustache.Mustache.Lambda
import com.samskivert.mustache.Template
import com.springer.samatra.extras.core.PrintStackTrace
import com.springer.samatra.extras.mustache.MustacheRenderer

import java.util
import scala.jdk.CollectionConverters.MapHasAsScala

class I18N(translator: Translator, tr: => MustacheRenderer) extends Lambda {

  override def execute(frag: Template#Fragment, out: Writer): Unit = {
    val translationKey: String = frag.execute()

    case class TranslateError(exception: Throwable) extends PrintStackTrace

    (for {
      text <- translator.translate(translationKey).left.map(e => TranslateError(new RuntimeException(s"Error getting translation ${e}")))
      rendered <- {
        val value = frag.context()
        if (value.isInstanceOf[Map[_, _]] && text.indexOf("{{") > -1)
          tr.rendered(new StringReader(text), value.asInstanceOf[util.Map[String, Any]].asScala.toMap)
        else
          Right(text)
      }
    } yield rendered) match {
      case Right(text) => out.write(text)
      case Left(err) => throw new IllegalArgumentException(s"${err.toString} for $translationKey in ${translator.language}", err.exception)
    }
  }
}
