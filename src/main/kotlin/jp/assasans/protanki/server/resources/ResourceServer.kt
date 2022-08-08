package jp.assasans.protanki.server.resources

import java.io.InputStream
import kotlin.io.path.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.BuildConfig
import jp.assasans.protanki.server.IResourceManager
import jp.assasans.protanki.server.ServerIdResource
import jp.assasans.protanki.server.extensions.gitVersion
import jp.assasans.protanki.server.utils.ResourceUtils

interface IResourceServer {
  suspend fun run()
  suspend fun stop()
}

class ResourceServer : IResourceServer, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val resourceManager: IResourceManager by inject()

  private val originalPackName = "original"

  private lateinit var engine: ApplicationEngine

  val client = HttpClient(CIO) {
    install(DefaultRequest) {
      headers {
        set(HttpHeaders.UserAgent, "ProTanki Server/${BuildConfig.gitVersion}")
      }
    }
  }

  override suspend fun run() {
    engine = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
      routing {
        get("/{id1}/{id2}/{id3}/{id4}/{version}/{file}") {
          val resourceId = ResourceUtils.decodeId(
            listOf(
              call.parameters["id1"]!!, call.parameters["id2"]!!, call.parameters["id3"]!!, call.parameters["id4"]!!,
              call.parameters["version"]!!
            )
          )
          val file = call.parameters["file"]!!

          val resource = resourceManager.get("static/$originalPackName/${resourceId.id}/${resourceId.version}/$file")
          if(resource.notExists()) {
            val stream = downloadOriginal(resourceId, file)
            if(stream == null) {
              call.response.status(HttpStatusCode.NotFound)
              call.respondText(ContentType.Text.Html) { getNotFoundBody(resourceId, file) }

              logger.debug { "Resource ${resourceId.id}:${resourceId.version}/$file not found" }
              return@get
            }

            if(resource.parent.notExists()) resource.parent.createDirectories()
            withContext(Dispatchers.IO) {
              resource.outputStream().use { output -> stream.copyTo(output) }
            }
          }

          val contentType = when(resource.extension) {
            "jpg"  -> ContentType.Image.JPEG
            "png"  -> ContentType.Image.PNG
            "json" -> ContentType.Application.Json
            "xml"  -> ContentType.Application.Xml
            else   -> ContentType.Application.OctetStream
          }

          call.respondOutputStream(contentType) { resource.inputStream().copyTo(this) }
          logger.trace { "Sent resource ${resourceId.id}:${resourceId.version}/$file" }
        }

        static("/assets") {
          staticRootFolder = resourceManager.get("assets").toFile()
          files(".")
        }
      }
    }.start()

    logger.info { "Started resource server" }
  }

  private suspend fun downloadOriginal(resourceId: ServerIdResource, file: String): InputStream? {
    return withContext(Dispatchers.IO) {
      val response = client.get("http://54.36.175.134/${ResourceUtils.encodeId(resourceId).joinToString("/")}/$file") {
        expectSuccess = false
      }

      if(response.status == HttpStatusCode.OK) {
        logger.debug { "Downloaded original resource: ${resourceId.id}/${resourceId.version}/$file" }
        return@withContext response.bodyAsChannel().toInputStream()
      }
      if(response.status == HttpStatusCode.NotFound) return@withContext null
      throw Exception("Failed to download resource ${resourceId.id}:${resourceId.version}/${file}. Status code: ${response.status}")
    }
  }

  private fun getNotFoundBody(resourceId: ServerIdResource, file: String) = """
    |<!DOCTYPE html>
    |<html>
    |<head>
    |  <title>404 Not Found</title>
    |  
    |  <style>
    |    body {
    |      font-family: monospace;
    |      font-size: 1.25em;
    |    }
    |    
    |    span.resource {
    |      font-weight: bold;
    |    }
    |  </style>
    |</head>
    |<body>
    |  <h1>Not Found</h1>
    |  <p>The requested resource <span class="resource">${resourceId.id}:${resourceId.version}/$file</span> was not found on this server.</p>
    |  <hr />
    |  <h4><a href="https://github.com/Assasans/protanki-server" target="_black" rel="noopener">protanki-server</a>, ${BuildConfig.gitVersion}</h4>
    |</body>
    |</html>
  """.trimMargin()

  override suspend fun stop() {
    logger.debug { "Stopping Ktor engine..." }
    engine.stop(2000, 3000)
    client.close()

    logger.info { "Stopped resource server" }
  }
}
