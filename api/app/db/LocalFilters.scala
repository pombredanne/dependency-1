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
    organizationIdColumnName: String,
    orgKeyBindVarName: String,
    orgKey: String
  ): String = {
    s"and $organizationIdColumnName = (select id from organizations where deleted_at is null and key = lower(trim({$orgKeyBindVarName})))"
  }

}
