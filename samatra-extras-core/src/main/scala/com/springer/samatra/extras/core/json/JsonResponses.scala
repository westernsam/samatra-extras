package com.springer.samatra.extras.core.json

import java.io.{OutputStream, OutputStreamWriter, StringWriter, Writer}

import JsonSerialization.{JsonSerilizer, NonRecursiveJsonSerializer}
import com.springer.samatra.routing.Routings.HttpResp
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.collection.immutable.{List, Nil}
import scala.language.implicitConversions

object JsonResponses {

  case class JsonResponse(body: Map[String, Any])

  case class JsonHttpResp(json: JsonResponse) extends HttpResp {
    private val serializer: JsonSerilizer = new NonRecursiveJsonSerializer()
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json; charset=utf-8")
      resp.setStatus(200)
      try serializer.toJson(json.body, resp.getOutputStream) finally resp.getOutputStream.flush()
    }
  }

  implicit def fromJsonResponse(json: JsonResponse): HttpResp = JsonHttpResp(json)
}

object JsonSerialization {

  private class Stack[A] private (var elems: List[A]) {
    def this() = this(Nil)
    def isEmpty:Boolean = elems.isEmpty
    def pop(): A = {
      val res = elems.head
      elems = elems.tail
      res
    }
    def push(elem: A): this.type = { elems = elem :: elems; this }
  }

  trait JsonSerilizer {
    def toJson(m: Map[String, Any]): String = {
      val w: StringWriter = new StringWriter()
      toJson(m, w)
      w.toString
    }

    def toJson(m: Map[String, Any], outputStream: OutputStream): Unit = {
      val w = new OutputStreamWriter(outputStream)
      try toJson(m, w) finally w.flush()
    }

    def toJson(m: Map[String, Any], writer: Writer): Unit
  }

  class NonRecursiveJsonSerializer extends JsonSerilizer {

    sealed trait Event
    sealed case class StartArray() extends Event
    sealed case class StartMap() extends Event
    sealed case class Comma() extends Event

    //probably slower than necessary
    def escapeJson(str: String) :String = str
      .replace("\\", "\\\\")
      .replace("\\/", "\\\\/")
      .replace("\"", "\\\"")
      .replace("\b", "\\b")
      .replace("\f", "\\f")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

    def toJson(m: Map[String, Any], w: Writer): Unit = {

      val stack: Stack[Any] = new Stack[Any]()
      stack.push(m)
      stack.push(StartMap())

      while (!stack.isEmpty) {
        stack.pop() match {
          case _: StartMap => w.write('{')
          case _: StartArray => w.write('[')
          case _: Comma => w.write(',')

          case map: Map[_, Any] =>

            map.headOption match {
              case Some(keyValue) =>
                if (!keyValue._1.isInstanceOf[String])
                  throw new IllegalArgumentException(s"Can't jsonify maps with keys that are not Strings. Key $keyValue._1 is a ${keyValue._1.getClass.getName}")

                w.write(s""""${keyValue._1}":""")
                stack.push(map.tail)
                if (map.size > 1)
                  stack.push(Comma())
                stack.push(keyValue._2)
                keyValue._2 match {
                  case _: Map[_, Any] => stack.push(StartMap())
                  case _: Iterable[Any] => stack.push(StartArray())
                  case _ =>
                }

              case None =>
                w.write('}')
            }

          case list: Iterable[Any] =>
            list.headOption match {
              case Some(value) =>
                stack.push(list.tail)
                if (list.size > 1)
                  stack.push(Comma())
                stack.push(value)
                value match {
                  case _: Map[_, Any] => stack.push(StartMap())
                  case _: Iterable[Any] => stack.push(StartArray())
                  case _ =>
                }

              case None =>
                w.write(']')
            }

          case str: String => w.write(s""""${escapeJson(str)}"""")
          case boolean: Boolean => w.write(s"$boolean")
          case number: Number => w.write(s"${number.toString}")
          case nil if nil == null => w.write("null")

          case any => throw new IllegalArgumentException(s"Can't jsonify $any of type ${any.getClass.getName}")
        }
      }
    }
  }

}
