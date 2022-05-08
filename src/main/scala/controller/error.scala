package controller

trait ControllerError

case object InvalidDataError extends ControllerError
case object InternalServerError extends ControllerError
case object NotFoundError extends ControllerError
