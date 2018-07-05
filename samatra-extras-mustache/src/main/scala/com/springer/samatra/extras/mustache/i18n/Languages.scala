package com.springer.samatra.extras.mustache.i18n

sealed trait I18nFailure
case class TranslationUnavailable(untranslated: String) extends I18nFailure
case class UnsupportedLanguage(languageCode: String) extends I18nFailure

class Languages(val supportedLanguages : Map[String, PropertiesLoader]) {

  private val languages: Map[String, Map[String, String]] = supportedLanguages.map { case (locale, pl) =>
    locale -> pl.load
  }

  def translate(localeIdentifier: String, key: String): Either[I18nFailure, String] = {
    for {
      language <- languages.get(localeIdentifier).toRight(
        UnsupportedLanguage(localeIdentifier)
      )
      translation <- language.get(key).toRight(
        TranslationUnavailable(key)
      )
    } yield translation
  }

  def translator(lang: String): Translator = new Translator {
    override def language: String = lang
    override def translate(key: String): Either[I18nFailure, String] = Languages.this.translate(lang, key)
  }
}

object Languages {
  val en: String = "En"
}

trait Translator {
  def language: String
  def translate(key: String): Either[I18nFailure, String]
  def unsafeTranslate(key: String): String =
    translate(key) match {
      case Right(translation) => translation
      case Left(TranslationUnavailable(unavailableKey)) => throw new IllegalArgumentException(s"Key not found: $unavailableKey")
      case Left(UnsupportedLanguage(unavailableLanguage)) => throw new IllegalArgumentException(s"Language not found: $unavailableLanguage")
    }

  def apply(key: String): String = unsafeTranslate(key)
}
