package db

private[db] object LocalFilters {

  def organizationByKey(
    organizationKeyColumnName: String,
    orgKeyBindVarName: String,
    orgKey: String
  ): String = {
    s"and $organizationKeyColumnName = lower(trim({$orgKeyBindVarName}))"
  }

  def organizationSubueryByKey(
    organizationGuidColumnName: String,
    orgKeyBindVarName: String,
    orgKey: String
  ): String = {
    s"and $organizationGuidColumnName = (select guid from organizations where deleted_at is null and key = lower(trim({$orgKeyBindVarName})))"
  }

}
