package com.vertx.api.app

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.net.http.HttpResponse
import java.util.stream.Collectors

class MainVerticle : AbstractVerticle() {

  val config =
    JsonObject().put("db_name", "test")
      .put("connection_string", "mongodb://localhost:27017")
      .put("useObjectId", true)
  private lateinit var mongoClient: MongoClient
  override fun start(startFuture: Future<Void>) {
    val server = vertx
      .createHttpServer()

    mongoClient = MongoClient.createShared(vertx, config)

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    router.post("/users").handler(this::createUser)
    router.get("/users").handler(this::getUsers)
    router.put("/users").handler(this::updateUser)
    router.delete("/users").handler(this::deleteUser)



    server.requestHandler(router).listen(8080) {

      if (it.succeeded()) {
        println("Started server on port 8080")
      } else {
        println("Failed to start server")
      }
    }
  }


  private fun createUser(context: RoutingContext) {

    val body = context.bodyAsJson
    mongoClient.insert("users", body) { res ->

      if (res.succeeded()) {

        val id = res.result()
        context.response().end(id)

      } else {
        context.response().end(res.cause().toString())
      }

    }
  }


  private fun updateUser(context: RoutingContext) {

    val newName = context.bodyAsJson.getString("name")
    val userId = context.request().params().get("id")

    val query = json {
      obj("_id" to userId)
    }

    val update = json {
      obj("\$set" to obj("name" to newName))
    }
    mongoClient.updateCollection("users", query, update) { res ->

      if (res.succeeded()) {

        val response = json {
          obj("success" to true, "message" to "User name updated successfully")
        }
        context.response()
          .setStatusCode(204)
          .putHeader("Content-Type", "application/json")
          .end(response.encode())
      } else {

        val failureResponse = json {
          obj("message" to res.cause().message, "success" to false)
        }
        context.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(failureResponse.encode())
      }


    }


  }

  private fun getUserById(context: RoutingContext) {

    val path = context.request().getParam("id")

  }

  private fun getUsers(context: RoutingContext) {

    val query = json {
      obj()
    }

    val response = JsonObject()
    mongoClient.find("users", query) { res ->

      if (res.succeeded()) {
        response.put("success", true)
          .put("users", res.result())

        context.response().setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(response.encode())

      } else {
        response.put("success", false)
          .put("error", res.cause().message)

        context.response().setStatusCode(400)
          .putHeader("Content-Type", "application/json")
          .end(response.encode())
      }
    }
  }


  private fun deleteUser(context: RoutingContext) {

    val userId = context.request().params().get("id")
    val query = json {
      obj("_id" to userId)
    }

    mongoClient.removeDocument("users",query) { res ->

      if (res.succeeded()) {

        context.response().setStatusCode(200)
          .putHeader("Content-Type","application/json")
          .end("Deleted successfully")
      }
      else {

        context.response().setStatusCode(500)
          .end("Failed to delete user")
      }

    }
  }
}
