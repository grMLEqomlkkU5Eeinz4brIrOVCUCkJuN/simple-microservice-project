package services

import org.mindrot.jbcrypt.BCrypt
import models.ProjectResponse

sealed trait AccessLevel
case object Owner extends AccessLevel
case object EditAccess extends AccessLevel
case object ReadOnlyAccess extends AccessLevel
case object NoAccess extends AccessLevel

object ProjectAccessService {

  def getAccessLevel(
    project: ProjectResponse,
    userId: Long,
    password: Option[String] = None
  ): AccessLevel = {
    if (project.ownerId == userId) {
      return Owner
    }

    ProjectPermissionService.getPermission(project.id, userId) match {
      case Some(perm) if perm.revokedAt.isEmpty =>
        perm.permissionLevel match {
          case "EDIT" => return EditAccess
          case "READONLY" => return ReadOnlyAccess
          case _ => return NoAccess
        }
      case _ => ()
    }

    if (project.isPublic || project.isShared) {
      password match {
        case Some(pwd) =>
          if (project.editPasswordHash.isDefined && verifyPassword(pwd, project.editPasswordHash.get)) {
            return EditAccess
          }
          if (project.viewPasswordHash.isDefined && verifyPassword(pwd, project.viewPasswordHash.get)) {
            return ReadOnlyAccess
          }
        case None =>
          if (project.isPublic && project.editPasswordHash.isEmpty && project.viewPasswordHash.isEmpty) {
            return ReadOnlyAccess
          }
      }
    }
    NoAccess
  }

  def canPerformAction(
    project: ProjectResponse,
    userId: Long,
    action: ProjectAction,
    password: Option[String] = None
  ): Boolean = {
    val accessLevel = getAccessLevel(project, userId, password)

    (action, accessLevel) match {
      case (_, Owner) => true  // Owner can do anything
      case (ProjectAction.View, EditAccess) => true
      case (ProjectAction.View, ReadOnlyAccess) => true
      case (ProjectAction.Edit, EditAccess) => true
      case (ProjectAction.Delete, Owner) => true
      case (ProjectAction.Share, Owner) => true
      case (ProjectAction.ManagePermissions, Owner) => true
      case _ => false
    }
  }

  def getAccessibleProjects(userId: Long): Seq[ProjectResponse] = {
    val ownedProjects = ProjectService.getByOwnerId(userId)

    val sharedProjects = ProjectPermissionService
      .getUserPermissions(userId)
      .flatMap(perm => ProjectService.getById(perm.projectId))

    (ownedProjects ++ sharedProjects).distinctBy(_.id)
  }

  def isPubliclyAccessible(project: ProjectResponse): Boolean =
    AuthorizationService.isPubliclyAccessible(project)

  def requiresPasswordForAccess(project: ProjectResponse): Boolean =
    AuthorizationService.requiresPasswordForAccess(project)

  def getUsersWithAccess(projectId: Long): Seq[(Long, String)] = {
    ProjectService.getById(projectId) match {
      case Some(project) =>
        val owner = Seq((project.ownerId, "OWNER"))
        val invited = ProjectPermissionService
          .getProjectPermissions(projectId)
          .map(perm => (perm.userId, perm.permissionLevel))
        owner ++ invited
      case None => Seq.empty
    }
  }

  private def verifyPassword(plaintext: String, hash: String): Boolean = {
    try {
      BCrypt.checkpw(plaintext, hash)
    } catch {
      case _: Exception => false
    }
  }
}

sealed trait ProjectAction
object ProjectAction {
  case object View extends ProjectAction
  case object Edit extends ProjectAction
  case object Delete extends ProjectAction
  case object Share extends ProjectAction
  case object ManagePermissions extends ProjectAction
}