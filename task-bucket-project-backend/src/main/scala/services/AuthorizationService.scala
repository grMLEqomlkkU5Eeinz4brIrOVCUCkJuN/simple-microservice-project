package services

import models.ProjectResponse

object AuthorizationService:

  sealed trait AuthError
  case object NotAuthorized extends AuthError
  case object AccessDenied extends AuthError
  case object ResourceNotFound extends AuthError
  case object OwnerOnly extends AuthError
  case object EditAccessRequired extends AuthError


  def canViewProject(project: ProjectResponse, userId: Long, shareToken: Option[String] = None): Either[AuthError, Unit] =
    if project.ownerId == userId then Right(())
    else if ProjectPermissionService.hasPermission(project.id, userId, "READONLY") then Right(())
    else if ProjectPermissionService.hasPermission(project.id, userId, "EDIT") then Right(())
    else if shareToken.isDefined then Right(())  // Share token validates separately
    else Left(AccessDenied)

  def canEditProject(project: ProjectResponse, userId: Long): Either[AuthError, Unit] =
    if project.ownerId == userId then Right(())
    else if ProjectPermissionService.hasPermission(project.id, userId, "EDIT") then Right(())
    else Left(EditAccessRequired)

  def canDeleteProject(project: ProjectResponse, userId: Long): Either[AuthError, Unit] =
    if project.ownerId == userId then Right(())
    else Left(OwnerOnly)

  def canManagePermissions(project: ProjectResponse, userId: Long): Either[AuthError, Unit] =
    if project.ownerId == userId then Right(())
    else Left(OwnerOnly)

  def canViewBucket(bucket: models.BucketResponse, userId: Long, projectId: Long): Either[AuthError, Unit] =
    if bucket.projectId != projectId then Left(ResourceNotFound)
    else
      ProjectService.getById(projectId) match
        case Some(project) =>
          canViewProject(project, userId)
        case None => Left(ResourceNotFound)

  def canEditBucket(bucket: models.BucketResponse, userId: Long, projectId: Long): Either[AuthError, Unit] =
    if bucket.projectId != projectId then Left(ResourceNotFound)
    else
      ProjectService.getById(projectId) match
        case Some(project) =>
          canEditProject(project, userId)
        case None => Left(ResourceNotFound)

  def canDeleteBucket(bucket: models.BucketResponse, userId: Long, projectId: Long): Either[AuthError, Unit] =
    if bucket.projectId != projectId then Left(ResourceNotFound)
    else
      ProjectService.getById(projectId) match
        case Some(project) =>
          canDeleteProject(project, userId)
        case None => Left(ResourceNotFound)

  def canViewTask(task: models.TaskResponse, userId: Long, bucketId: Long): Either[AuthError, Unit] =
    if task.bucketId != bucketId then Left(ResourceNotFound)
    else
      BucketService.getById(bucketId) match
        case Some(bucket) =>
          canViewBucket(bucket, userId, bucket.projectId)
        case None => Left(ResourceNotFound)

  def canEditTask(task: models.TaskResponse, userId: Long, bucketId: Long): Either[AuthError, Unit] =
    if task.bucketId != bucketId then Left(ResourceNotFound)
    else
      BucketService.getById(bucketId) match
        case Some(bucket) =>
          canEditBucket(bucket, userId, bucket.projectId)
        case None => Left(ResourceNotFound)

  def canDeleteTask(task: models.TaskResponse, userId: Long, bucketId: Long): Either[AuthError, Unit] =
    if task.bucketId != bucketId then Left(ResourceNotFound)
    else
      BucketService.getById(bucketId) match
        case Some(bucket) =>
          canDeleteBucket(bucket, userId, bucket.projectId)
        case None => Left(ResourceNotFound)

  def isPubliclyAccessible(project: ProjectResponse): Boolean =
    project.isPublic && (
      project.editPasswordHash.isEmpty && project.viewPasswordHash.isEmpty
    )

  def requiresPasswordForAccess(project: ProjectResponse): Boolean =
    (project.isPublic || project.isShared) && (
      project.editPasswordHash.isDefined || project.viewPasswordHash.isDefined
    )

  def formatError(error: AuthError): String = error match
    case NotAuthorized => "Not authorized"
    case AccessDenied => "Access denied"
    case ResourceNotFound => "Resource not found"
    case OwnerOnly => "Only project owner can perform this action"
    case EditAccessRequired => "Edit access required"

sealed trait ServiceError
case class AuthorizationError(authError: AuthorizationService.AuthError) extends ServiceError
case class NotFoundError(resource: String) extends ServiceError
case class OperationFailed(message: String) extends ServiceError
