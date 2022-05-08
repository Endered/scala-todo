package usecase

sealed trait UsecaseError
case object NotFoundError extends UsecaseError
case object DatabaseError extends UsecaseError
