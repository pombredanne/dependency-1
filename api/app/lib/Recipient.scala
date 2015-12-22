package com.bryzek.dependency.api.lib

import io.flow.user.v0.models.Name

/**
  * Information we use to render email messages, including the links
  * to unsubscribe.
  */
case class Recipient(
  email: String,
  name: Name,
  identifier: String
)
