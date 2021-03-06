package com.github.ldaniels528.trifecta.messages.codec

import com.github.ldaniels528.trifecta.TxConfig
import com.github.ldaniels528.trifecta.TxConfig.{TxFailedSchema, TxSuccessSchema}
import com.github.ldaniels528.trifecta.messages.codec.avro.{AvroCodec, AvroDecoder}
import com.github.ldaniels528.trifecta.messages.codec.json.JsonMessageDecoder
import com.github.ldaniels528.trifecta.messages.codec.apache.ApacheAccessLogDecoder
import com.github.ldaniels528.trifecta.messages.codec.gzip.GzipCodec
import com.kbb.trifecta.decoders.LogRawMessageDecoder

import scala.util.{Success, Try}

/**
  * Message CODEC Factory
  * @author lawrence.daniels@gmail.com
  */
object MessageCodecFactory {

  /**
    * Optionally returns a message decoder for the given URL
    * @param url the given message decoder URL (e.g. "avro:file:avro/quotes.avsc")
    * @return an option of a [[MessageDecoder]]
    */
  def getDecoder(config: TxConfig, url: String): Option[MessageDecoder[_]] = {
    url match {
      case "apachelog" => Option(ApacheAccessLogDecoder)
      case s if s.startsWith("avro:") => Option(AvroCodec.resolve(s.drop(5)))
      case "bytes" => Option(LoopBackCodec)
      case s if s.startsWith("decoder:") =>
        config.getDecoders.find(_.name == s.drop(8)) map (_.decoder match {
          case TxSuccessSchema(_, avroDecoder, _) => avroDecoder
          case TxFailedSchema(_, cause, _) => throw new IllegalStateException(cause.getMessage)
        })
      case "gzip" => Option(GzipCodec)
      case "json" => Option(JsonMessageDecoder)
      case "lograw" => Option(LogRawMessageDecoder)
      case "text" => Option(PlainTextCodec)
      case _ => None
    }
  }

  /**
    * Optionally returns a message encoder for the given URL
    * @param url the given message encoder URL (e.g. "bytes")
    * @return an option of a [[MessageEncoder]]
    */
  def getEncoder(url: String): Option[MessageEncoder[_]] = {
    url match {
      case "bytes" => Option(LoopBackCodec)
      case "gzip" => Option(GzipCodec)
      case "text" => Option(PlainTextCodec)
      case _ => None
    }
  }

  /**
    * Returns the type name of the given message decoder
    * @param decoder the given message decoder
    * @return the type name (e.g. "json")
    */
  def getTypeName(decoder: MessageDecoder[_]): String = decoder match {
    case ApacheAccessLogDecoder => "apachelog"
    case av: AvroDecoder => "avro"
    case JsonMessageDecoder => "json"
    case GzipCodec => "gzip"
    case LoopBackCodec => "bytes"
    case PlainTextCodec => "text"
    case _ => "unknown"
  }

  /**
    * Loop-back CODEC
    * @author lawrence.daniels@gmail.com
    */
  object LoopBackCodec extends MessageDecoder[Array[Byte]] with MessageEncoder[Array[Byte]] {

    /**
      * Decodes the binary message into a typed object
      * @param message the given binary message
      * @return a decoded message wrapped in a Try-monad
      */
    override def decode(message: Array[Byte]): Try[Array[Byte]] = Success(message)

    /**
      * Encodes the binary message into a typed object
      * @param message the given binary message
      * @return a encoded message wrapped in a Try-monad
      */
    override def encode(message: Array[Byte]): Try[Array[Byte]] = Success(message)
  }

  /**
    * Plain-Text CODEC
    * @author lawrence.daniels@gmail.com
    */
  object PlainTextCodec extends MessageDecoder[String] with MessageEncoder[String] {

    /**
      * Decodes the binary message into a typed object
      * @param message the given binary message
      * @return a decoded message wrapped in a Try-monad
      */
    override def decode(message: Array[Byte]): Try[String] = Success(new String(message))

    /**
      * Encodes the binary message into a typed object
      * @param message the given binary message
      * @return a encoded message wrapped in a Try-monad
      */
    override def encode(message: Array[Byte]): Try[String] = Success(new String(message))
  }

}
