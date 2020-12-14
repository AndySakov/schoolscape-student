package controllers

import java.net.{NoRouteToHostException, SocketException, SocketTimeoutException}

import api.server.Commons._
import javax.inject.{Inject, Singleton}
import models.UserFactory
import play.api.mvc._
import requests.{RequestFailedException, TimeoutException}

import scala.util.{Failure, Success, Try}
//noinspection DuplicatedCode
@Singleton
class ApiController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  /**
    * This controller handles all the data management in the application
    * Mainly for grabbing data from the api endpoints
    * @author Obafemi Teminife
    * @note Might be broken down into several components later
    * @group controllers
    */

  def errFlash(reason: String): (String, String) = {
    "reason" -> reason
  }

  def login: Action[AnyContent] = Action{
    request: Request[AnyContent] => {
      request.body.asFormUrlEncoded.map{
        args =>
          val user = args("user").head
          val pass = args("pass").head
          Try(jsonify(requests.post(AUTH_USER, data = Map("user" -> user, "pass" -> pass)).text).hcursor) match {
            case Success(result) =>
              if(result.get[Boolean]("success").getOrElse(false)){
                UserFactory.initUser(user)
                Redirect("/dashboard/home").withNewSession.withSession("user" -> user)
              } else
                Redirect("/account/login").flashing(flash("Login Failed!!!", "danger"): _*)
            case Failure(exception) =>
              exception match {
                case _: NoRouteToHostException => Redirect("/error/api/connect_failure").flashing(errFlash("Server unreachable"))
                case _: SocketTimeoutException => Redirect("/error/api/connect_failure").flashing(errFlash("Socket connection timed out!"))
                case _: SocketException => Redirect("/error/api/connect_failure").flashing(errFlash("Socket related error!"))
                case _: TimeoutException => Redirect("/error/api/connect_failure").flashing(errFlash("Connection timed out"))
                case _ => InternalServerError(views.html.err.InternalServerError())
              }
          }
      }.getOrElse(Redirect("/error/login/failure"))
    }
  }

  def signup: Action[AnyContent] = Action {
    request: Request[AnyContent] => {
      request.body.asFormUrlEncoded.map {
        args => {
          val formData = Map("full" -> args("full").head, "user" -> args("user").head, "class" -> args("class").head, "pass" -> args("pass").head)
          Try(jsonify(requests.post(url = CREATE_USER, data = formData).text).hcursor) match {
            case Failure(exception) => exception match {
              case _: NoRouteToHostException => Redirect("/error/api/connect_failure").flashing(errFlash("Server unreachable"))
              case _: SocketTimeoutException => Redirect("/error/api/connect_failure").flashing(errFlash("Socket connection timed out!"))
              case _ => InternalServerError(views.html.err.InternalServerError())
            }
            case Success(result) =>
              if(result.get[Boolean]("success").getOrElse(false)) Redirect("/success/signup")
              else Redirect("/account/create").flashing(flash(result.get[String]("message").getOrElse("Signup Failed"), "danger"): _*)
          }
        }
      }.getOrElse(Redirect("/error/login/failure"))
    }
  }
  def editProfile: Action[AnyContent] = Action {
    request: Request[AnyContent] => {
      request.body.asFormUrlEncoded.map {
        args => {
          val formData = Map("oldUser" -> user.username, "name" -> args("name").head, "user" -> args("user").head, "pass" -> args("pass").head)
          Try(jsonify(requests.post(url = EDIT_USER, data = formData).text).hcursor) match {
            case Failure(exception) => exception match {
              case _: NoRouteToHostException => Redirect("/error/api/connect_failure")
              case _: SocketTimeoutException => Redirect("/error/api/connect_failure")
              case _: RequestFailedException => Redirect("/acccount/profile").flashing(flash("Unable to edit account!", "danger"): _*)
              case _ => InternalServerError(views.html.err.InternalServerError())
            }
            case Success(result) =>
              UserFactory.initUser(args("user").head)
              Redirect("/account/login").withNewSession.flashing(flash("Edited account successfully!", "success"): _*)
          }
        }
      }.getOrElse(Redirect("/acccount/profile").flashing(flash("Unable to edit account!", "danger"): _*))
    }
  }
}