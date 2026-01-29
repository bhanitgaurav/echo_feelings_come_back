package com.bhanit.apps.echo.core.domain.model

import com.bhanit.apps.echo.core.util.ErrorCode

class ServiceException(val errorCode: ErrorCode) : Exception(errorCode.message)
