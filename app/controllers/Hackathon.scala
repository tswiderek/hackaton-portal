package controllers

import play.api._
import data.Form
import data.Forms._
import play.api.mvc._
import play.api.i18n._
import model._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column
import helpers.Forms._

object Hackathon extends Controller with securesocial.core.SecureSocial {

  def hackathonsJson = Action {
    transaction {
      val hackathons = Model.hackathons.toList
      Ok(com.codahale.jerkson.Json.generate(hackathons)).as(JSON)
    }
  }

  val hackathonForm = Form(
    mapping(
      "subject" -> nonEmptyText,
      "status" -> enum(HackathonStatus),
      "submitterId" -> longNumber,
      "locationId" -> longNumber)(model.Hackathon.apply)(model.Hackathon.unapply))

  def index = UserAwareAction {
    implicit request =>
      transaction {
        Ok(views.html.hackathons.index(Model.hackathons.toList, request.user))
      }
  }

  def view(id: Long) = UserAwareAction {
    implicit request =>
      transaction {
        val users:Map[Long, String] = model.User.all.map({ u => (u.id, u.name) }).toMap
        val locations:Map[Long, String] = model.Location.all.toList.map({ l => (l.id, l.name) }).toMap
        val news = model.News.all(id)
        Ok(views.html.hackathons.view(Model.hackathons.lookup(id), news, users, locations, request.user))
      }
  }

  def create = SecuredAction() {
    implicit request =>
      transaction {
        Ok(views.html.hackathons.create(hackathonForm, model.User.all.toList, model.Location.all.toList, request.user))
      }
  }

  def save = SecuredAction() {
    implicit request =>
      hackathonForm.bindFromRequest.fold(
        errors => transaction {
          BadRequest(views.html.hackathons.create(errors, model.User.all.toList, model.Location.all.toList, request.user))
        },
        hackathon => transaction {
          Model.hackathons.insert(hackathon)
          Redirect(routes.Hackathon.index).flashing("status" -> "added", "title" -> hackathon.subject)
        }
      )
  }

  def edit(id: Long) = SecuredAction() {
    implicit request =>
      transaction {
        Model.hackathons.lookup(id).map {
          hackathon =>
            Ok(views.html.hackathons.edit(id, hackathonForm.fill(hackathon), model.User.all.toList, model.Location.all.toList, request.user))
        }.get
      }
  }

  def update(id: Long) = SecuredAction() {
    implicit request =>
      hackathonForm.bindFromRequest.fold(
        errors => transaction {
          BadRequest(views.html.hackathons.edit(id, errors, model.User.all.toList, model.Location.all.toList, request.user))
        },
        hackathon => transaction {
          Model.hackathons.update(h =>
            where(h.id === id)
              set(
              h.subject := hackathon.subject,
              h.status := hackathon.status,
              h.submitterId := hackathon.submitterId,
              h.locationId := hackathon.locationId))
          Redirect(routes.Hackathon.index).flashing("status" -> "updated", "title" -> hackathon.subject)
        })
  }

  def delete(id: Long) = SecuredAction() {
    implicit request =>
      transaction {
        Model.hackathons.deleteWhere(h => h.id === id)
      }
      Redirect(routes.Hackathon.index).flashing("status" -> "deleted")
  }
}