package com.abistama.supporthero.application.slackJira

enum class UseCaseResultType {
    SUCCESS,
    ERROR,
}

data class UseCaseResult(
    val type: UseCaseResultType,
    val message: String,
) {
    companion object {
        val RESULT_SUCCESS = UseCaseResult(UseCaseResultType.SUCCESS, "")
        val RESULT_ERROR = UseCaseResult(UseCaseResultType.ERROR, "")
    }
}
